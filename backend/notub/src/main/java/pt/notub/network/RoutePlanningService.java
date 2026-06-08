package pt.notub.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pt.notub.network.dto.*;
import pt.notub.models.*;
import pt.notub.repositories.PontosDePassagemRepository;
import pt.notub.repositories.TrajetoRepository;
import pt.notub.repositories.ViagemRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoutePlanningService {

    private static final Logger log = LoggerFactory.getLogger(RoutePlanningService.class);

    private static final int TRANSFER_BUFFER_MINUTES = 2;
    private static final double WALKING_SPEED_KMH = 3.0;
    private static final double MAX_WALKING_DISTANCE_KM = 1.0;
    private static final int MAX_WAIT_MINUTES = 180;
    private static final int MAX_RESULTS = 3;
    private static final int MAX_TRANSFERS = 2;
    private static final int MAX_STATES = 2000;
    private static final double MAX_BUS_SPEED_KMH = 40.0;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final double NEARBY_RADIUS_KM = 0.5;
    private static final int MAX_NEARBY = 5;

    private record State(long paragemId, long trajetoId) {}

    private record WalkEdge(long toId, int minutes) {}

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
    private Map<Long, List<PontosDePassagem>> paragemToPontos;
    private Map<Long, Long> pontoToTrajeto;
    private Map<String, Map<Long, List<Viagem>>> viagensByServiceAndTrajeto;
    private Set<String> transferNamesSet;
    private Map<Long, List<WalkEdge>> nearbyStopsMap;

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
        for (PontosDePassagem p : allPontos) {
            if (p.getParagem() != null) {
                paragemCache.putIfAbsent(p.getParagem().getId(), p.getParagem());
                nomeToIds.computeIfAbsent(p.getParagem().getNome(), k -> new HashSet<>())
                        .add(p.getParagem().getId());
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
        paragemToPontos = new HashMap<>();
        pontoToTrajeto = new HashMap<>();
        for (var entry : trajetoPontosMap.entrySet()) {
            Long trajetoId = entry.getKey();
            Map<Long, Integer> offsets = new HashMap<>();
            for (PontosDePassagem p : entry.getValue()) {
                offsets.put(p.getParagem().getId(), p.getTempoDesdeInicio());
                paragemToPontos.computeIfAbsent(p.getParagem().getId(), k -> new ArrayList<>()).add(p);
                pontoToTrajeto.put(p.getId(), trajetoId);
            }
            trajetoOffsetMap.put(trajetoId, offsets);
        }

        viagensByServiceAndTrajeto = new HashMap<>();
        for (Viagem v : allViagens) {
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

        transferNamesSet = new HashSet<>();
        for (Map.Entry<String, Set<Long>> entry : nomeToIds.entrySet()) {
            String nomeParagem = entry.getKey();
            Set<Long> idsDoGrupo = entry.getValue();

            Set<Long> trajetosNoGrupo = new HashSet<>();
            for (Long id : idsDoGrupo) {
                for (PontosDePassagem p : paragemToPontos.getOrDefault(id, List.of())) {
                    Long tid = pontoToTrajeto.get(p.getId());
                    if (tid != null) trajetosNoGrupo.add(tid);
                }
            }
            if (trajetosNoGrupo.size() >= 2) {
                transferNamesSet.add(nomeParagem);
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
                if (Math.abs(dlat) > 0.01 || Math.abs(dlon) > 0.02) continue;
                double dist = haversineKm(p1.getLocalizacao().getLatitude(), p1.getLocalizacao().getLongitude(),
                        p2.getLocalizacao().getLatitude(), p2.getLocalizacao().getLongitude());
                if (dist <= NEARBY_RADIUS_KM) {
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
        log.info("Built {} walk edges for {} paragens (radius={}km)", edgeCount, nearbyStopsMap.size(), NEARBY_RADIUS_KM);

        initialized = true;
        log.info("RoutePlanningService cache initialized in {}ms. {} paragens, {} trajetos, {} viagens grouped into {} services",
                System.currentTimeMillis() - start, paragemCache.size(), trajetoPontosMap.size(),
                allViagens.size(), viagensByServiceAndTrajeto.size());
    }

    public List<RotaDTO> planearRota(Long origemId, Long destinoId) {
        return planearRota(origemId, destinoId, LocalTime.now(), LocalDate.now().getDayOfWeek());
    }

    public List<RotaDTO> planearRota(Long origemId, Long destinoId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        ensureInitialized();

        long start = System.currentTimeMillis();
        if (origemId.equals(destinoId)) return List.of();

        String serviceId = resolveServiceId(dayOfWeek);
        int queryMinutes = toMinutes(queryTime);

        Map<Long, List<Viagem>> viagensByTrajeto = viagensByServiceAndTrajeto.getOrDefault(serviceId, Map.of());

        log.info("Planning route from={} to={} time={} day={} serviceId={} viagens={}",
                origemId, destinoId, queryTime, dayOfWeek, serviceId, viagensByTrajeto.size());

        Set<Long> origemIds = expandByName(origemId);
        Set<Long> destinoIds = expandByName(destinoId);

        log.debug("Expanded: origemIds={} destinoIds={}", origemIds, destinoIds);

        List<RotaDTO> results = new ArrayList<>();
        Set<String> seenRouteKeys = new HashSet<>();

        Paragem origParagem = paragemCache.get(origemId);
        Paragem destParagem = paragemCache.get(destinoId);
        double walkDistKm = walkDistanceKm(origParagem, destParagem);
        if (walkDistKm > 0 && walkDistKm <= MAX_WALKING_DISTANCE_KM) {
            int walkMin = estimateWalkingMinutes(origParagem, destParagem);
            RotaDTO walkRoute = buildWalkingRoute(origParagem, destParagem, walkMin, queryMinutes);
            if (walkRoute != null) {
                results.add(walkRoute);
                seenRouteKeys.add("walk");
            }
        }

        for (Long oid : origemIds) {
            for (Long did : destinoIds) {
                if (oid.equals(did)) continue;

                Paragem oP = paragemCache.get(oid);
                Paragem dP = paragemCache.get(did);
                if (oP != null && dP != null && walkDistanceKm(oP, dP) <= MAX_WALKING_DISTANCE_KM
                        && oP.getNome().equals(dP.getNome())) {
                    continue;
                }

                long t0 = System.currentTimeMillis();
                List<RotaDTO> direct = findDirectRoutes(oid, did, queryMinutes, viagensByTrajeto);
                long directMs = System.currentTimeMillis() - t0;

                for (RotaDTO r : direct) {
                    if (seenRouteKeys.add(buildRouteKey(r))) results.add(r);
                }

                log.debug("Pair oid={} did={}: direct={} in {}ms, total={}", oid, did, direct.size(), directMs, results.size());

                if (results.size() < MAX_RESULTS) {
                    t0 = System.currentTimeMillis();
                    List<RotaDTO> transfers = findTransferRoutes(oid, did, queryMinutes, viagensByTrajeto);
                    long transferMs = System.currentTimeMillis() - t0;

                    for (RotaDTO r : transfers) {
                        if (seenRouteKeys.add(buildRouteKey(r))) results.add(r);
                    }

                    log.debug("Pair oid={} did={}: transfers={} in {}ms, total={}", oid, did, transfers.size(), transferMs, results.size());
                }

                if (results.size() >= MAX_RESULTS * 2) break;
            }
            if (results.size() >= MAX_RESULTS * 2) break;
        }

        results.sort(Comparator
                .comparingInt(RotaDTO::getTrocas)
                .thenComparingInt(RotaDTO::getTotalMinutos));

        List<RotaDTO> finalResults = results.stream().limit(MAX_RESULTS).collect(Collectors.toList());
        log.info("Route planning complete: {} results in {}ms", finalResults.size(), System.currentTimeMillis() - start);
        return finalResults;
    }

    private List<RotaDTO> findDirectRoutes(Long origemId, Long destinoId, int queryMinutes,
                                           Map<Long, List<Viagem>> viagensByTrajeto) {
        List<RotaDTO> directRoutes = new ArrayList<>();

        for (Map.Entry<Long, List<PontosDePassagem>> entry : trajetoPontosMap.entrySet()) {
            Long trajetoId = entry.getKey();
            List<PontosDePassagem> pontos = entry.getValue();

            int origemIdx = -1, destinoIdx = -1;
            for (int i = 0; i < pontos.size(); i++) {
                Long pid = pontos.get(i).getParagem().getId();
                if (pid.equals(origemId)) origemIdx = i;
                if (pid.equals(destinoId)) destinoIdx = i;
            }

            if (origemIdx < 0 || destinoIdx <= origemIdx) continue;

            int offsetOrigin = pontos.get(origemIdx).getTempoDesdeInicio();
            int offsetDest = pontos.get(destinoIdx).getTempoDesdeInicio();

            int minDepTOD = ((queryMinutes - offsetOrigin) % 1440 + 1440) % 1440;
            LocalTime minDepTime = LocalTime.of(minDepTOD / 60, minDepTOD % 60);

            Viagem nextDep = findNextDeparture(trajetoId, minDepTime, viagensByTrajeto);
            if (nextDep == null) continue;

            int depTOD = toMinutes(nextDep.getHoraPartida());
            int depAbsolute = depTOD;
            while (depAbsolute < queryMinutes - offsetOrigin) depAbsolute += 1440;

            int depAtOrigin = depAbsolute + offsetOrigin;
            int arrAtDest = depAbsolute + offsetDest;
            int wait = depAtOrigin - queryMinutes;

            if (wait > MAX_WAIT_MINUTES) continue;
            if (wait < 0) continue;

            Trajeto trajeto = trajetoCache.get(trajetoId);
            RotaDTO.SegmentoDTO seg = buildSegmentDTO(trajetoId, trajeto, pontos, origemIdx, destinoIdx);
            int duracao = offsetDest - offsetOrigin;
            seg.setDuracaoMinutos(Math.max(1, duracao));
            seg.setEsperaMinutos(wait);
            seg.setHoraPartida(minutesToTime(depAtOrigin));
            seg.setHoraChegada(minutesToTime(arrAtDest));

            RotaDTO rota = assembleRota(List.of(seg), true);
            rota.setTotalMinutos(arrAtDest - queryMinutes);
            directRoutes.add(rota);
        }

        directRoutes.sort(Comparator.comparingInt(RotaDTO::getTotalMinutos));
        return directRoutes.stream().limit(MAX_RESULTS).collect(Collectors.toList());
    }

    private List<RotaDTO> findTransferRoutes(Long origemId, Long destinoId, int queryMinutes,
                                             Map<Long, List<Viagem>> viagensByTrajeto) {

        Paragem destParagem = paragemCache.get(destinoId);
        Paragem origParagem = paragemCache.get(origemId);
        double maxDistanceKm = 15.0;
        if (destParagem != null && origParagem != null
                && destParagem.getLocalizacao() != null && origParagem.getLocalizacao() != null) {
            double origDestDist = haversineKm(
                    origParagem.getLocalizacao().getLatitude(), origParagem.getLocalizacao().getLongitude(),
                    destParagem.getLocalizacao().getLatitude(), destParagem.getLocalizacao().getLongitude());
            maxDistanceKm = Math.max(origDestDist * 2.5, 10.0);
        }

        Map<State, Integer> bestCost = new HashMap<>();
        Map<State, State> cameFrom = new HashMap<>();
        Map<State, Integer> stateTrajetoDep = new HashMap<>();
        Map<State, Integer> stateTransfersMap = new HashMap<>();

        PriorityQueue<Object[]> queue = new PriorityQueue<>(Comparator.comparingInt(a -> (int) a[2]));

        int initH = heuristicMinutes(origemId, destinoId);

        List<PontosDePassagem> originPontos = paragemToPontos.getOrDefault(origemId, List.of());
        for (PontosDePassagem p : originPontos) {
            Long tid = pontoToTrajeto.get(p.getId());
            if (tid == null) continue;

            Integer offset = trajetoOffsetMap.getOrDefault(tid, Map.of()).get(origemId);
            if (offset == null) continue;

            int minDepTOD = ((queryMinutes - offset) % 1440 + 1440) % 1440;
            LocalTime minDepTime = LocalTime.of(minDepTOD / 60, minDepTOD % 60);

            Viagem nextDep = findNextDeparture(tid, minDepTime, viagensByTrajeto);
            if (nextDep == null) continue;

            int depTOD = toMinutes(nextDep.getHoraPartida());
            int depAbsolute = depTOD;
            while (depAbsolute < queryMinutes - offset) depAbsolute += 1440;

            int costAtOrigin = depAbsolute + offset;
            int wait = costAtOrigin - queryMinutes;
            if (wait > MAX_WAIT_MINUTES || wait < 0) continue;

            State s = new State(origemId, tid);
            if (costAtOrigin < bestCost.getOrDefault(s, Integer.MAX_VALUE)) {
                int fScore = costAtOrigin + initH;
                bestCost.put(s, costAtOrigin);
                cameFrom.put(s, null);
                stateTrajetoDep.put(s, depAbsolute);
                stateTransfersMap.put(s, 0);
                queue.add(new Object[]{origemId, tid, fScore, costAtOrigin, 0});
            }
        }

        State destState = null;
        int explored = 0;

        while (!queue.isEmpty()) {
            Object[] current = queue.poll();
            long currentParagemId = (long) current[0];
            long currentTrajetoId = (long) current[1];
            int currentGScore = (int) current[3];
            int currentTransfers = (int) current[4];
            explored++;

            if (explored > MAX_STATES) {
                log.warn("Transfer search exceeded {} states, aborting, queue={}", MAX_STATES, queue.size());
                break;
            }

            State cs = new State(currentParagemId, currentTrajetoId);
            if (currentGScore > bestCost.getOrDefault(cs, Integer.MAX_VALUE)) continue;

            if (destState != null && currentGScore > bestCost.getOrDefault(destState, Integer.MAX_VALUE)) {
                break;
            }

            if (currentParagemId == destinoId) {
                if (destState == null || currentGScore < bestCost.getOrDefault(destState, Integer.MAX_VALUE)) {
                    bestCost.put(cs, currentGScore);
                    destState = cs;
                }
                continue;
            }

            List<PontosDePassagem> trajetoPontos = trajetoPontosMap.get(currentTrajetoId);
            if (trajetoPontos == null) continue;

            int currentIdx = -1;
            for (int i = 0; i < trajetoPontos.size(); i++) {
                if (trajetoPontos.get(i).getParagem().getId().equals(currentParagemId)) {
                    currentIdx = i;
                    break;
                }
            }
            if (currentIdx < 0) continue;

            int depAbsolute = stateTrajetoDep.getOrDefault(cs, currentGScore - trajetoOffsetMap.getOrDefault(currentTrajetoId, Map.of()).getOrDefault(currentParagemId, 0));

            for (int i = currentIdx + 1; i < trajetoPontos.size(); i++) {
                PontosDePassagem next = trajetoPontos.get(i);
                long nextParagemId = next.getParagem().getId();
                Paragem nextParagem = paragemCache.get(nextParagemId);
                if (nextParagem == null) continue;

                boolean isTransferStop = transferNamesSet.contains(nextParagem.getNome());
                boolean isDestination = nextParagemId == destinoId;

                if (!isTransferStop && !isDestination) continue;

                int nextOffset = next.getTempoDesdeInicio();
                int nextGScore = depAbsolute + nextOffset;

                if (destParagem != null && destParagem.getLocalizacao() != null) {
                    if (nextParagem.getLocalizacao() != null) {
                        double distToDest = haversineKm(
                                nextParagem.getLocalizacao().getLatitude(), nextParagem.getLocalizacao().getLongitude(),
                                destParagem.getLocalizacao().getLatitude(), destParagem.getLocalizacao().getLongitude());
                        if (distToDest > maxDistanceKm) continue;
                    }
                }

                int nextH = heuristicMinutes(nextParagemId, destinoId);

                State ns = new State(nextParagemId, currentTrajetoId);
                if (nextGScore < bestCost.getOrDefault(ns, Integer.MAX_VALUE)) {
                    bestCost.put(ns, nextGScore);
                    cameFrom.put(ns, cs);
                    stateTrajetoDep.put(ns, depAbsolute);
                    stateTransfersMap.put(ns, currentTransfers);
                    queue.add(new Object[]{nextParagemId, currentTrajetoId, nextGScore + nextH, nextGScore, currentTransfers});
                }

                if (isDestination) continue;
                if (nextGScore > currentGScore + MAX_WAIT_MINUTES) continue;
                if (currentTransfers >= MAX_TRANSFERS) continue;

                Set<Long> candidateIds = new HashSet<>();
                candidateIds.add(nextParagemId);
                candidateIds.addAll(nomeToIds.getOrDefault(nextParagem.getNome(), Set.of()));

                for (Long walkToId : candidateIds) {
                    for (PontosDePassagem transferPonto : paragemToPontos.getOrDefault(walkToId, List.of())) {
                        Long transferTrajetoId = pontoToTrajeto.get(transferPonto.getId());
                        if (transferTrajetoId == null || transferTrajetoId.equals(currentTrajetoId)) continue;

                        Integer transferOffset = trajetoOffsetMap.getOrDefault(transferTrajetoId, Map.of()).get(walkToId);
                        if (transferOffset == null) continue;

                        int walkBetweenPoles = walkToId.equals(nextParagemId) ? 0 : 1;
                        int arrAtTransfer = nextGScore + TRANSFER_BUFFER_MINUTES + walkBetweenPoles;

                        int minDepTOD2 = ((arrAtTransfer - transferOffset) % 1440 + 1440) % 1440;
                        LocalTime minDepTime2 = LocalTime.of(minDepTOD2 / 60, minDepTOD2 % 60);

                        Viagem nextDep2 = findNextDeparture(transferTrajetoId, minDepTime2, viagensByTrajeto);
                        if (nextDep2 == null) continue;

                        int depTOD2 = toMinutes(nextDep2.getHoraPartida());
                        int depAbsolute2 = depTOD2;
                        while (depAbsolute2 < arrAtTransfer - transferOffset) depAbsolute2 += 1440;

                        int costAfterTransfer = depAbsolute2 + transferOffset;
                        int waitAtTransfer = costAfterTransfer - arrAtTransfer;
                        if (waitAtTransfer > MAX_WAIT_MINUTES || waitAtTransfer < 0) continue;

                        int transferH = heuristicMinutes(walkToId, destinoId);
                        int newTransfers = currentTransfers + 1;
                        State ts = new State(walkToId, transferTrajetoId);

                        if (costAfterTransfer < bestCost.getOrDefault(ts, Integer.MAX_VALUE)) {
                            bestCost.put(ts, costAfterTransfer);
                            cameFrom.put(ts, ns);
                            stateTrajetoDep.put(ts, depAbsolute2);
                            stateTransfersMap.put(ts, newTransfers);
                            queue.add(new Object[]{walkToId, transferTrajetoId, costAfterTransfer + transferH, costAfterTransfer, newTransfers});
                        }
                    }
                }

                List<WalkEdge> nearby = nearbyStopsMap.getOrDefault(nextParagemId, List.of());
                for (WalkEdge we : nearby) {
                    if (nomeToIds.getOrDefault(nextParagem.getNome(), Set.of()).contains(we.toId())) continue;

                    int arrAtWalk = nextGScore + we.minutes();

                    Paragem walkTarget = paragemCache.get(we.toId());
                    if (walkTarget != null && destParagem != null && destParagem.getLocalizacao() != null && walkTarget.getLocalizacao() != null) {
                        double distWalkToDest = haversineKm(
                                walkTarget.getLocalizacao().getLatitude(), walkTarget.getLocalizacao().getLongitude(),
                                destParagem.getLocalizacao().getLatitude(), destParagem.getLocalizacao().getLongitude());
                        if (distWalkToDest > maxDistanceKm) continue;
                    }

                    for (PontosDePassagem walkPonto : paragemToPontos.getOrDefault(we.toId(), List.of())) {
                        Long walkTrajetoId = pontoToTrajeto.get(walkPonto.getId());
                        if (walkTrajetoId == null || walkTrajetoId.equals(currentTrajetoId)) continue;

                        Integer walkOffset = trajetoOffsetMap.getOrDefault(walkTrajetoId, Map.of()).get(we.toId());
                        if (walkOffset == null) continue;

                        int walkArrWithBuffer = arrAtWalk + TRANSFER_BUFFER_MINUTES;
                        int minDepTOD3 = ((walkArrWithBuffer - walkOffset) % 1440 + 1440) % 1440;
                        LocalTime minDepTime3 = LocalTime.of(minDepTOD3 / 60, minDepTOD3 % 60);

                        Viagem nextDep3 = findNextDeparture(walkTrajetoId, minDepTime3, viagensByTrajeto);
                        if (nextDep3 == null) continue;

                        int depTOD3 = toMinutes(nextDep3.getHoraPartida());
                        int depAbsolute3 = depTOD3;
                        while (depAbsolute3 < walkArrWithBuffer - walkOffset) depAbsolute3 += 1440;

                        int costAfterWalk = depAbsolute3 + walkOffset;
                        int waitAfterWalk = costAfterWalk - walkArrWithBuffer;
                        if (waitAfterWalk > MAX_WAIT_MINUTES || waitAfterWalk < 0) continue;

                        int walkH = heuristicMinutes(we.toId(), destinoId);
                        int walkTransfers = currentTransfers + 1;
                        State ws = new State(we.toId(), walkTrajetoId);

                        if (costAfterWalk < bestCost.getOrDefault(ws, Integer.MAX_VALUE)) {
                            bestCost.put(ws, costAfterWalk);
                            cameFrom.put(ws, ns);
                            stateTrajetoDep.put(ws, depAbsolute3);
                            stateTransfersMap.put(ws, walkTransfers);
                            queue.add(new Object[]{we.toId(), walkTrajetoId, costAfterWalk + walkH, costAfterWalk, walkTransfers});
                        }
                    }
                }
            }
        }

        log.debug("Transfer search explored {} states, destState={}", explored, destState != null);

        if (destState == null) return List.of();

        List<State> path = new ArrayList<>();
        State cur = destState;
        while (cur != null) {
            path.add(0, cur);
            cur = cameFrom.get(cur);
        }

        List<RotaDTO.SegmentoDTO> segmentos = new ArrayList<>();
        int segStart = 0;
        long currentTrajetoId = path.get(0).trajetoId();

        for (int i = 1; i < path.size(); i++) {
            long nextTrajetoId = path.get(i).trajetoId();
            boolean isLast = (i == path.size() - 1);
            boolean trajetoChanged = nextTrajetoId != currentTrajetoId;

            if (trajetoChanged || isLast) {
                int endIdx = isLast && !trajetoChanged ? i : i - 1;

                RotaDTO.SegmentoDTO seg = buildSegmentFromPath(currentTrajetoId, path, segStart, endIdx, stateTrajetoDep, queryMinutes);

                if (trajetoChanged && endIdx + 1 < path.size()) {
                    long transferFromId = path.get(endIdx).paragemId();
                    long transferToId = path.get(endIdx + 1).paragemId();
                    int walkMin = estimateWalkingMinutes(paragemCache.get(transferFromId), paragemCache.get(transferToId));
                    seg.setTempoCaminhadaMinutos(walkMin);

                    Paragem fromP = paragemCache.get(transferFromId);
                    Paragem toP = paragemCache.get(transferToId);
                    if (fromP != null && toP != null && !fromP.getNome().equals(toP.getNome()) && walkMin > 0) {
                        RotaDTO.SegmentoDTO walkSeg = new RotaDTO.SegmentoDTO();
                        walkSeg.setLinhaNome("A pe");
                        walkSeg.setOrigem(toParagemDTO(fromP));
                        walkSeg.setDestino(toParagemDTO(toP));
                        walkSeg.setParagens(List.of(toParagemDTO(fromP), toParagemDTO(toP)));
                        walkSeg.setDuracaoMinutos(walkMin);
                        walkSeg.setTempoCaminhadaMinutos(walkMin);
                        walkSeg.setHoraPartida(seg.getHoraChegada());
                        walkSeg.setHoraChegada(minutesToTime(toMinutes(LocalTime.parse(seg.getHoraChegada())) + walkMin));
                        segmentos.add(seg);
                        segmentos.add(walkSeg);
                        segStart = i;
                        currentTrajetoId = nextTrajetoId;
                        continue;
                    }
                }

                segmentos.add(seg);
                segStart = i;
                currentTrajetoId = nextTrajetoId;
            }
        }

        RotaDTO rota = assembleRota(segmentos, false);
        rota.setTotalMinutos(bestCost.getOrDefault(destState, 0) - queryMinutes);
        return List.of(rota);
    }

    public List<ProximoPasseDTO> findProximosPasses(Long trajetoId, Long paragemId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        ensureInitialized();

        String serviceId = resolveServiceId(dayOfWeek);
        int queryMinutes = toMinutes(queryTime);

        Map<Long, List<Viagem>> viagensByTrajeto = viagensByServiceAndTrajeto.getOrDefault(serviceId, Map.of());
        List<Viagem> viagens = viagensByTrajeto.getOrDefault(trajetoId, List.of());

        Map<Long, Integer> offsets = trajetoOffsetMap.getOrDefault(trajetoId, Map.of());
        Integer offset = offsets.get(paragemId);
        if (offset == null) return List.of();

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

    public List<HorarioDTO> findHorarios(Long linhaId, String serviceId) {
        ensureInitialized();

        List<Trajeto> trajetos = trajetoCache.values().stream()
                .filter(t -> t.getLinha() != null && t.getLinha().getId().equals(linhaId))
                .collect(Collectors.toList());

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

        List<Trajeto> trajetos = trajetoCache.values().stream()
                .filter(t -> t.getLinha() != null && t.getLinha().getId().equals(linhaId))
                .collect(Collectors.toList());

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

    private Viagem findNextDeparture(Long trajetoId, LocalTime afterTime, Map<Long, List<Viagem>> viagensByTrajeto) {
        List<Viagem> list = viagensByTrajeto.get(trajetoId);
        if (list == null || list.isEmpty()) return null;

        int lo = 0, hi = list.size();
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (list.get(mid).getHoraPartida().isBefore(afterTime)) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        if (lo < list.size()) return list.get(lo);
        return list.get(0);
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

    private RotaDTO.SegmentoDTO buildSegmentFromPath(Long trajetoId, List<State> path, int segStart, int endIdx,
                                                      Map<State, Integer> stateTrajetoDep,
                                                      int queryMinutes) {
        RotaDTO.SegmentoDTO seg = new RotaDTO.SegmentoDTO();
        seg.setTrajetoId(trajetoId);
        Trajeto trajeto = trajetoCache.get(trajetoId);
        if (trajeto != null) {
            seg.setLinhaNome(trajeto.getLinha() != null ? trajeto.getLinha().getNome() : "Linha");
            seg.setDirecao(trajeto.getDirecao() != null ? trajeto.getDirecao().name() : "");
        }
        seg.setDestinoFinal(destinoFinalMap.getOrDefault(trajetoId, ""));

        long segOriginId = path.get(segStart).paragemId();
        long segDestId = path.get(endIdx).paragemId();

        Paragem origemP = paragemCache.get(segOriginId);
        Paragem destinoP = paragemCache.get(segDestId);
        if (origemP != null) seg.setOrigem(toParagemDTO(origemP));
        if (destinoP != null) seg.setDestino(toParagemDTO(destinoP));

        List<PontosDePassagem> trajetoPontos = trajetoPontosMap.get(trajetoId);
        List<RotaDTO.ParagemDTO> paragens = new ArrayList<>();
        if (trajetoPontos != null) {
            int pStart = -1, pEnd = -1;
            for (int i = 0; i < trajetoPontos.size(); i++) {
                long pid = trajetoPontos.get(i).getParagem().getId();
                if (pid == segOriginId && pStart < 0) pStart = i;
                if (pid == segDestId) pEnd = i;
            }
            if (pStart >= 0 && pEnd >= 0 && pEnd >= pStart) {
                for (int i = pStart; i <= pEnd; i++) {
                    Paragem p = paragemCache.get(trajetoPontos.get(i).getParagem().getId());
                    if (p != null) paragens.add(toParagemDTO(p));
                }
            }
        }
        if (paragens.isEmpty()) {
            for (int j = segStart; j <= endIdx; j++) {
                Paragem p = paragemCache.get(path.get(j).paragemId());
                if (p != null) paragens.add(toParagemDTO(p));
            }
        }
        seg.setParagens(paragens);

        Map<Long, Integer> offsets = trajetoOffsetMap.get(trajetoId);
        int startTime = offsets != null ? offsets.getOrDefault(segOriginId, 0) : 0;
        int endTime = offsets != null ? offsets.getOrDefault(segDestId, 0) : 0;
        seg.setDuracaoMinutos(Math.max(1, endTime - startTime));

        State originState = path.get(segStart);
        Integer depAbs = stateTrajetoDep.get(originState);
        if (depAbs != null) {
            int depAtOrigin = depAbs + startTime;
            int arrAtDest = depAbs + endTime;
            int wait = depAtOrigin - queryMinutes;
            if (segStart > 0) {
                State prevEndState = path.get(segStart - 1);
                Integer prevDepAbs = stateTrajetoDep.get(prevEndState);
                if (prevDepAbs != null) {
                    Map<Long, Integer> prevOffsets = trajetoOffsetMap.get(prevEndState.trajetoId());
                    int prevOffset = prevOffsets != null ? prevOffsets.getOrDefault(prevEndState.paragemId(), 0) : 0;
                    int prevCost = prevDepAbs + prevOffset;
                    wait = depAtOrigin - prevCost - (seg.getTempoCaminhadaMinutos() > 0 ? seg.getTempoCaminhadaMinutos() : 0);
                }
            }
            seg.setEsperaMinutos(Math.max(0, wait));
            seg.setHoraPartida(minutesToTime(depAtOrigin));
            seg.setHoraChegada(minutesToTime(arrAtDest));
        }

        return seg;
    }

    private RotaDTO assembleRota(List<RotaDTO.SegmentoDTO> segmentos, boolean direta) {
        int totalBus = segmentos.stream().mapToInt(RotaDTO.SegmentoDTO::getDuracaoMinutos).sum();
        int totalWalk = segmentos.stream().mapToInt(RotaDTO.SegmentoDTO::getTempoCaminhadaMinutos).sum();
        int totalWait = segmentos.stream().mapToInt(RotaDTO.SegmentoDTO::getEsperaMinutos).sum();
        int trocas = Math.max(0, segmentos.size() - 1);

        RotaDTO rota = new RotaDTO();
        rota.setTotalMinutos(totalBus + totalWalk + totalWait);
        rota.setTrocas(trocas);
        rota.setTotalCaminhadaMinutos(totalWalk);
        rota.setDireta(direta);
        rota.setSegmentos(segmentos);

        if (!segmentos.isEmpty()) {
            rota.setHoraPartida(segmentos.get(0).getHoraPartida());
            rota.setHoraChegada(segmentos.get(segmentos.size() - 1).getHoraChegada());
        }

        return rota;
    }

    private Set<Long> expandByName(Long paragemId) {
        Set<Long> ids = new HashSet<>();
        ids.add(paragemId);
        Paragem p = paragemCache.get(paragemId);
        if (p != null && nomeToIds.containsKey(p.getNome())) {
            ids.addAll(nomeToIds.get(p.getNome()));
        }
        return ids;
    }

    private String resolveServiceId(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SATURDAY -> "SAB";
            case SUNDAY -> "DOM";
            default -> "UTEIS";
        };
    }

    private int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private String minutesToTime(int totalMinutes) {
        int h = (totalMinutes / 60) % 24;
        int m = totalMinutes % 60;
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

    private int heuristicMinutes(long paragemId, long destinoId) {
        Paragem from = paragemCache.get(paragemId);
        Paragem to = paragemCache.get(destinoId);
        if (from == null || to == null || from.getLocalizacao() == null || to.getLocalizacao() == null) return 0;
        double km = haversineKm(
                from.getLocalizacao().getLatitude(), from.getLocalizacao().getLongitude(),
                to.getLocalizacao().getLatitude(), to.getLocalizacao().getLongitude());
        return (int) Math.round(km / MAX_BUS_SPEED_KMH * 60);
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

    private String buildRouteKey(RotaDTO rota) {
        if (rota.getSegmentos() == null) return "empty";
        return rota.getSegmentos().stream()
                .map(s -> s.getLinhaNome() + ":" + s.getDirecao())
                .collect(Collectors.joining("|"));
    }

    private RotaDTO.ParagemDTO toParagemDTO(Paragem p) {
        if (p == null) return null;
        Double lat = p.getLocalizacao() != null ? p.getLocalizacao().getLatitude() : null;
        Double lon = p.getLocalizacao() != null ? p.getLocalizacao().getLongitude() : null;
        return new RotaDTO.ParagemDTO(p.getId(), p.getNome(), lat, lon);
    }

    private double walkDistanceKm(Paragem from, Paragem to) {
        if (from == null || to == null) return Double.MAX_VALUE;
        if (from.getLocalizacao() == null || to.getLocalizacao() == null) return Double.MAX_VALUE;
        if (from.getId().equals(to.getId())) return 0;
        return haversineKm(
                from.getLocalizacao().getLatitude(), from.getLocalizacao().getLongitude(),
                to.getLocalizacao().getLatitude(), to.getLocalizacao().getLongitude());
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

        RotaDTO rota = new RotaDTO();
        rota.setTotalMinutos(walkMin);
        rota.setTrocas(0);
        rota.setTotalCaminhadaMinutos(walkMin);
        rota.setDireta(true);
        rota.setCaminho(true);
        rota.setSegmentos(List.of(seg));
        rota.setHoraPartida(minutesToTime(queryMinutes));
        rota.setHoraChegada(minutesToTime(queryMinutes + walkMin));
        return rota;
    }
}