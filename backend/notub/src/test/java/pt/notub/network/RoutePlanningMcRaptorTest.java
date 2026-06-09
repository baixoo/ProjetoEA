package pt.notub.network;

import org.junit.jupiter.api.Test;
import pt.notub.models.*;
import pt.notub.network.dto.RotaDTO;
import pt.notub.repositories.PontosDePassagemRepository;
import pt.notub.repositories.TrajetoRepository;
import pt.notub.repositories.ViagemRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutePlanningMcRaptorTest {

    @Test
    void directRouteReportsOneZone() {
        Zona zone1 = mkZona(1, "Zona 1");
        Paragem origin = mkParagem(1L, "ORIGIN", 41.0, -8.0, zone1);
        Paragem middle = mkParagem(2L, "MIDDLE", 41.0, -8.1, zone1);
        Paragem destination = mkParagem(3L, "DESTINATION", 41.0, -8.2, zone1);

        Trajeto route = mkTrajeto(10L, "10", new Paragem[] { origin, middle, destination }, new int[] { 0, 5, 10 });
        RoutePlanningService service = buildService(List.of(route), List.of(mkViagem(1L, route, LocalTime.of(8, 0))));

        List<RotaDTO> routes = service.planearRota(origin.getId(), destination.getId(), LocalTime.of(7, 55), DayOfWeek.MONDAY);

        assertFalse(routes.isEmpty());
        RotaDTO first = routes.get(0);
        assertTrue(first.isDireta());
        assertEquals(1, first.getNrZonas());
        assertEquals(1, first.getZonas().get(0).getNum());
        assertEquals(1, first.getSegmentos().get(0).getNrZonas());
    }

    @Test
    void keepsEarlierMoreZonesAndLaterFewerZonesAsParetoAlternatives() {
        Zona zone1 = mkZona(1, "Zona 1");
        Zona zone2 = mkZona(2, "Zona 2");
        Paragem origin = mkParagem(1L, "ORIGIN", 41.0, -8.0, zone1);
        Paragem viaZone2 = mkParagem(2L, "VIA_ZONE_2", 41.0, -8.1, zone2);
        Paragem destination = mkParagem(3L, "DESTINATION", 41.0, -8.2, zone1);

        Trajeto fastMoreZones = mkTrajeto(10L, "FAST", new Paragem[] { origin, viaZone2, destination }, new int[] { 0, 5, 10 });
        Trajeto slowFewerZones = mkTrajeto(11L, "SLOW", new Paragem[] { origin, destination }, new int[] { 0, 20 });
        RoutePlanningService service = buildService(List.of(fastMoreZones, slowFewerZones), List.of(
                mkViagem(1L, fastMoreZones, LocalTime.of(8, 0)),
                mkViagem(2L, slowFewerZones, LocalTime.of(8, 0))
        ));

        List<RotaDTO> routes = service.planearRota(origin.getId(), destination.getId(), LocalTime.of(7, 55), DayOfWeek.MONDAY);

        assertTrue(routes.stream().anyMatch(r -> r.getNrZonas() == 1 && "SLOW".equals(r.getSegmentos().get(0).getLinhaNome())));
        assertTrue(routes.stream().anyMatch(r -> r.getNrZonas() == 2 && "FAST".equals(r.getSegmentos().get(0).getLinhaNome())));
    }

    @Test
    void removesRouteDominatedByArrivalRidesAndZones() {
        Zona zone1 = mkZona(1, "Zona 1");
        Zona zone2 = mkZona(2, "Zona 2");
        Paragem origin = mkParagem(1L, "ORIGIN", 41.0, -8.0, zone1);
        Paragem viaZone2 = mkParagem(2L, "VIA_ZONE_2", 41.0, -8.1, zone2);
        Paragem destination = mkParagem(3L, "DESTINATION", 41.0, -8.2, zone1);

        Trajeto dominant = mkTrajeto(10L, "DOMINANT", new Paragem[] { origin, destination }, new int[] { 0, 10 });
        Trajeto dominated = mkTrajeto(11L, "DOMINATED", new Paragem[] { origin, viaZone2, destination }, new int[] { 0, 10, 20 });
        RoutePlanningService service = buildService(List.of(dominant, dominated), List.of(
                mkViagem(1L, dominant, LocalTime.of(8, 0)),
                mkViagem(2L, dominated, LocalTime.of(8, 0))
        ));

        List<RotaDTO> routes = service.planearRota(origin.getId(), destination.getId(), LocalTime.of(7, 55), DayOfWeek.MONDAY);

        assertFalse(routes.isEmpty());
        assertTrue(routes.stream().allMatch(r -> r.getNrZonas() == 1));
        assertTrue(routes.stream().noneMatch(r -> "DOMINATED".equals(r.getSegmentos().get(0).getLinhaNome())));
    }

    @Test
    void walkingFootpathAcrossZonesIncrementsZoneCount() {
        Zona zone1 = mkZona(1, "Zona 1");
        Zona zone2 = mkZona(2, "Zona 2");
        Paragem origin = mkParagem(1L, "ORIGIN", 42.0, -8.0, zone1);
        Paragem destination = mkParagem(2L, "DESTINATION", 42.0, -8.001, zone2);

        Trajeto cacheOnlyRoute = mkTrajeto(10L, "CACHE", new Paragem[] { origin, destination }, new int[] { 0, 1 });
        RoutePlanningService service = buildService(List.of(cacheOnlyRoute), List.of());

        List<RotaDTO> routes = service.planearRota(origin.getId(), destination.getId(), LocalTime.of(8, 0), DayOfWeek.MONDAY);

        RotaDTO walking = routes.stream().filter(RotaDTO::isCaminho).findFirst().orElse(null);
        assertNotNull(walking);
        assertEquals(2, walking.getNrZonas());
        assertEquals("A pe", walking.getSegmentos().get(0).getLinhaNome());
        assertEquals(2, walking.getSegmentos().get(0).getNrZonas());
    }

    @Test
    void routePlannerDoesNotUseLegacyPriorityQueuePlanner() throws IOException {
        Path sourcePath = Path.of("src/main/java/pt/notub/network/RoutePlanningService.java");
        if (!Files.exists(sourcePath)) {
            sourcePath = Path.of("backend/notub/src/main/java/pt/notub/network/RoutePlanningService.java");
        }
        String source = Files.readString(sourcePath);

        assertFalse(source.contains("PriorityQueue"));
        assertFalse(source.contains("heuristicMinutes"));
        assertFalse(source.contains("findTransferRoutes"));
        assertFalse(source.contains("findDirectRoutes"));
        assertFalse(source.contains("MAX_STATES"));
    }

    private static RoutePlanningService buildService(List<Trajeto> trajetos, List<Viagem> viagens) {
        List<PontosDePassagem> pontos = new ArrayList<>();
        for (Trajeto trajeto : trajetos) {
            pontos.addAll(trajeto.getPontosDePassagem());
        }

        PontosDePassagemRepository pontosRepo = mock(PontosDePassagemRepository.class);
        TrajetoRepository trajetoRepo = mock(TrajetoRepository.class);
        ViagemRepository viagemRepo = mock(ViagemRepository.class);
        when(pontosRepo.findAll()).thenReturn(pontos);
        when(trajetoRepo.findAll()).thenReturn(trajetos);
        when(viagemRepo.findAll()).thenReturn(viagens);
        return new RoutePlanningService(pontosRepo, trajetoRepo, viagemRepo);
    }

    private static Zona mkZona(int num, String nome) {
        Zona zona = new Zona();
        zona.setId((long) num);
        zona.setNum(num);
        zona.setNome(nome);
        return zona;
    }

    private static Paragem mkParagem(long id, String nome, double lat, double lon, Zona zona) {
        Paragem paragem = new Paragem();
        paragem.setId(id);
        paragem.setNome(nome);
        paragem.setLocalizacao(new Point(lat, lon));
        paragem.setZona(zona);
        return paragem;
    }

    private static Trajeto mkTrajeto(long id, String linhaNome, Paragem[] paragens, int[] offsets) {
        Linha linha = new Linha();
        linha.setId(id);
        linha.setNome(linhaNome);

        Trajeto trajeto = new Trajeto();
        trajeto.setId(id);
        trajeto.setDirecao(Direcao.IDA);
        trajeto.setLinha(linha);

        List<PontosDePassagem> pontos = new ArrayList<>();
        for (int i = 0; i < paragens.length; i++) {
            PontosDePassagem ponto = new PontosDePassagem();
            ponto.setId(id * 100 + i);
            ponto.setOrdem(i);
            ponto.setTempoDesdeInicio(offsets[i]);
            ponto.setParagem(paragens[i]);
            pontos.add(ponto);
        }
        trajeto.setPontosDePassagem(pontos);
        return trajeto;
    }

    private static Viagem mkViagem(long id, Trajeto trajeto, LocalTime hora) {
        Viagem viagem = new Viagem();
        viagem.setId(id);
        viagem.setTrajeto(trajeto);
        viagem.setServiceId("UTEIS");
        viagem.setHoraPartida(hora);
        return viagem;
    }
}
