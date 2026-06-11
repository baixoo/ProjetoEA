package pt.notub.network.service;

import org.springframework.stereotype.Service;
import pt.notub.network.dto.RotaDTO;
import pt.notub.network.dto.ZonaResumoDTO;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.service.RoutingIndexService.ServiceRoutingIndex;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteDtoAssembler {

    private final WalkingTransferPolicy walkingPolicy;

    public RouteDtoAssembler(WalkingTransferPolicy walkingPolicy) {
        this.walkingPolicy = walkingPolicy;
    }

    public enum LegType {
        RIDE,
        WALK
    }

    public static class RouteLeg {
        private final LegType type;
        private final Long fromStopId;
        private final Long toStopId;
        private final Long trajetoId; // null for WALK
        private final int departureMinutes;
        private final int arrivalMinutes;
        private final int boardIndex; // -1 for WALK
        private final int alightIndex; // -1 for WALK
        private final int walkMinutes; // 0 for RIDE

        public RouteLeg(LegType type, Long fromStopId, Long toStopId, Long trajetoId,
                        int departureMinutes, int arrivalMinutes, int boardIndex, int alightIndex, int walkMinutes) {
            this.type = type;
            this.fromStopId = fromStopId;
            this.toStopId = toStopId;
            this.trajetoId = trajetoId;
            this.departureMinutes = departureMinutes;
            this.arrivalMinutes = arrivalMinutes;
            this.boardIndex = boardIndex;
            this.alightIndex = alightIndex;
            this.walkMinutes = walkMinutes;
        }

        public LegType getType() { return type; }
        public Long getFromStopId() { return fromStopId; }
        public Long getToStopId() { return toStopId; }
        public Long getTrajetoId() { return trajetoId; }
        public int getDepartureMinutes() { return departureMinutes; }
        public int getArrivalMinutes() { return arrivalMinutes; }
        public int getBoardIndex() { return boardIndex; }
        public int getAlightIndex() { return alightIndex; }
        public int getWalkMinutes() { return walkMinutes; }
    }

    public RotaDTO assembleRoute(List<RouteLeg> legs, ServiceRoutingIndex index, int queryMinutes) {
        if (legs == null || legs.isEmpty()) return null;

        List<RotaDTO.SegmentoDTO> segmentos = new ArrayList<>();
        int currentArrivalMinutes = queryMinutes;

        for (RouteLeg leg : legs) {
            if (leg.getType() == LegType.RIDE) {
                RotaDTO.SegmentoDTO seg = buildRideSegment(leg, index, currentArrivalMinutes);
                if (seg != null) {
                    segmentos.add(seg);
                }
                currentArrivalMinutes = leg.getArrivalMinutes();
            } else if (leg.getType() == LegType.WALK) {
                Paragem from = index.getParagemCache().get(leg.getFromStopId());
                Paragem to = index.getParagemCache().get(leg.getToStopId());

                if (walkingPolicy.shouldShowWalkSegment(from, to)) {
                    RotaDTO.SegmentoDTO seg = buildWalkSegment(leg, from, to, index, currentArrivalMinutes);
                    if (seg != null) {
                        segmentos.add(seg);
                    }
                }
                currentArrivalMinutes = leg.getArrivalMinutes();
            }
        }

        if (segmentos.isEmpty()) return null;

        RotaDTO rota = new RotaDTO();
        long rideSegmentsCount = segmentos.stream().filter(s -> !"A pe".equals(s.getLinhaNome())).count();
        int trocas = Math.max(0, (int) rideSegmentsCount - 1);
        int totalWalkMin = legs.stream()
                .filter(l -> l.getType() == LegType.WALK)
                .mapToInt(RouteLeg::getWalkMinutes)
                .sum();

        int finalArrivalMinutes = legs.get(legs.size() - 1).getArrivalMinutes();
        int totalMinutos = Math.max(0, finalArrivalMinutes - queryMinutes);

        boolean hasWalkLeg = segmentos.stream().anyMatch(seg -> "A pe".equals(seg.getLinhaNome()));
        boolean direta = (rideSegmentsCount == 1 && !hasWalkLeg);
        boolean caminho = (segmentos.size() == 1 && "A pe".equals(segmentos.get(0).getLinhaNome()));

        Set<Integer> routeZones = new TreeSet<>();
        for (RotaDTO.SegmentoDTO segmento : segmentos) {
            if (segmento.getZonas() != null) {
                for (ZonaResumoDTO z : segmento.getZonas()) {
                    routeZones.add(z.getNum());
                }
            }
        }

        rota.setTotalMinutos(totalMinutos);
        rota.setTrocas(trocas);
        rota.setTotalCaminhadaMinutos(totalWalkMin);
        rota.setDireta(direta);
        rota.setCaminho(caminho);
        rota.setSegmentos(segmentos);
        rota.setZonas(toZonaResumoDtos(routeZones, index));
        rota.setNrZonas(routeZones.size());
        rota.setHoraPartida(minutesToTime(queryMinutes));
        rota.setHoraChegada(minutesToTime(finalArrivalMinutes));

        return rota;
    }

    public RotaDTO buildWalkingOnlyRoute(Paragem from, Paragem to, int walkMin, int queryMinutes, ServiceRoutingIndex index) {
        if (from == null || to == null) return null;

        RotaDTO.SegmentoDTO seg = new RotaDTO.SegmentoDTO();
        seg.setLinhaNome("A pe");
        seg.setOrigem(toParagemDTO(from));
        seg.setDestino(toParagemDTO(to));
        seg.setParagens(List.of(toParagemDTO(from), toParagemDTO(to)));
        seg.setDuracaoMinutos(walkMin);
        seg.setTempoCaminhadaMinutos(walkMin);
        seg.setHoraPartida(minutesToTime(queryMinutes));
        seg.setHoraChegada(minutesToTime(queryMinutes + walkMin));

        Set<Integer> segmentZones = new TreeSet<>();
        addStopZone(segmentZones, from.getId(), index);
        addStopZone(segmentZones, to.getId(), index);
        seg.setZonas(toZonaResumoDtos(segmentZones, index));
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
        rota.setZonas(toZonaResumoDtos(segmentZones, index));
        rota.setNrZonas(segmentZones.size());
        return rota;
    }

    private RotaDTO.SegmentoDTO buildRideSegment(RouteLeg leg, ServiceRoutingIndex index, int readyTime) {
        Long trajetoId = leg.getTrajetoId();
        List<PontosDePassagem> pontos = index.getTrajetoPontosMap().get(trajetoId);
        if (pontos == null || leg.getBoardIndex() < 0 || leg.getAlightIndex() <= leg.getBoardIndex() || leg.getAlightIndex() >= pontos.size()) {
            return null;
        }

        RotaDTO.SegmentoDTO seg = new RotaDTO.SegmentoDTO();
        seg.setTrajetoId(trajetoId);

        // Fetch Trajeto details
        String destinoFinal = index.getDestinoFinalMap().getOrDefault(trajetoId, "");
        seg.setDestinoFinal(destinoFinal);

        // We need to look up details for Linha and Direcao from the PontosDePassagem (which is connected to Trajeto)
        Trajeto trajeto = pontos.get(0).getTrajeto();
        if (trajeto != null) {
            seg.setLinhaNome(trajeto.getLinha() != null ? trajeto.getLinha().getNome() : "Linha");
            seg.setDirecao(trajeto.getDirecao() != null ? trajeto.getDirecao().name() : "");
        }

        PontosDePassagem boardPonto = pontos.get(leg.getBoardIndex());
        PontosDePassagem alightPonto = pontos.get(leg.getAlightIndex());
        Paragem boardParagem = index.getParagemCache().get(boardPonto.getParagem().getId());
        Paragem alightParagem = index.getParagemCache().get(alightPonto.getParagem().getId());

        if (boardParagem != null) seg.setOrigem(toParagemDTO(boardParagem));
        if (alightParagem != null) seg.setDestino(toParagemDTO(alightParagem));

        List<RotaDTO.ParagemDTO> paragens = new ArrayList<>();
        Set<Integer> segmentZones = new TreeSet<>();
        for (int i = leg.getBoardIndex(); i <= leg.getAlightIndex(); i++) {
            Paragem p = index.getParagemCache().get(pontos.get(i).getParagem().getId());
            if (p != null) {
                paragens.add(toParagemDTO(p));
                if (p.getZona() != null) {
                    segmentZones.add(p.getZona().getNum());
                }
            }
        }
        seg.setParagens(paragens);
        seg.setDuracaoMinutos(Math.max(1, leg.getArrivalMinutes() - leg.getDepartureMinutes()));
        seg.setEsperaMinutos(Math.max(0, leg.getDepartureMinutes() - readyTime));
        seg.setHoraPartida(minutesToTime(leg.getDepartureMinutes()));
        seg.setHoraChegada(minutesToTime(leg.getArrivalMinutes()));
        seg.setZonas(toZonaResumoDtos(segmentZones, index));
        seg.setNrZonas(segmentZones.size());

        return seg;
    }

    private RotaDTO.SegmentoDTO buildWalkSegment(RouteLeg leg, Paragem from, Paragem to, ServiceRoutingIndex index, int readyTime) {
        if (from == null || to == null) return null;

        RotaDTO.SegmentoDTO seg = new RotaDTO.SegmentoDTO();
        seg.setLinhaNome("A pe");
        seg.setOrigem(toParagemDTO(from));
        seg.setDestino(toParagemDTO(to));
        seg.setParagens(List.of(toParagemDTO(from), toParagemDTO(to)));
        seg.setDuracaoMinutos(Math.max(0, leg.getArrivalMinutes() - leg.getDepartureMinutes()));
        seg.setTempoCaminhadaMinutos(Math.max(0, leg.getWalkMinutes()));
        seg.setHoraPartida(minutesToTime(leg.getDepartureMinutes()));
        seg.setHoraChegada(minutesToTime(leg.getArrivalMinutes()));
        seg.setEsperaMinutos(Math.max(0, leg.getDepartureMinutes() - readyTime));

        Set<Integer> segmentZones = new TreeSet<>();
        addStopZone(segmentZones, from.getId(), index);
        addStopZone(segmentZones, to.getId(), index);
        seg.setZonas(toZonaResumoDtos(segmentZones, index));
        seg.setNrZonas(segmentZones.size());

        return seg;
    }

    private RotaDTO.ParagemDTO toParagemDTO(Paragem p) {
        if (p == null) return null;
        Double lat = p.getLocalizacao() != null ? p.getLocalizacao().getLatitude() : null;
        Double lon = p.getLocalizacao() != null ? p.getLocalizacao().getLongitude() : null;
        return new RotaDTO.ParagemDTO(p.getId(), p.getNome(), lat, lon);
    }

    private void addStopZone(Set<Integer> zones, Long stopId, ServiceRoutingIndex index) {
        Paragem p = index.getParagemCache().get(stopId);
        if (p != null && p.getZona() != null) {
            zones.add(p.getZona().getNum());
        }
    }

    private List<ZonaResumoDTO> toZonaResumoDtos(Set<Integer> zoneNums, ServiceRoutingIndex index) {
        if (zoneNums == null || zoneNums.isEmpty()) return List.of();
        return zoneNums.stream()
                .sorted()
                .map(num -> new ZonaResumoDTO(num, index.getZoneNameByNum().getOrDefault(num, "Zona " + num)))
                .collect(Collectors.toList());
    }

    private String minutesToTime(int totalMinutes) {
        int minutesInDay = Math.floorMod(totalMinutes, 1440);
        int h = minutesInDay / 60;
        int m = minutesInDay % 60;
        return String.format("%02d:%02d", h, m);
    }
}
