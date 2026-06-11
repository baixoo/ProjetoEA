package pt.notub.network.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.network.dto.*;
import pt.notub.network.entity.*;
import pt.notub.network.repository.HorarioRepository;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.PontosDePassagemRepository;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.vehicle.entity.Point;
import pt.notub.zone.entity.Zona;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingRefactorTest {

    @Mock
    private HorarioRepository horarioRepository;

    @Mock
    private ParagemRepository paragemRepository;

    @Mock
    private TrajetoRepository trajetoRepository;

    @Mock
    private PontosDePassagemRepository pontosRepository;

    private RoutingIndexService indexService;
    private WalkingTransferPolicy walkingPolicy;
    private RouteDtoAssembler assembler;
    private RouteSearchEngine searchEngine;
    private ScheduleQueryService queryService;

    private Paragem aeroporto;
    private Paragem campanha;
    private Paragem hospSJoao;
    private Paragem hospSJoaoII;

    private Trajeto t604;
    private Trajeto t404;

    private Servico servicoUteis;

    @BeforeEach
    void setUp() {
        indexService = new RoutingIndexService(horarioRepository, paragemRepository, trajetoRepository, pontosRepository);
        walkingPolicy = new WalkingTransferPolicy();
        assembler = new RouteDtoAssembler(walkingPolicy);
        searchEngine = new RouteSearchEngine(walkingPolicy, assembler);
        queryService = new ScheduleQueryService(horarioRepository, paragemRepository, trajetoRepository, pontosRepository);

        Zona z1 = new Zona();
        z1.setId(1L);
        z1.setNum(1);
        z1.setNome("Zona 1");

        Zona z2 = new Zona();
        z2.setId(2L);
        z2.setNum(2);
        z2.setNome("Zona 2");

        // 95: Aeroporto (lat: 41.2424, lon: -8.6724)
        aeroporto = new Paragem();
        aeroporto.setId(95L);
        aeroporto.setNome("Aeroporto");
        aeroporto.setLocalizacao(new Point(41.2424, -8.6724));
        aeroporto.setZona(z1);

        // 2330: Terminal Intermodal Campanha II (lat: 41.1495, lon: -8.5855)
        campanha = new Paragem();
        campanha.setId(2330L);
        campanha.setNome("Terminal Intermodal Campanha II");
        campanha.setLocalizacao(new Point(41.1495, -8.5855));
        campanha.setZona(z2);

        // 1000: Hosp. S. Joao (lat: 41.1805, lon: -8.6045)
        hospSJoao = new Paragem();
        hospSJoao.setId(1000L);
        hospSJoao.setNome("Hosp. S. Joao");
        hospSJoao.setLocalizacao(new Point(41.1805, -8.6045));
        hospSJoao.setZona(z1);

        // 1001: Hosp. S. Joao II (lat: 41.1804, lon: -8.6035) - distance is ~108m
        hospSJoaoII = new Paragem();
        hospSJoaoII.setId(1001L);
        hospSJoaoII.setNome("Hosp. S. Joao II");
        hospSJoaoII.setLocalizacao(new Point(41.1804, -8.6035));
        hospSJoaoII.setZona(z1);

        servicoUteis = new Servico();
        servicoUteis.setId(1L);
        servicoUteis.setNome("UTEIS");

        Linha l604 = new Linha();
        l604.setId(10L);
        l604.setNome("604");

        Linha l404 = new Linha();
        l404.setId(11L);
        l404.setNome("404");

        t604 = new Trajeto();
        t604.setId(100L);
        t604.setDirecao(Direcao.IDA);
        t604.setLinha(l604);

        PontosDePassagem p604_1 = new PontosDePassagem();
        p604_1.setId(10001L);
        p604_1.setOrdem(0);
        p604_1.setParagem(aeroporto);
        p604_1.setTrajeto(t604);

        PontosDePassagem p604_2 = new PontosDePassagem();
        p604_2.setId(10002L);
        p604_2.setOrdem(1);
        p604_2.setParagem(hospSJoao);
        p604_2.setTrajeto(t604);

        t604.setPontosDePassagem(List.of(p604_1, p604_2));

        t404 = new Trajeto();
        t404.setId(101L);
        t404.setDirecao(Direcao.IDA);
        t404.setLinha(l404);

        PontosDePassagem p404_1 = new PontosDePassagem();
        p404_1.setId(20001L);
        p404_1.setOrdem(0);
        p404_1.setParagem(hospSJoaoII);
        p404_1.setTrajeto(t404);

        PontosDePassagem p404_2 = new PontosDePassagem();
        p404_2.setId(20002L);
        p404_2.setOrdem(1);
        p404_2.setParagem(campanha);
        p404_2.setTrajeto(t404);

        t404.setPontosDePassagem(List.of(p404_1, p404_2));

        lenient().when(paragemRepository.findAll()).thenReturn(List.of(aeroporto, campanha, hospSJoao, hospSJoaoII));
        lenient().when(trajetoRepository.findAll()).thenReturn(List.of(t604, t404));
        lenient().when(pontosRepository.findAll()).thenReturn(List.of(p604_1, p604_2, p404_1, p404_2));
    }

    @Test
    void testAeroportoCampanhaRoute() {
        // Setup schedules (Horarios)
        Horario h1_aeroporto = new Horario();
        h1_aeroporto.setId(1L);
        h1_aeroporto.setHora(LocalTime.of(8, 0));
        h1_aeroporto.setPontoPassagem(t604.getPontosDePassagem().get(0));
        h1_aeroporto.setServico(servicoUteis);
        h1_aeroporto.setGtfsTripId("TRIP604");

        Horario h1_hsj = new Horario();
        h1_hsj.setId(2L);
        h1_hsj.setHora(LocalTime.of(8, 20));
        h1_hsj.setPontoPassagem(t604.getPontosDePassagem().get(1));
        h1_hsj.setServico(servicoUteis);
        h1_hsj.setGtfsTripId("TRIP604");

        Horario h2_hsj2 = new Horario();
        h2_hsj2.setId(3L);
        h2_hsj2.setHora(LocalTime.of(8, 25));
        h2_hsj2.setPontoPassagem(t404.getPontosDePassagem().get(0));
        h2_hsj2.setServico(servicoUteis);
        h2_hsj2.setGtfsTripId("TRIP404");

        Horario h2_camp = new Horario();
        h2_camp.setId(4L);
        h2_camp.setHora(LocalTime.of(8, 45));
        h2_camp.setPontoPassagem(t404.getPontosDePassagem().get(1));
        h2_camp.setServico(servicoUteis);
        h2_camp.setGtfsTripId("TRIP404");

        when(horarioRepository.findByServicoNome("UTEIS"))
                .thenReturn(List.of(h1_aeroporto, h1_hsj, h2_hsj2, h2_camp));

        RoutingIndexService.ServiceRoutingIndex index = indexService.getRoutingIndex("UTEIS");
        List<RotaDTO> routes = searchEngine.search(95L, 2330L, 8 * 60, index);

        assertFalse(routes.isEmpty(), "Should find at least one route");
        RotaDTO topRoute = routes.get(0);

        // Verification
        assertEquals(45, topRoute.getTotalMinutos());
        assertEquals(1, topRoute.getTrocas(), "Should require 1 transfer");

        List<RotaDTO.SegmentoDTO> segments = topRoute.getSegmentos();
        assertEquals(3, segments.size(), "Should consist of: ride 604 -> walk -> ride 404");

        assertEquals("604", segments.get(0).getLinhaNome());
        assertEquals("A pe", segments.get(1).getLinhaNome());
        assertEquals("404", segments.get(2).getLinhaNome());

        // Verify total walk minutes
        assertTrue(topRoute.getTotalCaminhadaMinutos() > 0);
        assertEquals(1, topRoute.getTotalCaminhadaMinutos(), "Walking 108m at 4km/h takes approx 1.6 mins, rounded to 1");

        // Verify zones
        assertEquals(2, topRoute.getNrZonas(), "Should cover Zone 1 and Zone 2");

        // Standalone walk route between Aeroporto and Campanha should not be returned (12km > 800m limit)
        boolean hasWalkingOnlyRoute = routes.stream().anyMatch(RotaDTO::isCaminho);
        assertFalse(hasWalkingOnlyRoute, "Should not return a direct walk route for 12km");
    }

    @Test
    void testWalkingPolicyDirectWalkRejection() {
        // Clear all bus schedules, only direct walk candidate could exist
        when(horarioRepository.findByServicoNome("UTEIS")).thenReturn(List.of());
        RoutingIndexService.ServiceRoutingIndex index = indexService.getRoutingIndex("UTEIS");

        List<RotaDTO> routes = searchEngine.search(95L, 2330L, 8 * 60, index);
        assertTrue(routes.isEmpty(), "12km direct walk should be rejected");
    }

    @Test
    void testWalkingPolicyDirectWalkAcceptance() {
        // Create Stop Close A and Close B (distance ~300m)
        Paragem closeA = new Paragem();
        closeA.setId(9001L);
        closeA.setNome("Close A");
        closeA.setLocalizacao(new Point(41.1500, -8.6000));
        closeA.setZona(aeroporto.getZona());

        Paragem closeB = new Paragem();
        closeB.setId(9002L);
        closeB.setNome("Close B");
        closeB.setLocalizacao(new Point(41.1520, -8.6000)); // ~222 meters apart
        closeB.setZona(aeroporto.getZona());

        lenient().when(paragemRepository.findAll()).thenReturn(List.of(closeA, closeB));
        lenient().when(horarioRepository.findByServicoNome("UTEIS")).thenReturn(List.of());

        RoutingIndexService.ServiceRoutingIndex index = indexService.getRoutingIndex("UTEIS");
        List<RotaDTO> routes = searchEngine.search(9001L, 9002L, 8 * 60, index);

        assertFalse(routes.isEmpty(), "Direct walk under 800m should be accepted");
        RotaDTO walkRoute = routes.get(0);
        assertTrue(walkRoute.isCaminho());
        assertTrue(walkRoute.getTotalCaminhadaMinutos() > 0);
    }

    @Test
    void testWalkingPolicySameStopGroupInterchange() {
        // Two stops with the same name group (e.g. "Aliados I" and "Aliados II")
        Paragem aliadosI = new Paragem();
        aliadosI.setId(3001L);
        aliadosI.setNome("Aliados I");
        aliadosI.setLocalizacao(new Point(41.1400, -8.6100));
        aliadosI.setZona(aeroporto.getZona());

        Paragem aliadosII = new Paragem();
        aliadosII.setId(3002L);
        aliadosII.setNome("Aliados II");
        
        // Scenario 1: Physical distance is 0
        aliadosII.setLocalizacao(new Point(41.1400, -8.6100));

        assertTrue(walkingPolicy.getDistanceKm(aliadosI, aliadosII) <= 0.005);
        assertFalse(walkingPolicy.shouldShowWalkSegment(aliadosI, aliadosII), "Interchange with 0m distance should not show walk segment");

        // Scenario 2: Physical distance is 100m
        aliadosII.setLocalizacao(new Point(41.1409, -8.6100)); // ~100m distance
        assertTrue(walkingPolicy.getDistanceKm(aliadosI, aliadosII) > 0.005);
        assertTrue(walkingPolicy.shouldShowWalkSegment(aliadosI, aliadosII), "Interchange with real distance should show walk segment");
    }

    @Test
    void testScheduleQueries() {
        Horario h = new Horario();
        h.setId(10L);
        h.setHora(LocalTime.of(8, 30));
        h.setGtfsTripId("TRIP604");
        h.setServico(servicoUteis);
        h.setPontoPassagem(t604.getPontosDePassagem().get(0));

        when(horarioRepository.findByTrajetoParagemAndServico(100L, 95L, "UTEIS"))
                .thenReturn(List.of(h));
        when(trajetoRepository.existsById(100L)).thenReturn(true);
        when(paragemRepository.existsById(95L)).thenReturn(true);

        List<ProximoPasseDTO> nextPasses = queryService.findProximosPasses(100L, 95L, LocalTime.of(8, 10), DayOfWeek.MONDAY);
        assertEquals(1, nextPasses.size());
        assertEquals("08:30", nextPasses.get(0).getHora());
        assertEquals(20, nextPasses.get(0).getEsperaMinutos());
    }
}
