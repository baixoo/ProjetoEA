package pt.notub.network.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.repository.HorarioRepository;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.PontosDePassagemRepository;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.trip.repository.ViagemVeiculoRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RoutePlanningServiceTest {

    @Mock
    private PontosDePassagemRepository pontosRepository;

    @Mock
    private TrajetoRepository trajetoRepository;

    @Mock
    private HorarioRepository horarioRepository;

    @Mock
    private ParagemRepository paragemRepository;

    @Mock
    private ViagemVeiculoRepository viagemVeiculoRepository;

    private RoutePlanningService service;

    @BeforeEach
    void setUp() {
        lenient().when(pontosRepository.findAll()).thenReturn(List.of());
        lenient().when(trajetoRepository.findAll()).thenReturn(List.of());
        lenient().when(horarioRepository.findAll()).thenReturn(List.of());
        lenient().when(paragemRepository.findAll()).thenReturn(List.of());

        RoutingIndexService indexService = new RoutingIndexService(
                horarioRepository, paragemRepository, trajetoRepository, pontosRepository);
        WalkingTransferPolicy walkingPolicy = new WalkingTransferPolicy();
        RouteDtoAssembler assembler = new RouteDtoAssembler(walkingPolicy);
        RouteSearchEngine searchEngine = new RouteSearchEngine(walkingPolicy, assembler);
        ScheduleQueryService queryService = new ScheduleQueryService(
                horarioRepository, paragemRepository, trajetoRepository, pontosRepository, viagemVeiculoRepository);

        service = new RoutePlanningService(indexService, searchEngine, queryService);
    }

    @Test
    void planearRota_sameOriginAndDestination_returnsEmptyList() {
        assertTrue(service.planearRota(1L, 1L).isEmpty());
    }

    @Test
    void planearRota_invalidTime_throwsPedidoInvalidoException() {
        assertThrows(PedidoInvalidoException.class,
                () -> service.planearRota(1L, 2L, "25:99", "UTEIS"));
    }

    @Test
    void findProximasPassagensPorParagem_missingParagem_throwsNotFound() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.findProximasPassagensPorParagem(99L, "08:00", "MONDAY"));
    }

    @Test
    void normalizeStopGroupKey_ignoresGeneratedSuffixAndPunctuation() {
        assertEquals(
                RoutePlanningService.normalizeStopGroupKey("AV.ALIADOS I"),
                RoutePlanningService.normalizeStopGroupKey("Av. Aliados II"));
        assertEquals(
                RoutePlanningService.normalizeStopGroupKey("S.BENTO"),
                RoutePlanningService.normalizeStopGroupKey("S. Bento"));
    }
}
