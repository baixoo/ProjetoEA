package pt.notub.network.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.network.dto.ProximoPasseDTO;
import pt.notub.network.entity.*;
import pt.notub.network.repository.HorarioRepository;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.PontosDePassagemRepository;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.repository.ViagemVeiculoRepository;
import pt.notub.vehicle.entity.Veiculo;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ScheduleQueryServiceTest {

    @Mock
    private HorarioRepository horarioRepo;
    @Mock
    private ParagemRepository paragemRepo;
    @Mock
    private TrajetoRepository trajetoRepo;
    @Mock
    private PontosDePassagemRepository pontosRepo;
    @Mock
    private ViagemVeiculoRepository viagemVeiculoRepo;

    private ScheduleQueryService queryService;

    private Trajeto trajeto;
    private Paragem paragem;
    private Servico servico;
    private PontosDePassagem pontoPassagem;

    @BeforeEach
    void setUp() {
        queryService = new ScheduleQueryService(horarioRepo, paragemRepo, trajetoRepo, pontosRepo, viagemVeiculoRepo);

        trajeto = new Trajeto();
        trajeto.setId(1L);

        paragem = new Paragem();
        paragem.setId(2L);
        paragem.setNome("Stop A");

        servico = new Servico();
        servico.setNome("UTEIS");

        pontoPassagem = new PontosDePassagem();
        pontoPassagem.setId(10L);
        pontoPassagem.setTrajeto(trajeto);
        pontoPassagem.setParagem(paragem);
        pontoPassagem.setOrdem(3);

        when(trajetoRepo.existsById(1L)).thenReturn(true);
        when(paragemRepo.existsById(2L)).thenReturn(true);
    }

    @Test
    void testPassagemSemViagemAtivaMantemComportamentoAnterior() {
        Horario h = new Horario();
        h.setId(100L);
        h.setHora(LocalTime.of(12, 0));
        h.setGtfsTripId("TRIP-123");
        h.setServico(servico);
        h.setPontoPassagem(pontoPassagem);

        when(horarioRepo.findByTrajetoParagemAndServico(1L, 2L, "UTEIS")).thenReturn(List.of(h));
        when(viagemVeiculoRepo.findViagensIniciadasHoje(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        List<ProximoPasseDTO> res = queryService.findProximosPasses(1L, 2L, LocalTime.of(11, 45), DayOfWeek.MONDAY);

        assertEquals(1, res.size());
        ProximoPasseDTO p = res.get(0);
        assertEquals("12:00", p.getHora());
        assertEquals(15, p.getEsperaMinutos());
        assertNull(p.getHoraPrevista());
        assertNull(p.getLotacaoAtual());
        assertNull(p.getnLugares());
        assertNull(p.getTempoAtraso());
    }

    @Test
    void testViagemAtivaCorrespondenteDevolveDadosTempoReal() {
        Horario h = new Horario();
        h.setId(100L);
        h.setHora(LocalTime.of(12, 0));
        h.setGtfsTripId("TRIP-123");
        h.setServico(servico);
        h.setPontoPassagem(pontoPassagem);

        Veiculo v = new Veiculo();
        v.setId(10L);
        v.setLotacaoAtual(15);
        v.setnLugares(40);
        v.setTempoAtraso(5);

        ViagemVeiculo vv = new ViagemVeiculo();
        vv.setTrajeto(trajeto);
        vv.setGtfsTripId("TRIP-123");
        vv.setServiceId("UTEIS");
        vv.setVeiculo(v);
        PontosDePassagem pontoAtual = new PontosDePassagem();
        pontoAtual.setOrdem(1);
        vv.setPontoAtual(pontoAtual);

        when(horarioRepo.findByTrajetoParagemAndServico(1L, 2L, "UTEIS")).thenReturn(List.of(h));
        when(viagemVeiculoRepo.findViagensIniciadasHoje(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(vv));

        List<ProximoPasseDTO> res = queryService.findProximosPasses(1L, 2L, LocalTime.of(11, 45), DayOfWeek.MONDAY);

        assertEquals(1, res.size());
        ProximoPasseDTO p = res.get(0);
        assertEquals("12:00", p.getHora());
        assertEquals("12:05", p.getHoraPrevista());
        assertEquals(20, p.getEsperaMinutos());
        assertEquals(15, p.getLotacaoAtual());
        assertEquals(40, p.getnLugares());
        assertEquals(5, p.getTempoAtraso());
    }

    @Test
    void testViagensDeOutrosTrajetosOuServicosNaoContaminam() {
        Horario h = new Horario();
        h.setId(100L);
        h.setHora(LocalTime.of(12, 0));
        h.setGtfsTripId("TRIP-123");
        h.setServico(servico);
        h.setPontoPassagem(pontoPassagem);

        Veiculo v = new Veiculo();
        v.setId(10L);
        v.setLotacaoAtual(15);
        v.setnLugares(40);
        v.setTempoAtraso(5);

        ViagemVeiculo vvDiffTrip = new ViagemVeiculo();
        vvDiffTrip.setTrajeto(trajeto);
        vvDiffTrip.setGtfsTripId("TRIP-OTHER");
        vvDiffTrip.setServiceId("UTEIS");
        vvDiffTrip.setVeiculo(v);

        Trajeto otherTrajeto = new Trajeto();
        otherTrajeto.setId(99L);
        ViagemVeiculo vvDiffTrajeto = new ViagemVeiculo();
        vvDiffTrajeto.setTrajeto(otherTrajeto);
        vvDiffTrajeto.setGtfsTripId("TRIP-123");
        vvDiffTrajeto.setServiceId("UTEIS");
        vvDiffTrajeto.setVeiculo(v);

        ViagemVeiculo vvDiffService = new ViagemVeiculo();
        vvDiffService.setTrajeto(trajeto);
        vvDiffService.setGtfsTripId("TRIP-123");
        vvDiffService.setServiceId("SAB");
        vvDiffService.setVeiculo(v);

        when(horarioRepo.findByTrajetoParagemAndServico(1L, 2L, "UTEIS")).thenReturn(List.of(h));
        when(viagemVeiculoRepo.findViagensIniciadasHoje(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(vvDiffTrip, vvDiffTrajeto, vvDiffService));

        List<ProximoPasseDTO> res = queryService.findProximosPasses(1L, 2L, LocalTime.of(11, 45), DayOfWeek.MONDAY);

        assertEquals(1, res.size());
        ProximoPasseDTO p = res.get(0);
        assertEquals("12:00", p.getHora());
        assertNull(p.getHoraPrevista());
        assertNull(p.getLotacaoAtual());
        assertNull(p.getnLugares());
        assertNull(p.getTempoAtraso());
    }

    @Test
    void testVeiculoQueJaPassouPelaParagemDeixaDeAparecer() {
        Horario h = new Horario();
        h.setId(100L);
        h.setHora(LocalTime.of(12, 0));
        h.setGtfsTripId("TRIP-123");
        h.setServico(servico);
        h.setPontoPassagem(pontoPassagem);

        Veiculo v = new Veiculo();
        v.setId(10L);
        v.setLotacaoAtual(15);
        v.setnLugares(40);
        v.setTempoAtraso(5);

        ViagemVeiculo vv = new ViagemVeiculo();
        vv.setTrajeto(trajeto);
        vv.setGtfsTripId("TRIP-123");
        vv.setServiceId("UTEIS");
        vv.setVeiculo(v);
        PontosDePassagem pontoAtual = new PontosDePassagem();
        pontoAtual.setOrdem(3);
        vv.setPontoAtual(pontoAtual);

        when(horarioRepo.findByTrajetoParagemAndServico(1L, 2L, "UTEIS")).thenReturn(List.of(h));
        when(viagemVeiculoRepo.findViagensIniciadasHoje(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(vv));

        List<ProximoPasseDTO> res = queryService.findProximosPasses(1L, 2L, LocalTime.of(11, 45), DayOfWeek.MONDAY);

        assertTrue(res.isEmpty());
    }

    @Test
    void testAtrasoZeroAHorasAtrasoPositivoAlteraHoraEEspera() {
        Horario h1 = new Horario();
        h1.setId(101L);
        h1.setHora(LocalTime.of(12, 0));
        h1.setGtfsTripId("TRIP-A");
        h1.setServico(servico);
        h1.setPontoPassagem(pontoPassagem);

        Horario h2 = new Horario();
        h2.setId(102L);
        h2.setHora(LocalTime.of(12, 10));
        h2.setGtfsTripId("TRIP-B");
        h2.setServico(servico);
        h2.setPontoPassagem(pontoPassagem);

        Veiculo vA = new Veiculo();
        vA.setId(11L);
        vA.setTempoAtraso(0);

        ViagemVeiculo vvA = new ViagemVeiculo();
        vvA.setTrajeto(trajeto);
        vvA.setGtfsTripId("TRIP-A");
        vvA.setServiceId("UTEIS");
        vvA.setVeiculo(vA);

        Veiculo vB = new Veiculo();
        vB.setId(12L);
        vB.setTempoAtraso(5);

        ViagemVeiculo vvB = new ViagemVeiculo();
        vvB.setTrajeto(trajeto);
        vvB.setGtfsTripId("TRIP-B");
        vvB.setServiceId("UTEIS");
        vvB.setVeiculo(vB);

        when(horarioRepo.findByTrajetoParagemAndServico(1L, 2L, "UTEIS")).thenReturn(List.of(h1, h2));
        when(viagemVeiculoRepo.findViagensIniciadasHoje(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(vvA, vvB));

        List<ProximoPasseDTO> res = queryService.findProximosPasses(1L, 2L, LocalTime.of(11, 55), DayOfWeek.MONDAY);

        assertEquals(2, res.size());

        ProximoPasseDTO pA = res.stream().filter(x -> x.getHora().equals("12:00")).findFirst().orElseThrow();
        assertEquals("12:00", pA.getHoraPrevista());
        assertEquals(5, pA.getEsperaMinutos());
        assertEquals(0, pA.getTempoAtraso());

        ProximoPasseDTO pB = res.stream().filter(x -> x.getHora().equals("12:10")).findFirst().orElseThrow();
        assertEquals("12:15", pB.getHoraPrevista());
        assertEquals(20, pB.getEsperaMinutos());
        assertEquals(5, pB.getTempoAtraso());
    }

    @Test
    void testViagemTerminadaHojeDeixaDeAparecer() {
        Horario h = new Horario();
        h.setId(100L);
        h.setHora(LocalTime.of(12, 0));
        h.setGtfsTripId("TRIP-123");
        h.setServico(servico);
        h.setPontoPassagem(pontoPassagem);

        Veiculo v = new Veiculo();
        v.setId(10L);
        v.setLotacaoAtual(15);
        v.setnLugares(40);
        v.setTempoAtraso(5);

        ViagemVeiculo vv = new ViagemVeiculo();
        vv.setTrajeto(trajeto);
        vv.setGtfsTripId("TRIP-123");
        vv.setServiceId("UTEIS");
        vv.setVeiculo(v);
        vv.setFinishTime(LocalDateTime.now()); // Finished!
        PontosDePassagem pontoAtual = new PontosDePassagem();
        pontoAtual.setOrdem(1);
        vv.setPontoAtual(pontoAtual);

        when(horarioRepo.findByTrajetoParagemAndServico(1L, 2L, "UTEIS")).thenReturn(List.of(h));
        when(viagemVeiculoRepo.findViagensIniciadasHoje(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(vv));

        List<ProximoPasseDTO> res = queryService.findProximosPasses(1L, 2L, LocalTime.of(11, 45), DayOfWeek.MONDAY);

        assertTrue(res.isEmpty());
    }
}
