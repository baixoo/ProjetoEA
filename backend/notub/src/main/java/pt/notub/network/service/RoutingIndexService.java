package pt.notub.network.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pt.notub.network.entity.Horario;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.repository.HorarioRepository;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.PontosDePassagemRepository;
import pt.notub.network.repository.TrajetoRepository;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RoutingIndexService {

    private static final Logger log = LoggerFactory.getLogger(RoutingIndexService.class);
    private static final double WALKING_SPEED_KMH = 4.0;
    private static final double MAX_INDEX_WALK_DIST_KM = 1.0;
    private static final int MAX_NEARBY = 5;
    private static final Pattern GENERATED_SUFFIX_PATTERN = Pattern.compile("\\s+(?:I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII)$");

    private final HorarioRepository horarioRepo;
    private final ParagemRepository paragemRepo;
    private final TrajetoRepository trajetoRepo;
    private final PontosDePassagemRepository pontosRepo;

    public RoutingIndexService(HorarioRepository horarioRepo,
                               ParagemRepository paragemRepo,
                               TrajetoRepository trajetoRepo,
                               PontosDePassagemRepository pontosRepo) {
        this.horarioRepo = horarioRepo;
        this.paragemRepo = paragemRepo;
        this.trajetoRepo = trajetoRepo;
        this.pontosRepo = pontosRepo;
    }

    public static class WalkEdge {
        private final long toId;
        private final int minutes;

        public WalkEdge(long toId, int minutes) {
            this.toId = toId;
            this.minutes = minutes;
        }

        public long getToId() { return toId; }
        public int getMinutes() { return minutes; }
    }

    public static class CachedTrip {
        private final String gtfsTripId;
        private final int[] stopTimesMinutes; // index aligns with trajeto's PontosDePassagem list

        public CachedTrip(String gtfsTripId, int[] stopTimesMinutes) {
            this.gtfsTripId = gtfsTripId;
            this.stopTimesMinutes = stopTimesMinutes;
        }

        public String getGtfsTripId() { return gtfsTripId; }
        public int[] getStopTimesMinutes() { return stopTimesMinutes; }
    }

    public static class ServiceRoutingIndex {
        private final Map<Long, Paragem> paragemCache;
        private final Map<String, Set<Long>> nomeToIds;
        private final Map<Long, List<PontosDePassagem>> trajetoPontosMap;
        private final Map<Long, List<CachedTrip>> trajetoTripsMap;
        private final Map<Long, Set<Long>> paragemToTrajetoIds;
        private final Map<Long, String> destinoFinalMap;
        private final Map<Long, List<WalkEdge>> nearbyStopsMap;
        private final Map<Integer, String> zoneNameByNum;

        public ServiceRoutingIndex(Map<Long, Paragem> paragemCache,
                                   Map<String, Set<Long>> nomeToIds,
                                   Map<Long, List<PontosDePassagem>> trajetoPontosMap,
                                   Map<Long, List<CachedTrip>> trajetoTripsMap,
                                   Map<Long, Set<Long>> paragemToTrajetoIds,
                                   Map<Long, String> destinoFinalMap,
                                   Map<Long, List<WalkEdge>> nearbyStopsMap,
                                   Map<Integer, String> zoneNameByNum) {
            this.paragemCache = paragemCache;
            this.nomeToIds = nomeToIds;
            this.trajetoPontosMap = trajetoPontosMap;
            this.trajetoTripsMap = trajetoTripsMap;
            this.paragemToTrajetoIds = paragemToTrajetoIds;
            this.destinoFinalMap = destinoFinalMap;
            this.nearbyStopsMap = nearbyStopsMap;
            this.zoneNameByNum = zoneNameByNum;
        }

        public Map<Long, Paragem> getParagemCache() { return paragemCache; }
        public Map<String, Set<Long>> getNomeToIds() { return nomeToIds; }
        public Map<Long, List<PontosDePassagem>> getTrajetoPontosMap() { return trajetoPontosMap; }
        public Map<Long, List<CachedTrip>> getTrajetoTripsMap() { return trajetoTripsMap; }
        public Map<Long, Set<Long>> getParagemToTrajetoIds() { return paragemToTrajetoIds; }
        public Map<Long, String> getDestinoFinalMap() { return destinoFinalMap; }
        public Map<Long, List<WalkEdge>> getNearbyStopsMap() { return nearbyStopsMap; }
        public Map<Integer, String> getZoneNameByNum() { return zoneNameByNum; }
    }

    @Cacheable(value = "routingIndex", key = "#serviceName.toUpperCase()")
    public ServiceRoutingIndex getRoutingIndex(String serviceName) {
        long start = System.currentTimeMillis();
        log.info("Building Routing Index for service: {}", serviceName);

        List<Paragem> allParagens = paragemRepo.findAll();
        List<Trajeto> allTrajetos = trajetoRepo.findAll();
        List<Horario> schedules = horarioRepo.findByServicoNome(serviceName);

        log.info("Loaded {} paragens, {} trajetos, {} horarios for service {}", allParagens.size(), allTrajetos.size(), schedules.size(), serviceName);

        Map<Long, Paragem> paragemCache = new HashMap<>();
        Map<String, Set<Long>> nomeToIds = new HashMap<>();
        Map<Integer, String> zoneNameByNum = new TreeMap<>();
        for (Paragem p : allParagens) {
            paragemCache.put(p.getId(), p);
            nomeToIds.computeIfAbsent(normalizeStopGroupKey(p.getNome()), k -> new HashSet<>()).add(p.getId());
            if (p.getZona() != null) {
                zoneNameByNum.putIfAbsent(p.getZona().getNum(), p.getZona().getNome());
            }
        }

        Map<Long, List<PontosDePassagem>> trajetoPontosMap = new HashMap<>();
        Map<Long, String> destinoFinalMap = new HashMap<>();
        for (Trajeto t : allTrajetos) {
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

        Map<Long, Set<Long>> paragemToTrajetoIds = new HashMap<>();
        for (Map.Entry<Long, List<PontosDePassagem>> entry : trajetoPontosMap.entrySet()) {
            Long trajetoId = entry.getKey();
            for (PontosDePassagem pp : entry.getValue()) {
                if (pp.getParagem() != null) {
                    paragemToTrajetoIds.computeIfAbsent(pp.getParagem().getId(), k -> new HashSet<>()).add(trajetoId);
                }
            }
        }

        // Group Horarios by trajeto -> gtfsTripId -> list of schedules
        Map<Long, Map<String, List<Horario>>> groupedHorarios = new HashMap<>();
        for (Horario h : schedules) {
            if (h.getPontoPassagem() == null || h.getPontoPassagem().getTrajeto() == null) continue;
            Long trajetoId = h.getPontoPassagem().getTrajeto().getId();
            groupedHorarios
                    .computeIfAbsent(trajetoId, k -> new HashMap<>())
                    .computeIfAbsent(h.getGtfsTripId(), k -> new ArrayList<>())
                    .add(h);
        }

        Map<Long, List<CachedTrip>> trajetoTripsMap = new HashMap<>();
        for (Map.Entry<Long, Map<String, List<Horario>>> entry : groupedHorarios.entrySet()) {
            Long trajetoId = entry.getKey();
            List<PontosDePassagem> pontos = trajetoPontosMap.get(trajetoId);
            if (pontos == null || pontos.isEmpty()) continue;

            // Map stop ID to its index in the trajeto
            Map<Long, Integer> stopToIndex = new HashMap<>();
            for (int i = 0; i < pontos.size(); i++) {
                if (pontos.get(i).getParagem() != null) {
                    stopToIndex.put(pontos.get(i).getParagem().getId(), i);
                }
            }

            List<CachedTrip> cachedTrips = new ArrayList<>();
            for (Map.Entry<String, List<Horario>> tripEntry : entry.getValue().entrySet()) {
                String tripId = tripEntry.getKey();
                int[] times = new int[pontos.size()];
                Arrays.fill(times, Integer.MAX_VALUE);

                for (Horario h : tripEntry.getValue()) {
                    if (h.getPontoPassagem() != null && h.getPontoPassagem().getParagem() != null) {
                        Integer idx = stopToIndex.get(h.getPontoPassagem().getParagem().getId());
                        if (idx != null) {
                            times[idx] = h.getHora().getHour() * 60 + h.getHora().getMinute();
                        }
                    }
                }

                cachedTrips.add(new CachedTrip(tripId, times));
            }

            // Sort trips by departure time at the first stop (or first non-MAX_VALUE stop)
            cachedTrips.sort(Comparator.comparingInt(t -> {
                for (int time : t.getStopTimesMinutes()) {
                    if (time != Integer.MAX_VALUE) return time;
                }
                return Integer.MAX_VALUE;
            }));

            trajetoTripsMap.put(trajetoId, cachedTrips);
        }

        // Build walking edges
        Map<Long, List<WalkEdge>> nearbyStopsMap = new HashMap<>();
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
                if (dist <= MAX_INDEX_WALK_DIST_KM) {
                    int walkMin = Math.max(1, (int) Math.round(dist / WALKING_SPEED_KMH * 60));
                    edges.add(new WalkEdge(p2.getId(), walkMin));
                }
            }
            edges.sort(Comparator.comparingInt(WalkEdge::getMinutes));
            if (edges.size() > MAX_NEARBY) edges = edges.subList(0, MAX_NEARBY);
            if (!edges.isEmpty()) {
                nearbyStopsMap.put(p1.getId(), edges);
                edgeCount += edges.size();
            }
        }

        log.info("Routing Index for {} completed in {}ms. Cached {} stops, {} trips, {} walk edges.",
                serviceName, System.currentTimeMillis() - start, paragemCache.size(),
                trajetoTripsMap.values().stream().mapToInt(List::size).sum(), edgeCount);

        return new ServiceRoutingIndex(paragemCache, nomeToIds, trajetoPontosMap, trajetoTripsMap,
                paragemToTrajetoIds, destinoFinalMap, nearbyStopsMap, zoneNameByNum);
    }

    public static String normalizeStopGroupKey(String stopName) {
        if (stopName == null) return "";
        String normalized = Normalizer.normalize(stopName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .trim();
        normalized = GENERATED_SUFFIX_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized.replaceAll("[^A-Z0-9]+", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? stopName.toUpperCase(Locale.ROOT).trim() : normalized;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
