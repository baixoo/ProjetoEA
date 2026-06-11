package pt.notub.network.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pt.notub.network.dto.RotaDTO;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.service.RoutingIndexService.ServiceRoutingIndex;
import pt.notub.network.service.RoutingIndexService.CachedTrip;
import pt.notub.network.service.RoutingIndexService.WalkEdge;
import pt.notub.network.service.RouteDtoAssembler.RouteLeg;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteSearchEngine {

    private static final Logger log = LoggerFactory.getLogger(RouteSearchEngine.class);
    private static final int TRANSFER_BUFFER_MINUTES = 2;
    private static final int MAX_WAIT_MINUTES = 65;
    private static final int MAX_RESULTS = 3;

    private final WalkingTransferPolicy walkingPolicy;
    private final RouteDtoAssembler assembler;

    public RouteSearchEngine(WalkingTransferPolicy walkingPolicy, RouteDtoAssembler assembler) {
        this.walkingPolicy = walkingPolicy;
        this.assembler = assembler;
    }

    private enum LegType {
        SOURCE,
        WALK,
        RIDE
    }

    private static class Label {
        private final Long stopId;
        private final int arrivalMinutes;
        private final int rideCount;
        private final Set<Integer> zoneNums;
        private final Label previous;
        private final LegType legType;
        private final Long trajetoId; // null for WALK
        private final int departureMinutes;
        private final int boardIndex;
        private final int alightIndex;
        private final int walkMinutes;

        private Label(Long stopId, int arrivalMinutes, int rideCount, Set<Integer> zoneNums, Label previous,
                      LegType legType, Long trajetoId, int departureMinutes, int boardIndex, int alightIndex, int walkMinutes) {
            this.stopId = stopId;
            this.arrivalMinutes = arrivalMinutes;
            this.rideCount = rideCount;
            this.zoneNums = zoneNums;
            this.previous = previous;
            this.legType = legType;
            this.trajetoId = trajetoId;
            this.departureMinutes = departureMinutes;
            this.boardIndex = boardIndex;
            this.alightIndex = alightIndex;
            this.walkMinutes = walkMinutes;
        }
    }

    public List<RotaDTO> search(Long origemId, Long destinoId, int queryMinutes, ServiceRoutingIndex index) {
        if (origemId.equals(destinoId)) {
            return List.of();
        }

        Set<Long> origemIds = expandByName(origemId, index);
        Set<Long> destinoIds = expandByName(destinoId, index);

        List<RotaDTO> results = new ArrayList<>();

        // 1. Run RAPTOR Search
        List<Label> destLabels = findRaptorLabels(origemIds, destinoIds, queryMinutes, index);
        for (Label label : destLabels) {
            List<RouteLeg> legs = reconstructPath(label);
            RotaDTO rota = assembler.assembleRoute(legs, index, queryMinutes);
            if (rota != null) {
                results.add(rota);
            }
        }

        // 2. Direct Walk Candidate
        Paragem from = index.getParagemCache().get(origemId);
        Paragem to = index.getParagemCache().get(destinoId);
        if (from != null && to != null) {
            double walkDist = walkingPolicy.getDistanceKm(from, to);
            if (walkingPolicy.isDirectWalkAllowed(walkDist)) {
                int walkMin = walkingPolicy.estimateWalkMinutes(from, to);
                if (walkingPolicy.isDirectWalkWorthShowing(walkMin, results)) {
                    RotaDTO walkRoute = assembler.buildWalkingOnlyRoute(from, to, walkMin, queryMinutes, index);
                    if (walkRoute != null) {
                        results.add(walkRoute);
                    }
                }
            }
        }

        // 3. Deduplicate and filter Pareto-dominated results
        results = dedupeRoutes(results);
        results = filterDominated(results);
        results.sort(routeComparator());

        return results.stream().limit(MAX_RESULTS).collect(Collectors.toList());
    }

    private List<Label> findRaptorLabels(Set<Long> origemIds, Set<Long> destinoIds, int queryMinutes, ServiceRoutingIndex index) {
        Map<Long, List<Label>> bestLabelsByStop = new HashMap<>();
        Map<Long, List<Label>> previousRound = new HashMap<>();
        Set<Long> markedStops = new HashSet<>();

        for (Long origemId : origemIds) {
            if (!index.getParagemCache().containsKey(origemId)) continue;
            Set<Integer> originZones = new TreeSet<>();
            addStopZone(originZones, origemId, index);
            Label source = new Label(origemId, queryMinutes, 0, Collections.unmodifiableSet(originZones),
                    null, LegType.SOURCE, null, -1, -1, -1, 0);
            if (addParetoLabel(previousRound, bestLabelsByStop, source)) {
                markedStops.add(origemId);
            }
        }

        if (markedStops.isEmpty()) {
            return List.of();
        }

        markedStops.addAll(relaxFootpaths(markedStops, previousRound, bestLabelsByStop, index));

        int maxRounds = 4; // RAPTOR typically runs up to 3 or 4 transfers
        for (int round = 1; round <= maxRounds; round++) {
            if (markedStops.isEmpty()) break;

            Map<Long, Integer> routesToScan = buildRoutesToScan(markedStops, index);
            if (routesToScan.isEmpty()) break;

            Map<Long, List<Label>> currentRound = copyLabelMap(previousRound);
            Set<Long> transitMarked = new HashSet<>();

            for (Map.Entry<Long, Integer> entry : routesToScan.entrySet()) {
                Long trajetoId = entry.getKey();
                List<CachedTrip> trips = index.getTrajetoTripsMap().getOrDefault(trajetoId, List.of());
                scanRoute(trajetoId, entry.getValue(), trips, previousRound, currentRound, bestLabelsByStop, transitMarked, index);
            }

            if (transitMarked.isEmpty()) break;

            Set<Long> nextMarked = new HashSet<>(transitMarked);
            nextMarked.addAll(relaxFootpaths(transitMarked, currentRound, bestLabelsByStop, index));

            previousRound = currentRound;
            markedStops = nextMarked;
        }

        return collectDestinationLabels(destinoIds, bestLabelsByStop);
    }

    private Map<Long, Integer> buildRoutesToScan(Set<Long> markedStops, ServiceRoutingIndex index) {
        Map<Long, Integer> routesToScan = new HashMap<>();
        for (Long stopId : markedStops) {
            for (Long trajetoId : index.getParagemToTrajetoIds().getOrDefault(stopId, Set.of())) {
                List<PontosDePassagem> pontos = index.getTrajetoPontosMap().get(trajetoId);
                if (pontos != null) {
                    for (int i = 0; i < pontos.size(); i++) {
                        if (pontos.get(i).getParagem() != null && pontos.get(i).getParagem().getId().equals(stopId)) {
                            routesToScan.merge(trajetoId, i, Math::min);
                            break;
                        }
                    }
                }
            }
        }
        return routesToScan;
    }

    private void scanRoute(Long trajetoId,
                           int startIndex,
                           List<CachedTrip> trips,
                           Map<Long, List<Label>> previousRound,
                           Map<Long, List<Label>> currentRound,
                           Map<Long, List<Label>> bestLabelsByStop,
                           Set<Long> markedStops,
                           ServiceRoutingIndex index) {
        List<PontosDePassagem> pontos = index.getTrajetoPontosMap().get(trajetoId);
        if (pontos == null || pontos.isEmpty() || trips == null || trips.isEmpty() || startIndex >= pontos.size()) return;

        CachedTrip activeTrip = null;
        Label boardLabel = null;
        int boardIndex = -1;

        for (int i = startIndex; i < pontos.size(); i++) {
            PontosDePassagem ponto = pontos.get(i);
            if (ponto.getParagem() == null) continue;
            Long stopId = ponto.getParagem().getId();

            // 1. Alight if on active trip
            if (activeTrip != null) {
                int arrivalTime = activeTrip.getStopTimesMinutes()[i];
                if (arrivalTime != Integer.MAX_VALUE) {
                    Label rideLabel = new Label(
                            stopId,
                            arrivalTime,
                            boardLabel.rideCount + 1,
                            zonesWithRouteSegment(boardLabel.zoneNums, pontos, boardIndex, i, index),
                            boardLabel,
                            LegType.RIDE,
                            trajetoId,
                            activeTrip.getStopTimesMinutes()[boardIndex],
                            boardIndex,
                            i,
                            0
                    );
                    if (addParetoLabel(currentRound, bestLabelsByStop, rideLabel)) {
                        markedStops.add(stopId);
                    }
                }
            }

            // 2. Check for boarding or switching trips
            List<Label> boardLabels = previousRound.getOrDefault(stopId, List.of());
            for (Label label : boardLabels) {
                int boardReady = label.arrivalMinutes + (label.rideCount > 0 ? TRANSFER_BUFFER_MINUTES : 0);

                for (CachedTrip trip : trips) {
                    int depTime = trip.getStopTimesMinutes()[i];
                    if (depTime != Integer.MAX_VALUE && depTime >= boardReady && depTime - boardReady <= MAX_WAIT_MINUTES) {
                        int activeArrival = (activeTrip == null) ? Integer.MAX_VALUE : activeTrip.getStopTimesMinutes()[i];
                        if (activeTrip == null || depTime < activeArrival) {
                            activeTrip = trip;
                            boardLabel = label;
                            boardIndex = i;
                        }
                        break; // Sorted by departure, so first match is earliest
                    }
                }
            }
        }
    }

    private Set<Long> relaxFootpaths(Set<Long> sourceStops,
                                     Map<Long, List<Label>> roundLabels,
                                     Map<Long, List<Label>> bestLabelsByStop,
                                     ServiceRoutingIndex index) {
        Set<Long> markedStops = new HashSet<>();

        for (Long fromStopId : sourceStops) {
            List<Label> labels = new ArrayList<>(roundLabels.getOrDefault(fromStopId, List.of()));
            for (Label fromLabel : labels) {
                List<WalkEdge> walkEdges = getWalkEdges(fromStopId, index);
                for (WalkEdge edge : walkEdges) {
                    int arrival = fromLabel.arrivalMinutes + edge.getMinutes();
                    Set<Integer> walkZones = new TreeSet<>(fromLabel.zoneNums);
                    addStopZone(walkZones, edge.getToId(), index);

                    Label walkLabel = new Label(
                            edge.getToId(),
                            arrival,
                            fromLabel.rideCount,
                            Collections.unmodifiableSet(walkZones),
                            fromLabel,
                            LegType.WALK,
                            null,
                            fromLabel.arrivalMinutes,
                            -1,
                            -1,
                            edge.getMinutes()
                    );
                    if (addParetoLabel(roundLabels, bestLabelsByStop, walkLabel)) {
                        markedStops.add(edge.getToId());
                    }
                }
            }
        }

        return markedStops;
    }

    private List<WalkEdge> getWalkEdges(Long stopId, ServiceRoutingIndex index) {
        Map<Long, Integer> bestWalk = new HashMap<>();

        // Nearby physical walk edges
        for (WalkEdge edge : index.getNearbyStopsMap().getOrDefault(stopId, List.of())) {
            double dist = walkingPolicy.getDistanceKm(
                    index.getParagemCache().get(stopId),
                    index.getParagemCache().get(edge.getToId())
            );
            if (walkingPolicy.isTransferWalkAllowed(dist)) {
                bestWalk.merge(edge.getToId(), edge.getMinutes(), Math::min);
            }
        }

        // Same stop name group (normalized interchanges)
        Paragem stop = index.getParagemCache().get(stopId);
        if (stop != null) {
            String groupKey = RoutingIndexService.normalizeStopGroupKey(stop.getNome());
            for (Long sameNameId : index.getNomeToIds().getOrDefault(groupKey, Set.of())) {
                if (!sameNameId.equals(stopId)) {
                    Paragem sameStop = index.getParagemCache().get(sameNameId);
                    double dist = walkingPolicy.getDistanceKm(stop, sameStop);
                    if (walkingPolicy.isTransferWalkAllowed(dist)) {
                        int walkMin = walkingPolicy.estimateWalkMinutes(stop, sameStop);
                        bestWalk.merge(sameNameId, walkMin, Math::min);
                    }
                }
            }
        }

        return bestWalk.entrySet().stream()
                .map(entry -> new WalkEdge(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(WalkEdge::getMinutes))
                .collect(Collectors.toList());
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

    private Map<Long, List<Label>> copyLabelMap(Map<Long, List<Label>> source) {
        Map<Long, List<Label>> copy = new HashMap<>();
        for (Map.Entry<Long, List<Label>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
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
        return pareto;
    }

    private List<RouteLeg> reconstructPath(Label label) {
        List<RouteLeg> legs = new ArrayList<>();
        Label current = label;
        while (current != null && current.previous != null) {
            RouteDtoAssembler.LegType type = (current.legType == LegType.RIDE) ? RouteDtoAssembler.LegType.RIDE : RouteDtoAssembler.LegType.WALK;
            legs.add(0, new RouteLeg(
                    type,
                    current.previous.stopId,
                    current.stopId,
                    current.trajetoId,
                    current.departureMinutes,
                    current.arrivalMinutes,
                    current.boardIndex,
                    current.alightIndex,
                    current.walkMinutes
            ));
            current = current.previous;
        }
        return legs;
    }

    private Set<Long> expandByName(Long paragemId, ServiceRoutingIndex index) {
        Set<Long> ids = new HashSet<>();
        ids.add(paragemId);
        Paragem p = index.getParagemCache().get(paragemId);
        if (p != null) {
            String groupKey = RoutingIndexService.normalizeStopGroupKey(p.getNome());
            if (index.getNomeToIds().containsKey(groupKey)) {
                ids.addAll(index.getNomeToIds().get(groupKey));
            }
        }
        return ids;
    }

    private Set<Integer> zonesWithRouteSegment(Set<Integer> baseZones, List<PontosDePassagem> pontos, int startIdx, int endIdx, ServiceRoutingIndex index) {
        Set<Integer> zones = new TreeSet<>(baseZones);
        for (int i = Math.max(0, startIdx); i <= endIdx && i < pontos.size(); i++) {
            if (pontos.get(i).getParagem() != null) {
                addStopZone(zones, pontos.get(i).getParagem().getId(), index);
            }
        }
        return Collections.unmodifiableSet(zones);
    }

    private void addStopZone(Set<Integer> zones, Long stopId, ServiceRoutingIndex index) {
        Paragem stop = index.getParagemCache().get(stopId);
        if (stop != null && stop.getZona() != null) {
            zones.add(stop.getZona().getNum());
        }
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

    private boolean dominates(RotaDTO a, RotaDTO b) {
        return a.getTotalMinutos() <= b.getTotalMinutos()
                && a.getTotalCaminhadaMinutos() <= b.getTotalCaminhadaMinutos()
                && a.getTrocas() <= b.getTrocas()
                && a.getNrZonas() <= b.getNrZonas()
                && (a.getTotalMinutos() < b.getTotalMinutos()
                || a.getTotalCaminhadaMinutos() < b.getTotalCaminhadaMinutos()
                || a.getTrocas() < b.getTrocas()
                || a.getNrZonas() < b.getNrZonas());
    }

    private List<RotaDTO> filterDominated(List<RotaDTO> routes) {
        List<RotaDTO> nonDominated = new ArrayList<>();
        for (RotaDTO r : routes) {
            boolean dominatedByOther = false;
            for (RotaDTO other : routes) {
                if (other != r && dominates(other, r)) {
                    dominatedByOther = true;
                    break;
                }
            }
            if (!dominatedByOther) {
                nonDominated.add(r);
            }
        }
        return nonDominated;
    }

    private Comparator<RotaDTO> routeComparator() {
        return Comparator
                .comparingInt(RotaDTO::getTotalMinutos)
                .thenComparingInt(RotaDTO::getTotalCaminhadaMinutos)
                .thenComparingInt(RotaDTO::getTrocas)
                .thenComparingInt(RotaDTO::getNrZonas);
    }
}
