package pt.notub.network.service;

import java.text.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.dto.HorarioDTO;
import pt.notub.network.dto.HorarioParagemDTO;
import pt.notub.network.dto.ParagemProximasPassagensDTO;
import pt.notub.network.dto.ProximoPasseDTO;
import pt.notub.network.dto.RotaDTO;
import pt.notub.network.dto.ZonaResumoDTO;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.repository.PontosDePassagemRepository;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.trip.entity.Viagem;
import pt.notub.trip.repository.ViagemRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RoutePlanningService {

    private static final Logger log = LoggerFactory.getLogger(RoutePlanningService.class);

    private static final int TRANSFER_BUFFER_MINUTES = 2;
    private static final double WALKING_SPEED_KMH = 4.0;
    private static final double MAX_WALKING_DISTANCE_KM = 1.0;
    private static final int MAX_WAIT_MINUTES = 65;
    private static final int MAX_RESULTS = 3;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern GENERATED_SUFFIX_PATTERN = Pattern.compile("\\s+(?:I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII)$");

    private static final int MAX_NEARBY = 5;

    private record WalkEdge(long toId, int minutes) {}

    private enum LegType {
        SOURCE,
        WALK,
        RIDE
    }

    private static final class Label {
        private final Long stopId;
        private final int arrivalMinutes;
        private final int rideCount;
        private final Set<Integer> zoneNums;
        private final Label previous;
        private final LegType legType;
        private final Long trajetoId;
        private final int departureMinutes;
        private final int boardIndex;
        private final int alightIndex;
        private final int walkMinutes;

        private Label(Long stopId,
                      int arrivalMinutes,
                      int rideCount,
                      Set<Integer> zoneNums,
                      Label previous,
                      LegType legType,
                      Long trajetoId,
                      int departureMinutes,
                      int boardIndex,
                      int alightIndex,
                      int walkMinutes) {
            this.stopId = stopId;
            this.arrivalMinutes = arrivalMinutes;
            this.rideCount = rideCount;
            this.zoneNums = immutableZones(zoneNums);
            this.previous = previous;
            this.legType = legType;
            this.trajetoId = trajetoId;
            this.departureMinutes = departureMinutes;
            this.boardIndex = boardIndex;
            this.alightIndex = alightIndex;
            this.walkMinutes = walkMinutes;
        }
    }

    private static final class BoardingCandidate {
        private final Label boardLabel;
        private final int boardIndex;
        private final int depAbsolute;
        private final int departureAtBoard;

        private BoardingCandidate(Label boardLabel, int boardIndex, int depAbsolute, int departureAtBoard) {
            this.boardLabel = boardLabel;
            this.boardIndex = boardIndex;
            this.depAbsolute = depAbsolute;
            this.departureAtBoard = departureAtBoard;
        }
    }

    private final PontosDePassagemRepository pontosRepo;
    private final TrajetoRepository trajetoRepo;
    private final ViagemRepository viagemRepo;

    private volatile boolean initialized = false;
    private Map<Long, Paragem> paragemCache;
    private Map<String, Set<Long>> nomeToIds;
    private Map<Long, Trajeto> trajetoCache;
    private Map<Long, List<PontosDePassagem>> trajetoPontosMap;
    private Map<Long, String> destinoFinalMap;
    private Map<Long, Map<Long, Integer>> trajetoOffsetMap;
    private Map<Long, Map<Long, Integer>> trajetoStopIndexMap;
    private Map<Long, List<PontosDePassagem>> paragemToPontos;
    private Map<Long, Set<Long>> paragemToTrajetoIds;
    private Map<Long, Long> pontoToTrajeto;
    private Map<String, Map<Long, List<Viagem>>> viagensByServiceAndTrajeto;
    private Map<Long, List<WalkEdge>> nearbyStopsMap;
    private Map<Integer, String> zoneNameByNum;

    public RoutePlanningService(PontosDePassagemRepository pontosRepo,
                                TrajetoRepository trajetoRepo,
                                ViagemRepository viagemRepo) {
        this.pontosRepo = pontosRepo;
        this.trajetoRepo = trajetoRepo;
        this.viagemRepo = viagemRepo;
    }

    private synchronized void ensureInitialized() {
        if (initialized) return;

        long start = System.currentTimeMillis();
        log.info("Initializing RoutePlanningService cache...");

        List<PontosDePassagem> allPontos = pontosRepo.findAll();
        List<Trajeto> allTrajetos = trajetoRepo.findAll();
        List<Viagem> allViagens = viagemRepo.findAll();

        log.info("Loaded {} pontos, {} trajetos, {} viagens in {}ms",
                allPontos.size(), allTrajetos.size(), allViagens.size(),
                System.currentTimeMillis() - start);

        paragemCache = new HashMap<>();
        nomeToIds = new HashMap<>();
        zoneNameByNum = new TreeMap<>();
        for (PontosDePassagem p : allPontos) {
            if (p.getParagem() != null) {
                Paragem paragem = p.getParagem();
                paragemCache.putIfAbsent(paragem.getId(), paragem);
                nomeToIds.computeIfAbsent(normalizeStopGroupKey(paragem.getNome()), k -> new HashSet<>()).add(paragem.getId());
                if (paragem.getZona() != null) {
                    zoneNameByNum.putIfAbsent(paragem.getZona().getNum(), paragem.getZona().getNome());
                }
            }
        }

        trajetoCache = new HashMap<>();
        trajetoPontosMap = new HashMap<>();
        destinoFinalMap = new HashMap<>();
        for (Trajeto t : allTrajetos) {
            trajetoCache.put(t.getId(), t);
            if (t.getPontosDePassagem() != null) {
                List<PontosDePassagem> sorted = new ArrayList<>(t.getPontosDePassagem());
                sorted.sort(Comparator.comparingInt(PontosDePassagem::getOrdem));
                trajetoPontosMap.put(t.getId(), sorted);
                if (!sorted.isEmpty()) {
                    Paragem last = sorted.get(sorted.size() - 1).getParagem();
                    destinoFinalMap.put(t.getId(), last != null ? last.getNome() : "");
                }
            }
        }

        trajetoOffsetMap = new HashMap<>();
        trajetoStopIndexMap = new HashMap<>();
        paragemToPontos = new HashMap<>();
        paragemToTrajetoIds = new HashMap<>();
        pontoToTrajeto = new HashMap<>();
        for (var entry : trajetoPontosMap.entrySet()) {
            Long trajetoId = entry.getKey();
            Map<Long, Integer> offsets = new HashMap<>();
            Map<Long, Integer> stopIndices = new HashMap<>();
            List<PontosDePassagem> pontos = entry.getValue();
            for (int i = 0; i < pontos.size(); i++) {
                PontosDePassagem p = pontos.get(i);
                if (p.getParagem() == null) continue;
                Long stopId = p.getParagem().getId();
                offsets.put(stopId, p.getTempoDesdeInicio());
                stopIndices.putIfAbsent(stopId, i);
                paragemToPontos.computeIfAbsent(stopId, k -> new ArrayList<>()).add(p);
                paragemToTrajetoIds.computeIfAbsent(stopId, k -> new HashSet<>()).add(trajetoId);
                pontoToTrajeto.put(p.getId(), trajetoId);
            }
            trajetoOffsetMap.put(trajetoId, offsets);
            trajetoStopIndexMap.put(trajetoId, stopIndices);
        }

        viagensByServiceAndTrajeto = new HashMap<>();
        for (Viagem v : allViagens) {
            if (v.getTrajeto() == null || v.getHoraPartida() == null || v.getServiceId() == null) continue;
            viagensByServiceAndTrajeto
                    .computeIfAbsent(v.getServiceId(), k -> new HashMap<>())
                    .computeIfAbsent(v.getTrajeto().getId(), k -> new ArrayList<>())
                    .add(v);
        }
        for (var outer : viagensByServiceAndTrajeto.values()) {
            for (var list : outer.values()) {
                list.sort(Comparator.comparing(Viagem::getHoraPartida));
            }
        }

        nearbyStopsMap = new HashMap<>();
        List<Paragem> allParagens = new ArrayList<>(paragemCache.values());
        int edgeCount = 0;
        for (Paragem p1 : allParagens) {
            if (p1.getLocalizacao() == null) continue;
            List<WalkEdge> edges = new ArrayList<>();
            for (Paragem p2 : allParagens) {
                if (p2.getId().equals(p1.getId()) || p2.getLocalizacao() == null) continue;
                double dlat = p2.getLocalizacao().getLatitude() - p1.getLocalizacao().getLatitude();
                double dlon = p2.getLocalizacao().getLongitude() - p1.getLocalizacao().getLongitude();
                if (Math.abs(dlat) > 0.02 || Math.abs(dlon) > 0.03) continue;
                double dist = haversineKm(p1.getLocalizacao().getLatitude(), p1.getLocalizacao().getLongitude(),
                        p2.getLocalizacao().getLatitude(), p2.getLocalizacao().getLongitude());
                if (dist <= MAX_WALKING_DISTANCE_KM) {
                    int walkMin = Math.max(1, (int) Math.round(dist / WALKING_SPEED_KMH * 60));
                    edges.add(new WalkEdge(p2.getId(), walkMin));
                }
            }
            edges.sort(Comparator.comparingInt(WalkEdge::minutes));
            if (edges.size() > MAX_NEARBY) edges = edges.subList(0, MAX_NEARBY);
            if (!edges.isEmpty()) {
                nearbyStopsMap.put(p1.getId(), edges);
                edgeCount += edges.size();
            }
        }
        log.info("Built {} walk edges for {} paragens", edgeCount, nearbyStopsMap.size());

        initialized = true;
        log.info("RoutePlanningService cache initialized in {}ms. {} paragens, {} trajetos, {} viagens grouped into {} services",
                System.currentTimeMillis() - start, paragemCache.size(), trajetoPontosMap.size(),
                allViagens.size(), viagensByServiceAndTrajeto.size());
    }

    public List<RotaDTO> planearRota(Long origemId, Long destinoId) {
        return planearRota(origemId, destinoId, LocalTime.now(), LocalDate.now().getDayOfWeek());
    }

    public List<RotaDTO> planearRota(Long origemId, Long destinoId, String time, String day) {
        return planearRota(origemId, destinoId, parseTimeOrDefault(time), parseDayOrDefault(day));
    }

    public List<RotaDTO> planearRota(Long origemId, Long destinoId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        ensureInitialized();

        long start = System.currentTimeMillis();
        if (origemId.equals(destinoId)) return List.of();

        String serviceId = resolveServiceId(dayOfWeek);
        int queryMinutes = toMinutes(queryTime);

        log.info("Planning route from={} to={} time={} day={} serviceId={}",
                origemId, destinoId, queryTime, dayOfWeek, serviceId);

        Set<Long> origemIds = expandByName(origemId);
        Set<Long> destinoIds = expandByName(destinoId);

        List<RotaDTO> results = new ArrayList<>(findRaptorRoutes(origemIds, destinoIds, serviceId, queryMinutes));

        RotaDTO walkRoute = buildDirectWalkCandidate(origemId, destinoId, queryMinutes);
        if (walkRoute != null) {
            boolean transitWinsOrTies = results.stream().anyMatch(r -> r.getTotalMinutos() <= walkRoute.getTotalMinutos()
                    && r.getNrZonas() <= walkRoute.getNrZonas());
            if (!transitWinsOrTies) {
                results.add(walkRoute);
            }
        }

        results = dedupeRoutes(results);
        results.sort(routeComparator());

        List<RotaDTO> finalResults = results.stream().limit(MAX_RESULTS).collect(Collectors.toList());
        log.info("Route planning complete: {} results in {}ms", finalResults.size(), System.currentTimeMillis() - start);
        return finalResults;
    }

    public List<ProximoPasseDTO> findProximosPasses(Long trajetoId, Long paragemId, String time, String day) {
        return findProximosPasses(trajetoId, paragemId, parseTimeOrDefault(time), parseDayOrDefault(day));
    }

    private List<RotaDTO> findRaptorRoutes(Set<Long> origemIds, Set<Long> destinoIds, String serviceId, int queryMinutes) {
        Map<Long, List<Viagem>> viagensByTrajeto = viagensByServiceAndTrajeto.getOrDefault(serviceId, Map.of());
        Map<Long, List<Label>> bestLabelsByStop = new HashMap<>();
        Map<Long, List<Label>> previousRound = new HashMap<>();
        Set<Long> markedStops = new HashSet<>();

        for (Long origemId : origemIds) {
            if (!paragemCache.containsKey(origemId)) continue;
            Label source = new Label(origemId, queryMinutes, 0, zonesWithStop(Set.of(), origemId),
                    null, LegType.SOURCE, null, -1, -1, -1, 0);
            if (addParetoLabel(previousRound, bestLabelsByStop, source)) {
                markedStops.add(origemId);
            }
        }

        if (markedStops.isEmpty()) {
            return List.of();
        }

        markedStops.addAll(relaxFootpaths(markedStops, previousRound, bestLabelsByStop));

        while (!markedStops.isEmpty()) {
            Map<Long, Integer> routesToScan = buildRoutesToScan(markedStops);
            if (routesToScan.isEmpty()) break;

            Map<Long, List<Label>> currentRound = copyLabelMap(previousRound);
            Set<Long> transitMarked = new HashSet<>();

            for (Map.Entry<Long, Integer> entry : routesToScan.entrySet()) {
                Long trajetoId = entry.getKey();
                List<Viagem> viagens = viagensByTrajeto.getOrDefault(trajetoId, List.of());
                scanRoute(trajetoId, entry.getValue(), viagens, previousRound, currentRound, bestLabelsByStop, transitMarked);
            }

            if (transitMarked.isEmpty()) break;

            Set<Long> nextMarked = new HashSet<>(transitMarked);
            nextMarked.addAll(relaxFootpaths(transitMarked, currentRound, bestLabelsByStop));

            previousRound = currentRound;
            markedStops = nextMarked;
        }

        List<Label> destinationLabels = collectDestinationLabels(destinoIds, bestLabelsByStop);
        return destinationLabels.stream()
                .map(label -> buildRouteFromLabel(label, queryMinutes))
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toList(), routes -> {
                    List<RotaDTO> deduped = dedupeRoutes(routes);
                    deduped.sort(routeComparator());
                    return deduped.stream().limit(MAX_RESULTS).collect(Collectors.toList());
                }));
    }

    private Map<Long, Integer> buildRoutesToScan(Set<Long> markedStops) {
        Map<Long, Integer> routesToScan = new HashMap<>();
        for (Long stopId : markedStops) {
            for (Long trajetoId : paragemToTrajetoIds.getOrDefault(stopId, Set.of())) {
                Integer stopIndex = trajetoStopIndexMap.getOrDefault(trajetoId, Map.of()).get(stopId);
                if (stopIndex != null) {
                    routesToScan.merge(trajetoId, stopIndex, Math::min);
                }
            }
        }
        return routesToScan;
    }

    private void scanRoute(Long trajetoId,
                           int startIndex,
                           List<Viagem> viagens,
                           Map<Long, List<Label>> previousRound,
                           Map<Long, List<Label>> currentRound,
                           Map<Long, List<Label>> bestLabelsByStop,
                           Set<Long> markedStops) {
        List<PontosDePassagem> pontos = trajetoPontosMap.get(trajetoId);
        if (pontos == null || pontos.isEmpty() || viagens.isEmpty() || startIndex >= pontos.size()) return;

        List<BoardingCandidate> onboard = new ArrayList<>();
        Map<Long, Integer> offsets = trajetoOffsetMap.getOrDefault(trajetoId, Map.of());

        for (int i = startIndex; i < pontos.size(); i++) {
            PontosDePassagem ponto = pontos.get(i);
            if (ponto.getParagem() == null) continue;
            Long stopId = ponto.getParagem().getId();
            Integer stopOffset = offsets.get(stopId);
            if (stopOffset == null) continue;

            for (BoardingCandidate candidate : onboard) {
                if (i <= candidate.boardIndex) continue;
                int arrival = candidate.depAbsolute + stopOffset;
                Label rideLabel = new Label(
                        stopId,
                        arrival,
                        candidate.boardLabel.rideCount + 1,
                        zonesWithRouteSegment(candidate.boardLabel.zoneNums, pontos, candidate.boardIndex, i),
                        candidate.boardLabel,
                        LegType.RIDE,
                        trajetoId,
                        candidate.departureAtBoard,
                        candidate.boardIndex,
                        i,
                        0
                );
                if (addParetoLabel(currentRound, bestLabelsByStop, rideLabel)) {
                    markedStops.add(stopId);
                }
            }

            List<Label> boardLabels = previousRound.getOrDefault(stopId, List.of());
            for (Label boardLabel : boardLabels) {
                BoardingCandidate candidate = findBoardingCandidate(viagens, stopOffset, boardLabel, i);
                if (candidate != null && onboard.stream().noneMatch(existing -> sameBoardingCandidate(existing, candidate))) {
                    onboard.add(candidate);
                }
            }
        }
    }

    private BoardingCandidate findBoardingCandidate(List<Viagem> viagens, int stopOffset, Label boardLabel, int boardIndex) {
        int boardReady = boardLabel.arrivalMinutes + (boardLabel.rideCount > 0 ? TRANSFER_BUFFER_MINUTES : 0);
        int minDepTOD = Math.floorMod(boardReady - stopOffset, 1440);
        LocalTime minDepTime = LocalTime.of(minDepTOD / 60, minDepTOD % 60);

        Viagem trip = findNextDeparture(viagens, minDepTime);
        if (trip == null) return null;

        int depAbsolute = toMinutes(trip.getHoraPartida());
        while (depAbsolute < boardReady - stopOffset) depAbsolute += 1440;

        int departureAtBoard = depAbsolute + stopOffset;
        if (departureAtBoard < boardReady || departureAtBoard - boardReady > MAX_WAIT_MINUTES) return null;

        return new BoardingCandidate(boardLabel, boardIndex, depAbsolute, departureAtBoard);
    }

    private boolean sameBoardingCandidate(BoardingCandidate a, BoardingCandidate b) {
        return a.boardIndex == b.boardIndex
                && a.depAbsolute == b.depAbsolute
                && a.boardLabel.stopId.equals(b.boardLabel.stopId)
                && a.boardLabel.arrivalMinutes == b.boardLabel.arrivalMinutes
                && a.boardLabel.rideCount == b.boardLabel.rideCount
                && a.boardLabel.zoneNums.equals(b.boardLabel.zoneNums);
    }

    private Map<Long, List<Label>> copyLabelMap(Map<Long, List<Label>> source) {
        Map<Long, List<Label>> copy = new HashMap<>();
        for (Map.Entry<Long, List<Label>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    private Set<Long> relaxFootpaths(Set<Long> sourceStops,
                                     Map<Long, List<Label>> roundLabels,
                                     Map<Long, List<Label>> bestLabelsByStop) {
        Set<Long> markedStops = new HashSet<>();

        for (Long fromStopId : sourceStops) {
            List<Label> labels = new ArrayList<>(roundLabels.getOrDefault(fromStopId, List.of()));
            for (Label fromLabel : labels) {
                for (WalkEdge edge : getWalkEdges(fromStopId)) {
                    int arrival = fromLabel.arrivalMinutes + edge.minutes();
                    Label walkLabel = new Label(
                            edge.toId(),
                            arrival,
                            fromLabel.rideCount,
                            zonesWithStop(fromLabel.zoneNums, edge.toId()),
                            fromLabel,
                            LegType.WALK,
                            null,
                            -1,
                            -1,
                            -1,
                            edge.minutes()
                    );
                    if (addParetoLabel(roundLabels, bestLabelsByStop, walkLabel)) {
                        markedStops.add(edge.toId());
                    }
                }
            }
        }

        return markedStops;
    }

    private boolean addParetoLabel(Map<Long, List<Label>> roundLabels,
                                   Map<Long, List<Label>> bestLabelsByStop,
                                   Label candidate) {
        List<Label> globalLabels = bestLabelsByStop.computeIfAbsent(candidate.stopId, k -> new ArrayList<>());
        for (Label existing : globalLabels) {
            if (dominates(existing, candidate) || sameParetoState(existing, candidate)) {
                return false;
            }
        }

        globalLabels.removeIf(existing -> dominates(candidate, existing));
        globalLabels.add(candidate);

        List<Label> currentRoundLabels = roundLabels.computeIfAbsent(candidate.stopId, k -> new ArrayList<>());
        currentRoundLabels.removeIf(existing -> dominates(candidate, existing) || sameParetoState(existing, candidate));
        currentRoundLabels.add(candidate);
        return true;
    }

    private boolean dominates(Label a, Label b) {
        return a.arrivalMinutes <= b.arrivalMinutes
                && a.rideCount <= b.rideCount
                && a.zoneNums.size() <= b.zoneNums.size()
                && (a.arrivalMinutes < b.arrivalMinutes
                || a.rideCount < b.rideCount
                || a.zoneNums.size() < b.zoneNums.size());
    }

    private boolean sameParetoState(Label a, Label b) {
        return a.arrivalMinutes == b.arrivalMinutes
                && a.rideCount == b.rideCount
                && a.zoneNums.equals(b.zoneNums);
    }

    private List<Label> collectDestinationLabels(Set<Long> destinoIds, Map<Long, List<Label>> bestLabelsByStop) {
        List<Label> labels = new ArrayList<>();
        for (Long destinoId : destinoIds) {
            for (Label label : bestLabelsByStop.getOrDefault(destinoId, List.of())) {
                if (label.rideCount > 0) labels.add(label);
            }
        }

        List<Label> pareto = new ArrayList<>();
        for (Label label : labels) {
            boolean dominated = pareto.stream().anyMatch(existing -> dominates(existing, label) || sameParetoState(existing, label));
            if (dominated) continue;
            pareto.removeIf(existing -> dominates(label, existing));
            pareto.add(label);
        }
        pareto.sort(Comparator
                .comparingInt((Label l) -> Math.max(0, l.rideCount - 1))
                .thenComparingInt(l -> l.arrivalMinutes)
                .thenComparingInt(l -> l.zoneNums.size()));
        return pareto;
    }

    private RotaDTO buildRouteFromLabel(Label destinationLabel, int queryMinutes) {
        List<Label> chain = new ArrayList<>();
        Label current = destinationLabel;
        while (current != null && current.legType != LegType.SOURCE) {
            chain.add(0, current);
            current = current.previous;
        }

        if (chain.isEmpty()) return null;

        List<RotaDTO.SegmentoDTO> segmentos = new ArrayList<>();
        for (Label label : chain) {
            RotaDTO.SegmentoDTO segment = switch (label.legType) {
                case WALK -> buildWalkSegment(label.previous, label);
                case RIDE -> buildRideSegment(label.previous, label);
                case SOURCE -> null;
            };
            if (segment != null) segmentos.add(segment);
        }

        if (segmentos.isEmpty()) return null;

        RotaDTO rota = assembleRota(segmentos, false);
        rota.setCaminho(segmentos.size() == 1 && "A pe".equals(segmentos.get(0).getLinhaNome()));
        boolean hasWalkLeg = segmentos.stream().anyMatch(seg -> "A pe".equals(seg.getLinhaNome()));
        long rideSegments = segmentos.stream().filter(seg -> !"A pe".equals(seg.getLinhaNome())).count();
        rota.setDireta(rideSegments == 1 && !hasWalkLeg);
        rota.setTotalMinutos(Math.max(0, destinationLabel.arrivalMinutes - queryMinutes));
        rota.setZonas(toZonaResumoDtos(destinationLabel.zoneNums));
        rota.setNrZonas(destinationLabel.zoneNums.size());
        return rota;
    }

    private RotaDTO.SegmentoDTO buildRideSegment(Label boardLabel, Label rideLabel) {
        if (boardLabel == null || rideLabel.trajetoId == null) return null;
        List<PontosDePassagem> pontos = trajetoPontosMap.get(rideLabel.trajetoId);
        if (pontos == null || rideLabel.boardIndex < 0 || rideLabel.alightIndex < rideLabel.boardIndex || rideLabel.alightIndex >= pontos.size()) {
            return null;
        }

        RotaDTO.SegmentoDTO seg = buildSegmentDTO(rideLabel.trajetoId, trajetoCache.get(rideLabel.trajetoId), pontos,
                rideLabel.boardIndex, rideLabel.alightIndex);
        seg.setHoraPartida(minutesToTime(rideLabel.departureMinutes));
        seg.setHoraChegada(minutesToTime(rideLabel.arrivalMinutes));
        seg.setDuracaoMinutos(Math.max(1, rideLabel.arrivalMinutes - rideLabel.departureMinutes));
        seg.setEsperaMinutos(Math.max(0, rideLabel.departureMinutes - boardLabel.arrivalMinutes));
        Set<Integer> segmentZones = zonesWithRouteSegment(Set.of(), pontos, rideLabel.boardIndex, rideLabel.alightIndex);
        seg.setZonas(toZonaResumoDtos(segmentZones));
        seg.setNrZonas(segmentZones.size());
        return seg;
    }

    private RotaDTO.SegmentoDTO buildWalkSegment(Label fromLabel, Label walkLabel) {
        if (fromLabel == null || walkLabel == null) return null;

        Paragem from = paragemCache.get(fromLabel.stopId);
        Paragem to = paragemCache.get(walkLabel.stopId);
        if (from == null || to == null) return null;

        RotaDTO.SegmentoDTO seg = new RotaDTO.SegmentoDTO();
        seg.setLinhaNome("A pe");
        seg.setOrigem(toParagemDTO(from));
        seg.setDestino(toParagemDTO(to));
        seg.setParagens(List.of(toParagemDTO(from), toParagemDTO(to)));
        seg.setDuracaoMinutos(Math.max(0, walkLabel.arrivalMinutes - fromLabel.arrivalMinutes));
        seg.setTempoCaminhadaMinutos(Math.max(0, walkLabel.walkMinutes));
        seg.setHoraPartida(minutesToTime(fromLabel.arrivalMinutes));
        seg.setHoraChegada(minutesToTime(walkLabel.arrivalMinutes));
        Set<Integer> segmentZones = zonesWithStop(zonesWithStop(Set.of(), fromLabel.stopId), walkLabel.stopId);
        seg.setZonas(toZonaResumoDtos(segmentZones));
        seg.setNrZonas(segmentZones.size());
        return seg;
    }

    private RotaDTO assembleRota(List<RotaDTO.SegmentoDTO> segmentos, boolean direta) {
        int totalRide = segmentos.stream()
                .filter(s -> !"A pe".equals(s.getLinhaNome()))
                .mapToInt(RotaDTO.SegmentoDTO::getDuracaoMinutos)
                .sum();
        int totalWalk = segmentos.stream().mapToInt(RotaDTO.SegmentoDTO::getTempoCaminhadaMinutos).sum();
        int totalWait = segmentos.stream().mapToInt(RotaDTO.SegmentoDTO::getEsperaMinutos).sum();

        long rideSegments = segmentos.stream()
                .filter(s -> !"A pe".equals(s.getLinhaNome()))
                .count();
        int trocas = Math.max(0, (int) rideSegments - 1);

        Set<Integer> routeZones = new TreeSet<>();
        for (RotaDTO.SegmentoDTO segmento : segmentos) {
            if (segmento.getZonas() == null) continue;
            for (ZonaResumoDTO zona : segmento.getZonas()) {
                routeZones.add(zona.getNum());
            }
        }

        RotaDTO rota = new RotaDTO();
        rota.setTotalMinutos(totalRide + totalWalk + totalWait);
        rota.setTrocas(trocas);
        rota.setTotalCaminhadaMinutos(totalWalk);
        rota.setDireta(direta);
        rota.setSegmentos(segmentos);
        rota.setZonas(toZonaResumoDtos(routeZones));
        rota.setNrZonas(routeZones.size());

        if (!segmentos.isEmpty()) {
            rota.setHoraPartida(segmentos.get(0).getHoraPartida());
            rota.setHoraChegada(segmentos.get(segmentos.size() - 1).getHoraChegada());
        }

        return rota;
    }

    private RotaDTO buildDirectWalkCandidate(Long origemId, Long destinoId, int queryMinutes) {
        Paragem from = paragemCache.get(origemId);
        Paragem to = paragemCache.get(destinoId);
        double walkDistKm = walkDistanceKm(from, to);
        if (walkDistKm <= 0 || walkDistKm > MAX_WALKING_DISTANCE_KM) return null;
        int walkMin = estimateWalkingMinutes(from, to);
        return buildWalkingRoute(from, to, walkMin, queryMinutes);
    }

    public List<ProximoPasseDTO> findProximosPasses(Long trajetoId, Long paragemId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        ensureInitialized();

        requireTrajeto(trajetoId);
        requireParagem(paragemId);

        String serviceId = resolveServiceId(dayOfWeek);
        int queryMinutes = toMinutes(queryTime);

        Map<Long, List<Viagem>> viagensByTrajeto = viagensByServiceAndTrajeto.getOrDefault(serviceId, Map.of());
        List<Viagem> viagens = viagensByTrajeto.getOrDefault(trajetoId, List.of());

        Map<Long, Integer> offsets = trajetoOffsetMap.getOrDefault(trajetoId, Map.of());
        Integer offset = offsets.get(paragemId);
        if (offset == null) {
            throw new RecursoNaoEncontradoException("Paragem nao encontrada");
        }

        List<ProximoPasseDTO> result = new ArrayList<>();
        for (Viagem v : viagens) {
            int depTOD = toMinutes(v.getHoraPartida());
            int depAtStop = depTOD + offset;
            int wait = depAtStop - queryMinutes;
            if (wait < 0) wait += 1440;
            if (wait > MAX_WAIT_MINUTES) continue;
            result.add(new ProximoPasseDTO(minutesToTime(depAtStop), wait));
            if (result.size() >= 10) break;
        }

        result.sort(Comparator.comparingInt(ProximoPasseDTO::getEsperaMinutos));
        return result;
    }

    public ParagemProximasPassagensDTO findProximasPassagensPorParagem(Long paragemId, String time, String day) {
        return findProximasPassagensPorParagem(paragemId, parseTimeOrDefault(time), parseDayOrDefault(day));
    }

    public ParagemProximasPassagensDTO findProximasPassagensPorParagem(Long paragemId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        ensureInitialized();

        requireParagem(paragemId);

        Paragem paragem = paragemCache.get(paragemId);
        if (paragem == null) {
            throw new RecursoNaoEncontradoException("Paragem nao encontrada");
        }

        Map<Long, List<PontosDePassagem>> pontosByTrajeto = new HashMap<>();
        for (PontosDePassagem ponto : paragemToPontos.getOrDefault(paragemId, List.of())) {
            Long trajetoId = pontoToTrajeto.get(ponto.getId());
            if (trajetoId != null) {
                pontosByTrajeto.computeIfAbsent(trajetoId, k -> new ArrayList<>()).add(ponto);
            }
        }

        Map<Long, ParagemProximasPassagensDTO.LinhaProximasPassagensDTO> linhasById = new TreeMap<>();
        for (Long trajetoId : pontosByTrajeto.keySet()) {
            Trajeto trajeto = trajetoCache.get(trajetoId);
            if (trajeto == null || trajeto.getLinha() == null) {
                continue;
            }

            ParagemProximasPassagensDTO.TrajetoProximasPassagensDTO trajetoDTO = new ParagemProximasPassagensDTO.TrajetoProximasPassagensDTO();
            trajetoDTO.setTrajetoId(trajetoId);
            trajetoDTO.setDirecao(trajeto.getDirecao() != null ? trajeto.getDirecao().name() : "");
            trajetoDTO.setDestinoFinal(destinoFinalMap.getOrDefault(trajetoId, ""));
            trajetoDTO.setProximosPasses(findProximosPasses(trajetoId, paragemId, queryTime, dayOfWeek));

            ParagemProximasPassagensDTO.LinhaProximasPassagensDTO linhaDTO = linhasById.computeIfAbsent(
                    trajeto.getLinha().getId(),
                    id -> {
                        ParagemProximasPassagensDTO.LinhaProximasPassagensDTO dto = new ParagemProximasPassagensDTO.LinhaProximasPassagensDTO();
                        dto.setLinhaId(trajeto.getLinha().getId());
                        dto.setLinhaNome(trajeto.getLinha().getNome());
                        dto.setTrajetos(new ArrayList<>());
                        return dto;
                    }
            );
            linhaDTO.getTrajetos().add(trajetoDTO);
        }

        ParagemProximasPassagensDTO result = new ParagemProximasPassagensDTO();
        result.setParagemId(paragemId);
        result.setParagemNome(paragem.getNome());
        result.setLinhas(new ArrayList<>(linhasById.values()));
        return result;
    }

    public List<HorarioDTO> findHorarios(Long linhaId, String serviceId) {
        ensureInitialized();
        serviceId = normalizeServiceId(serviceId);

        List<Trajeto> trajetos = trajetoCache.values().stream()
                .filter(t -> t.getLinha() != null && t.getLinha().getId().equals(linhaId))
                .collect(Collectors.toList());

        if (trajetos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Linha nao encontrada");
        }

        Map<Long, List<Viagem>> viagensByTrajeto = viagensByServiceAndTrajeto.getOrDefault(serviceId, Map.of());

        List<HorarioDTO> result = new ArrayList<>();
        for (Trajeto t : trajetos) {
            List<Viagem> viagens = viagensByTrajeto.getOrDefault(t.getId(), List.of());
            String destinoFinal = destinoFinalMap.getOrDefault(t.getId(), "");

            List<String> partidas = viagens.stream()
                    .map(v -> v.getHoraPartida().format(TIME_FMT))
                    .collect(Collectors.toList());

            result.add(new HorarioDTO(t.getId(), t.getDirecao().name(), destinoFinal, partidas));
        }

        return result;
    }

    public List<HorarioParagemDTO> findHorariosPorParagem(Long linhaId, String serviceId) {
        ensureInitialized();
        serviceId = normalizeServiceId(serviceId);

        List<Trajeto> trajetos = trajetoCache.values().stream()
                .filter(t -> t.getLinha() != null && t.getLinha().getId().equals(linhaId))
                .collect(Collectors.toList());

        if (trajetos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Linha nao encontrada");
        }

        Map<Long, List<Viagem>> viagensByTrajeto = viagensByServiceAndTrajeto.getOrDefault(serviceId, Map.of());

        List<HorarioParagemDTO> result = new ArrayList<>();
        for (Trajeto t : trajetos) {
            List<PontosDePassagem> pontos = trajetoPontosMap.getOrDefault(t.getId(), List.of());
            List<Viagem> viagens = viagensByTrajeto.getOrDefault(t.getId(), List.of());

            HorarioParagemDTO dto = new HorarioParagemDTO();
            dto.setTrajetoId(t.getId());
            dto.setDirecao(t.getDirecao().name());
            dto.setDestinoFinal(destinoFinalMap.getOrDefault(t.getId(), ""));
            dto.setLinhaNome(t.getLinha() != null ? t.getLinha().getNome() : "");

            List<HorarioParagemDTO.ParagemHorarioDTO> paragensHorario = new ArrayList<>();
            for (PontosDePassagem p : pontos) {
                HorarioParagemDTO.ParagemHorarioDTO ph = new HorarioParagemDTO.ParagemHorarioDTO();
                ph.setParagemId(p.getParagem().getId());
                ph.setNome(p.getParagem().getNome());
                ph.setTempoDesdeInicio(p.getTempoDesdeInicio());

                Map<Integer, List<Integer>> horarios = new TreeMap<>();
                for (Viagem v : viagens) {
                    int depMin = toMinutes(v.getHoraPartida()) + p.getTempoDesdeInicio();
                    int hour = (depMin / 60) % 24;
                    int minute = depMin % 60;
                    horarios.computeIfAbsent(hour, k -> new ArrayList<>()).add(minute);
                }
                for (var entry : horarios.entrySet()) {
                    Collections.sort(entry.getValue());
                }
                ph.setHorarios(horarios);
                paragensHorario.add(ph);
            }
            dto.setParagens(paragensHorario);
            result.add(dto);
        }

        return result;
    }

    private Viagem findNextDeparture(List<Viagem> viagens, LocalTime afterTime) {
        if (viagens == null || viagens.isEmpty()) return null;

        int lo = 0, hi = viagens.size();
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (viagens.get(mid).getHoraPartida().isBefore(afterTime)) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        if (lo < viagens.size()) return viagens.get(lo);
        return viagens.get(0);
    }

    private RotaDTO.SegmentoDTO buildSegmentDTO(Long trajetoId, Trajeto trajeto, List<PontosDePassagem> pontos,
                                                int origemIdx, int destinoIdx) {
        RotaDTO.SegmentoDTO seg = new RotaDTO.SegmentoDTO();
        seg.setTrajetoId(trajetoId);

        if (trajeto != null) {
            seg.setLinhaNome(trajeto.getLinha() != null ? trajeto.getLinha().getNome() : "Linha");
            seg.setDirecao(trajeto.getDirecao() != null ? trajeto.getDirecao().name() : "");
        }
        seg.setDestinoFinal(destinoFinalMap.getOrDefault(trajetoId, ""));

        PontosDePassagem origemPonto = pontos.get(origemIdx);
        PontosDePassagem destinoPonto = pontos.get(destinoIdx);
        Paragem origemParagem = paragemCache.get(origemPonto.getParagem().getId());
        Paragem destinoParagem = paragemCache.get(destinoPonto.getParagem().getId());

        if (origemParagem != null) seg.setOrigem(toParagemDTO(origemParagem));
        if (destinoParagem != null) seg.setDestino(toParagemDTO(destinoParagem));

        List<RotaDTO.ParagemDTO> paragens = new ArrayList<>();
        for (int i = origemIdx; i <= destinoIdx; i++) {
            Paragem p = paragemCache.get(pontos.get(i).getParagem().getId());
            if (p != null) paragens.add(toParagemDTO(p));
        }
        seg.setParagens(paragens);

        return seg;
    }

    private Set<Long> expandByName(Long paragemId) {
        Set<Long> ids = new HashSet<>();
        ids.add(paragemId);
        Paragem p = paragemCache.get(paragemId);
        if (p != null) {
            String groupKey = normalizeStopGroupKey(p.getNome());
            if (nomeToIds.containsKey(groupKey)) {
                ids.addAll(nomeToIds.get(groupKey));
            }
        }
        return ids;
    }

    private List<WalkEdge> getWalkEdges(Long stopId) {
        Map<Long, Integer> bestWalk = new HashMap<>();
        for (WalkEdge edge : nearbyStopsMap.getOrDefault(stopId, List.of())) {
            bestWalk.merge(edge.toId(), edge.minutes(), Math::min);
        }

        Paragem stop = paragemCache.get(stopId);
        if (stop != null) {
            String groupKey = normalizeStopGroupKey(stop.getNome());
            for (Long sameNameId : nomeToIds.getOrDefault(groupKey, Set.of())) {
                if (!sameNameId.equals(stopId)) {
                    bestWalk.merge(sameNameId, 0, Math::min);
                }
            }
        }

        return bestWalk.entrySet().stream()
                .map(entry -> new WalkEdge(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(WalkEdge::minutes))
                .collect(Collectors.toList());
    }

    private Set<Integer> zonesWithRouteSegment(Set<Integer> baseZones, List<PontosDePassagem> pontos, int startIdx, int endIdx) {
        Set<Integer> zones = new TreeSet<>(baseZones);
        for (int i = Math.max(0, startIdx); i <= endIdx && i < pontos.size(); i++) {
            if (pontos.get(i).getParagem() != null) {
                addStopZone(zones, pontos.get(i).getParagem().getId());
            }
        }
        return immutableZones(zones);
    }

    private Set<Integer> zonesWithStop(Set<Integer> baseZones, Long stopId) {
        Set<Integer> zones = new TreeSet<>(baseZones);
        addStopZone(zones, stopId);
        return immutableZones(zones);
    }

    private void addStopZone(Set<Integer> zones, Long stopId) {
        Paragem stop = paragemCache.get(stopId);
        if (stop != null && stop.getZona() != null) {
            zones.add(stop.getZona().getNum());
            zoneNameByNum.putIfAbsent(stop.getZona().getNum(), stop.getZona().getNome());
        }
    }

    private static Set<Integer> immutableZones(Set<Integer> zones) {
        return Collections.unmodifiableSet(new TreeSet<>(zones));
    }

    static String normalizeStopGroupKey(String stopName) {
        if (stopName == null) {
            return "";
        }
        String normalized = Normalizer.normalize(stopName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .trim();
        normalized = GENERATED_SUFFIX_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized.replaceAll("[^A-Z0-9]+", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? stopName.toUpperCase(Locale.ROOT).trim() : normalized;
    }

    private List<ZonaResumoDTO> toZonaResumoDtos(Set<Integer> zoneNums) {
        if (zoneNums == null || zoneNums.isEmpty()) return List.of();
        return zoneNums.stream()
                .sorted()
                .map(num -> new ZonaResumoDTO(num, zoneNameByNum.getOrDefault(num, "Zona " + num)))
                .collect(Collectors.toList());
    }

    private String resolveServiceId(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SATURDAY -> "SAB";
            case SUNDAY -> "DOM";
            default -> "UTEIS";
        };
    }

    private LocalTime parseTimeOrDefault(String time) {
        if (time == null || time.isBlank()) {
            return LocalTime.now();
        }
        try {
            return LocalTime.parse(time, TIME_FMT);
        } catch (Exception e) {
            throw new PedidoInvalidoException("Hora invalida");
        }
    }

    private DayOfWeek parseDayOrDefault(String day) {
        if (day == null || day.isBlank()) {
            return LocalDate.now().getDayOfWeek();
        }
        try {
            return DayOfWeek.valueOf(day.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PedidoInvalidoException("Dia invalido");
        }
    }

    private String normalizeServiceId(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            return "UTEIS";
        }
        return serviceId.trim().toUpperCase();
    }

    private void requireTrajeto(Long trajetoId) {
        if (!trajetoCache.containsKey(trajetoId)) {
            throw new RecursoNaoEncontradoException("Trajeto nao encontrado");
        }
    }

    private void requireParagem(Long paragemId) {
        if (!paragemCache.containsKey(paragemId)) {
            throw new RecursoNaoEncontradoException("Paragem nao encontrada");
        }
    }

    private int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private String minutesToTime(int totalMinutes) {
        int minutesInDay = Math.floorMod(totalMinutes, 1440);
        int h = minutesInDay / 60;
        int m = minutesInDay % 60;
        return String.format("%02d:%02d", h, m);
    }

    private int estimateWalkingMinutes(Paragem from, Paragem to) {
        if (from == null || to == null) return 0;
        if (from.getLocalizacao() == null || to.getLocalizacao() == null) return 0;
        if (from.getId().equals(to.getId())) return 0;

        double distanceKm = haversineKm(
                from.getLocalizacao().getLatitude(), from.getLocalizacao().getLongitude(),
                to.getLocalizacao().getLatitude(), to.getLocalizacao().getLongitude());
        return Math.max(1, (int) Math.round(distanceKm / WALKING_SPEED_KMH * 60));
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double walkDistanceKm(Paragem from, Paragem to) {
        if (from == null || to == null) return Double.MAX_VALUE;
        if (from.getLocalizacao() == null || to.getLocalizacao() == null) return Double.MAX_VALUE;
        if (from.getId().equals(to.getId())) return 0;
        return haversineKm(
                from.getLocalizacao().getLatitude(), from.getLocalizacao().getLongitude(),
                to.getLocalizacao().getLatitude(), to.getLocalizacao().getLongitude());
    }

    private RotaDTO.ParagemDTO toParagemDTO(Paragem p) {
        if (p == null) return null;
        Double lat = p.getLocalizacao() != null ? p.getLocalizacao().getLatitude() : null;
        Double lon = p.getLocalizacao() != null ? p.getLocalizacao().getLongitude() : null;
        return new RotaDTO.ParagemDTO(p.getId(), p.getNome(), lat, lon);
    }

    private RotaDTO buildWalkingRoute(Paragem from, Paragem to, int walkMin, int queryMinutes) {
        if (from == null || to == null || walkMin <= 0) return null;

        RotaDTO.SegmentoDTO seg = new RotaDTO.SegmentoDTO();
        seg.setLinhaNome("A pe");
        seg.setOrigem(toParagemDTO(from));
        seg.setDestino(toParagemDTO(to));
        seg.setParagens(List.of(toParagemDTO(from), toParagemDTO(to)));
        seg.setDuracaoMinutos(walkMin);
        seg.setTempoCaminhadaMinutos(walkMin);
        seg.setHoraPartida(minutesToTime(queryMinutes));
        seg.setHoraChegada(minutesToTime(queryMinutes + walkMin));
        Set<Integer> segmentZones = zonesWithStop(zonesWithStop(Set.of(), from.getId()), to.getId());
        seg.setZonas(toZonaResumoDtos(segmentZones));
        seg.setNrZonas(segmentZones.size());

        RotaDTO rota = new RotaDTO();
        rota.setTotalMinutos(walkMin);
        rota.setTrocas(0);
        rota.setTotalCaminhadaMinutos(walkMin);
        rota.setDireta(true);
        rota.setCaminho(true);
        rota.setSegmentos(List.of(seg));
        rota.setHoraPartida(minutesToTime(queryMinutes));
        rota.setHoraChegada(minutesToTime(queryMinutes + walkMin));
        rota.setZonas(toZonaResumoDtos(segmentZones));
        rota.setNrZonas(segmentZones.size());
        return rota;
    }

    private List<RotaDTO> dedupeRoutes(List<RotaDTO> routes) {
        Map<String, RotaDTO> bestByKey = new LinkedHashMap<>();
        for (RotaDTO route : routes) {
            String key = buildRouteKey(route);
            RotaDTO existing = bestByKey.get(key);
            if (existing == null || routeComparator().compare(route, existing) < 0) {
                bestByKey.put(key, route);
            }
        }
        return new ArrayList<>(bestByKey.values());
    }

    private String buildRouteKey(RotaDTO rota) {
        if (rota.getSegmentos() == null) return "empty";
        return rota.getSegmentos().stream()
                .map(s -> {
                    String o = s.getOrigem() != null ? String.valueOf(s.getOrigem().getId()) : "?";
                    String d = s.getDestino() != null ? String.valueOf(s.getDestino().getId()) : "?";
                    String zones = s.getZonas() == null ? "" : s.getZonas().stream()
                            .map(z -> String.valueOf(z.getNum()))
                            .collect(Collectors.joining(","));
                    return s.getLinhaNome() + ":" + s.getDirecao() + ":" + o + ":" + d + ":" + s.getHoraPartida() + ":" + s.getHoraChegada() + ":" + zones;
                })
                .collect(Collectors.joining("|"));
    }

    private Comparator<RotaDTO> routeComparator() {
        return Comparator
                .comparingInt(RotaDTO::getTrocas)
                .thenComparingInt(RotaDTO::getTotalMinutos)
                .thenComparingInt(RotaDTO::getNrZonas);
    }
}
