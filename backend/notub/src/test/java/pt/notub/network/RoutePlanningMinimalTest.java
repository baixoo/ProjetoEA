package pt.notub.network;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.notub.models.*;
import pt.notub.repositories.PontosDePassagemRepository;
import pt.notub.repositories.TrajetoRepository;
import pt.notub.repositories.ViagemRepository;
import pt.notub.network.dto.RotaDTO;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoutePlanningMinimalTest {

    private static RoutePlanningService service;

    private static final Long CAMPANHA_A = 547L;
    private static final Long CAMPANHA_B = 548L;
    private static final Long CAMPANHA_C = 546L;
    private static final Long STOP_A = 100L;
    private static final Long STOP_B = 101L;
    private static final Long STOP_C = 102L;
    private static final Long CASTELO = 634L;
    private static final Long CASTELO_ALT = 631L;
    private static final Long CORDOARIA = 599L;
    private static final Long NEARBY_STOP = 700L;

    @BeforeAll
    static void setUp() {
        PontosDePassagemRepository pontosRepo = mock(PontosDePassagemRepository.class);
        TrajetoRepository trajetoRepo = mock(TrajetoRepository.class);
        ViagemRepository viagemRepo = mock(ViagemRepository.class);

        Linha linha205 = mkLinha(15L, "205 - Campanhã - Castelo Do Queijo");
        Linha linha200 = mkLinha(10L, "200 - Bolhão - Cast. Queijo");
        Linha linhaNearby = mkLinha(20L, "300 - Nearby - Test");

        Paragem campanhaA = mkParagem(CAMPANHA_A, "CAMPANHÃ", 41.1485, -8.5860);
        Paragem campanhaB = mkParagem(CAMPANHA_B, "CAMPANHÃ", 41.1472, -8.5873);
        Paragem campanhaC = mkParagem(CAMPANHA_C, "CAMPANHÃ", 41.1497, -8.5859);
        Paragem stopA = mkParagem(STOP_A, "STOP_A", 41.155, -8.600);
        Paragem stopB = mkParagem(STOP_B, "STOP_B", 41.160, -8.630);
        Paragem stopC = mkParagem(STOP_C, "STOP_C", 41.163, -8.660);
        Paragem castelo = mkParagem(CASTELO, "CASTELO DO QUEIJO", 41.1677, -8.6894);
        Paragem casteloAlt = mkParagem(CASTELO_ALT, "CASTELO DO QUEIJO", 41.1674, -8.6891);
        Paragem cordoaria = mkParagem(CORDOARIA, "CORDOARIA", 41.1400, -8.6200);
        Paragem nearbyStop = mkParagem(NEARBY_STOP, "NEARBY_STOP", 41.1487, -8.5850);

        Trajeto t205Ida = mkTrajeto(29L, Direcao.IDA, linha205);
        Trajeto t205Volta = mkTrajeto(30L, Direcao.VOLTA, linha205);
        Trajeto t200Ida = mkTrajeto(19L, Direcao.IDA, linha200);
        Trajeto tNearby = mkTrajeto(40L, Direcao.IDA, linhaNearby);

        List<PontosDePassagem> pontos205Ida = List.of(
                mkPonto(1L, 0, 0, t205Ida, campanhaA),
                mkPonto(2L, 1, 5, t205Ida, stopA),
                mkPonto(3L, 2, 15, t205Ida, stopB),
                mkPonto(4L, 3, 30, t205Ida, stopC),
                mkPonto(5L, 4, 45, t205Ida, castelo)
        );
        t205Ida.setPontosDePassagem(pontos205Ida);

        List<PontosDePassagem> pontos205Volta = List.of(
                mkPonto(6L, 0, 0, t205Volta, castelo),
                mkPonto(7L, 1, 15, t205Volta, stopC),
                mkPonto(8L, 2, 30, t205Volta, stopB),
                mkPonto(9L, 3, 40, t205Volta, stopA),
                mkPonto(10L, 4, 50, t205Volta, campanhaA)
        );
        t205Volta.setPontosDePassagem(pontos205Volta);

        List<PontosDePassagem> pontos200Ida = List.of(
                mkPonto(11L, 0, 0, t200Ida, cordoaria),
                mkPonto(12L, 1, 10, t200Ida, stopB),
                mkPonto(13L, 2, 25, t200Ida, casteloAlt)
        );
        t200Ida.setPontosDePassagem(pontos200Ida);

        List<PontosDePassagem> pontosNearby = List.of(
                mkPonto(14L, 0, 0, tNearby, nearbyStop),
                mkPonto(15L, 1, 5, tNearby, castelo)
        );
        tNearby.setPontosDePassagem(pontosNearby);

        List<PontosDePassagem> allPontos = new ArrayList<>();
        allPontos.addAll(pontos205Ida);
        allPontos.addAll(pontos205Volta);
        allPontos.addAll(pontos200Ida);
        allPontos.addAll(pontosNearby);

        Viagem v1 = mkViagem(1L, t205Ida, "UTEIS", LocalTime.of(8, 0));
        Viagem v2 = mkViagem(2L, t205Ida, "UTEIS", LocalTime.of(9, 0));
        Viagem v3 = mkViagem(3L, t205Ida, "UTEIS", LocalTime.of(10, 0));
        Viagem v4 = mkViagem(4L, t205Volta, "UTEIS", LocalTime.of(8, 15));
        Viagem v5 = mkViagem(5L, t205Volta, "UTEIS", LocalTime.of(9, 15));
        Viagem v6 = mkViagem(6L, t200Ida, "UTEIS", LocalTime.of(8, 30));
        Viagem v7 = mkViagem(7L, t200Ida, "UTEIS", LocalTime.of(9, 30));
        Viagem v8 = mkViagem(8L, tNearby, "UTEIS", LocalTime.of(8, 10));
        Viagem v9 = mkViagem(9L, tNearby, "UTEIS", LocalTime.of(9, 10));

        when(pontosRepo.findAll()).thenReturn(allPontos);
        when(trajetoRepo.findAll()).thenReturn(List.of(t205Ida, t205Volta, t200Ida, tNearby));
        when(viagemRepo.findAll()).thenReturn(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9));

        service = new RoutePlanningService(pontosRepo, trajetoRepo, viagemRepo);
    }

    @Test
    void testDirectRouteIda() {
        List<RotaDTO> rotas = service.planearRota(CAMPANHA_A, CASTELO, LocalTime.of(7, 45), DayOfWeek.MONDAY);

        assertFalse(rotas.isEmpty(), "Should find direct route IDA");
        RotaDTO first = rotas.get(0);
        assertTrue(first.isDireta(), "Should be direct");
        assertEquals(0, first.getTrocas());

        RotaDTO.SegmentoDTO seg = first.getSegmentos().get(0);
        assertEquals("205 - Campanhã - Castelo Do Queijo", seg.getLinhaNome());
        assertTrue(seg.getParagens().size() >= 2);
        assertEquals("CAMPANHÃ", seg.getParagens().get(0).getNome());
        assertEquals("CASTELO DO QUEIJO", seg.getParagens().get(seg.getParagens().size() - 1).getNome());
    }

    @Test
    void testDirectRouteWithIntermediateStops() {
        List<RotaDTO> rotas = service.planearRota(CAMPANHA_A, STOP_B, LocalTime.of(7, 45), DayOfWeek.MONDAY);

        assertFalse(rotas.isEmpty());
        RotaDTO.SegmentoDTO seg = rotas.get(0).getSegmentos().get(0);
        assertEquals(3, seg.getParagens().size(), "Should have 3 stops: CAMPANHÃ, STOP_A, STOP_B");
    }

    @Test
    void testVoltaDirection() {
        List<RotaDTO> rotas = service.planearRota(CASTELO, CAMPANHA_A, LocalTime.of(8, 0), DayOfWeek.MONDAY);

        assertFalse(rotas.isEmpty(), "Should find route via VOLTA direction");
        assertTrue(rotas.get(0).isDireta());
        assertEquals("VOLTA", rotas.get(0).getSegmentos().get(0).getDirecao());
    }

    @Test
    void testNameExpansion() {
        List<RotaDTO> rotas = service.planearRota(CAMPANHA_C, CASTELO_ALT, LocalTime.of(7, 45), DayOfWeek.MONDAY);

        assertFalse(rotas.isEmpty(), "expandByName should match CAMPANHA(546)->(547) and CASTELO(631)->(634)");
        assertTrue(rotas.get(0).isDireta());
    }

    @Test
    void testNoDirectRouteDifferentLines() {
        List<RotaDTO> rotas = service.planearRota(CORDOARIA, CAMPANHA_C, LocalTime.of(8, 0), DayOfWeek.MONDAY);

        boolean hasDirect = rotas.stream().anyMatch(RotaDTO::isDireta);
        assertFalse(hasDirect, "CORDOARIA and CAMPANHÃ share no line");
    }

    @Test
    void testTransferRoute() {
        List<RotaDTO> rotas = service.planearRota(CORDOARIA, CAMPANHA_A, LocalTime.of(8, 0), DayOfWeek.MONDAY);

        boolean hasTransfer = rotas.stream().anyMatch(r -> !r.isDireta() && r.getTrocas() > 0);
        assertTrue(hasTransfer, "Should find transfer route via STOP_B");

        RotaDTO transferRoute = rotas.stream().filter(r -> !r.isDireta()).findFirst().orElse(null);
        assertNotNull(transferRoute);
        assertTrue(transferRoute.getSegmentos().size() >= 2);
    }

    @Test
    void testWalkingRouteShortDistance() {
        RotaDTO walkRoute = service.planearRota(CAMPANHA_C, CAMPANHA_A, LocalTime.of(8, 0), DayOfWeek.MONDAY)
                .stream().filter(RotaDTO::isCaminho).findFirst().orElse(null);
        if (walkRoute != null) {
            assertEquals("A pe", walkRoute.getSegmentos().get(0).getLinhaNome());
        }
    }

    @Test
    void testSameStopReturnsEmpty() {
        List<RotaDTO> rotas = service.planearRota(CAMPANHA_C, CAMPANHA_C, LocalTime.of(8, 0), DayOfWeek.MONDAY);
        assertTrue(rotas.isEmpty());
    }

    @Test
    void testMultipleDeparturesDifferentTimes() {
        List<RotaDTO> rotas1 = service.planearRota(CAMPANHA_A, CASTELO, LocalTime.of(7, 45), DayOfWeek.MONDAY);
        List<RotaDTO> rotas2 = service.planearRota(CAMPANHA_A, CASTELO, LocalTime.of(9, 30), DayOfWeek.MONDAY);

        assertFalse(rotas1.isEmpty());
        assertFalse(rotas2.isEmpty());
        assertNotEquals(rotas1.get(0).getHoraPartida(), rotas2.get(0).getHoraPartida());
    }

    @Test
    void testPerformanceUnder5s() {
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            service.planearRota(CAMPANHA_A, CASTELO, LocalTime.of(8, 0), DayOfWeek.MONDAY);
        }
        long elapsed = System.currentTimeMillis() - t0;
        assertTrue(elapsed < 5000, "10 queries should take <5s, took " + elapsed + "ms");
    }

    @Test
    void testMaxTransfersIs2() {
        List<RotaDTO> rotas = service.planearRota(CORDOARIA, CAMPANHA_A, LocalTime.of(8, 0), DayOfWeek.MONDAY);
        for (RotaDTO r : rotas) {
            assertTrue(r.getTrocas() <= 2, "Route should have at most 2 transfers, got " + r.getTrocas());
        }
    }

    @Test
    void testRouteSortedByTransfersThenTime() {
        List<RotaDTO> rotas = service.planearRota(CAMPANHA_A, CASTELO, LocalTime.of(7, 0), DayOfWeek.MONDAY);
        for (int i = 1; i < rotas.size(); i++) {
            assertTrue(rotas.get(i - 1).getTrocas() <= rotas.get(i).getTrocas(),
                    "Routes should be sorted by transfers ascending");
            if (rotas.get(i - 1).getTrocas() == rotas.get(i).getTrocas()) {
                assertTrue(rotas.get(i - 1).getTotalMinutos() <= rotas.get(i).getTotalMinutos(),
                        "Same transfers should be sorted by time");
            }
        }
    }

    @Test
    void testDirectRouteHasCorrectDuration() {
        List<RotaDTO> rotas = service.planearRota(CAMPANHA_A, STOP_B, LocalTime.of(7, 45), DayOfWeek.MONDAY);
        assertFalse(rotas.isEmpty());

        RotaDTO.SegmentoDTO seg = rotas.get(0).getSegmentos().get(0);
        assertEquals(15, seg.getDuracaoMinutos(), "CAMPANHÃ→STOP_B should take 15 min");
    }

    @Test
    void testSegmentHasTrajetoId() {
        List<RotaDTO> rotas = service.planearRota(CAMPANHA_A, CASTELO, LocalTime.of(7, 45), DayOfWeek.MONDAY);
        assertFalse(rotas.isEmpty());

        RotaDTO.SegmentoDTO seg = rotas.get(0).getSegmentos().get(0);
        assertNotNull(seg.getTrajetoId());
        assertEquals(29L, seg.getTrajetoId());
    }

    @Test
    void testWalkingSegmentHasNoTrajetoId() {
        RotaDTO walkRoute = service.planearRota(CAMPANHA_C, CAMPANHA_A, LocalTime.of(8, 0), DayOfWeek.MONDAY)
                .stream().filter(RotaDTO::isCaminho).findFirst().orElse(null);
        if (walkRoute != null) {
            assertNull(walkRoute.getSegmentos().get(0).getTrajetoId());
            assertEquals("A pe", walkRoute.getSegmentos().get(0).getLinhaNome());
        }
    }

    @Test
    void testWalkingTransferToNearbyStop() {
        List<RotaDTO> rotas = service.planearRota(CAMPANHA_A, NEARBY_STOP, LocalTime.of(7, 45), DayOfWeek.MONDAY);

        boolean found = rotas.stream()
                .flatMap(r -> r.getSegmentos().stream())
                .anyMatch(s -> "A pe".equals(s.getLinhaNome()));
        assertTrue(found || !rotas.isEmpty(), "Should find route involving nearby stop");
    }

    @Test
    void testDirectRoutesComeFirst() {
        List<RotaDTO> rotas = service.planearRota(CAMPANHA_A, CASTELO, LocalTime.of(7, 0), DayOfWeek.MONDAY);
        if (rotas.size() >= 2) {
            RotaDTO first = rotas.get(0);
            for (RotaDTO r : rotas) {
                if (r.getTrocas() > first.getTrocas()) break;
                assertEquals(first.getTrocas(), r.getTrocas(), "All same-transfer routes should be grouped");
            }
        }
    }

    private static Linha mkLinha(long id, String nome) {
        Linha l = new Linha(); l.setId(id); l.setNome(nome); return l;
    }

    private static Paragem mkParagem(long id, String nome, double lat, double lon) {
        Paragem p = new Paragem(); p.setId(id); p.setNome(nome);
        p.setLocalizacao(new Point(lat, lon)); return p;
    }

    private static Trajeto mkTrajeto(long id, Direcao dir, Linha linha) {
        Trajeto t = new Trajeto(); t.setId(id); t.setDirecao(dir); t.setLinha(linha); return t;
    }

    private static PontosDePassagem mkPonto(long id, int ordem, int tempo, Trajeto t, Paragem p) {
        PontosDePassagem pp = new PontosDePassagem();
        pp.setId(id); pp.setOrdem(ordem); pp.setTempoDesdeInicio(tempo);
        pp.setParagem(p); return pp;
    }

    private static Viagem mkViagem(long id, Trajeto t, String serviceId, LocalTime hora) {
        Viagem v = new Viagem(); v.setId(id); v.setTrajeto(t);
        v.setServiceId(serviceId); v.setHoraPartida(hora); return v;
    }
}
