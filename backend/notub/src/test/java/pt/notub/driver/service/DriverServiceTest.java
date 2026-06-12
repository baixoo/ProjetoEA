package pt.notub.driver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.ConflitoException;
import pt.notub.driver.dto.DriverTrajetoDTO;
import pt.notub.driver.dto.StartViagemRequest;
import pt.notub.driver.dto.ViagemScheduleDTO;
import pt.notub.driver.dto.AvancarViagemResponseDTO;
import pt.notub.network.entity.Direcao;
import pt.notub.network.entity.Linha;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.entity.Horario;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.network.repository.HorarioRepository;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.repository.ViagemVeiculoRepository;
import pt.notub.vehicle.dto.VeiculoDTO;
import pt.notub.vehicle.entity.TipoVeiculo;
import pt.notub.vehicle.entity.Veiculo;
import pt.notub.vehicle.repository.VeiculoRepository;

import pt.notub.trip.service.ViagemService;


import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ViagemVeiculoRepository viagemVeiculoRepository;

    @Mock
    private TrajetoRepository trajetoRepository;

    @Mock
    private HorarioRepository horarioRepository;

    @Mock
    private ViagemService viagemService;

    private DriverService service;

    @BeforeEach
    void setUp() {
        service = new DriverService(
            veiculoRepository, 
            viagemVeiculoRepository, 
            trajetoRepository, 
            horarioRepository,
            viagemService
        );
    }

    @Test
    void getVeiculosComLinha_usesDerivedQuery() {
        Linha linha = new Linha();
        linha.setId(1L);
        linha.setNome("Linha 1");

        Veiculo veiculo = new Veiculo();
        veiculo.setId(10L);
        veiculo.setMatricula("AA-11-BB");
        veiculo.setLinha(linha);
        veiculo.setTipo(TipoVeiculo.AUTOCARRO);

        when(veiculoRepository.findByLinhaIsNotNull()).thenReturn(List.of(veiculo));

        List<VeiculoDTO> result = service.getVeiculosComLinha();

        verify(veiculoRepository).findByLinhaIsNotNull();
        verify(veiculoRepository, never()).findAll();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getLinhaId());
        assertEquals("Linha 1", result.get(0).getLinhaNome());
    }

    @Test
    void getTrajetos_withLineId_usesRepositoryQuery() {
        Linha linha = new Linha();
        linha.setId(2L);
        linha.setNome("Linha 2");

        Paragem primeira = new Paragem();
        primeira.setNome("Stop A");
        Paragem ultima = new Paragem();
        ultima.setNome("Stop B");

        PontosDePassagem ponto1 = new PontosDePassagem();
        ponto1.setOrdem(1);
        ponto1.setParagem(primeira);
        PontosDePassagem ponto2 = new PontosDePassagem();
        ponto2.setOrdem(2);
        ponto2.setParagem(ultima);

        Trajeto trajeto = new Trajeto();
        trajeto.setId(5L);
        trajeto.setLinha(linha);
        trajeto.setDirecao(Direcao.IDA);
        trajeto.setPontosDePassagem(List.of(ponto1, ponto2));

        when(trajetoRepository.findByLinhaId(2L)).thenReturn(List.of(trajeto));

        List<DriverTrajetoDTO> result = service.getTrajetos(2L);

        verify(trajetoRepository).findByLinhaId(2L);
        verify(trajetoRepository, never()).findAll();
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).id());
        assertEquals("Linha 2", result.get(0).linha());
        assertEquals("IDA", result.get(0).direcao());
        assertEquals("Stop A", result.get(0).primeiraParagem());
        assertEquals("Stop B", result.get(0).ultimaParagem());
    }

    @Test
    void startViagem_rejectsMismatchingLine() {
        Linha l1 = new Linha(); l1.setId(1L);
        Linha l2 = new Linha(); l2.setId(2L);

        Veiculo v = new Veiculo();
        v.setId(10L);
        v.setLinha(l1);
        v.setTipo(TipoVeiculo.AUTOCARRO);

        Trajeto t = new Trajeto();
        t.setId(20L);
        t.setLinha(l2);

        when(veiculoRepository.findById(10L)).thenReturn(Optional.of(v));
        when(trajetoRepository.findById(20L)).thenReturn(Optional.of(t));

        StartViagemRequest req = new StartViagemRequest(10L, 20L, "UTEIS", "GTFS-TRIP-123");

        assertThrows(PedidoInvalidoException.class, () -> service.startViagem(req));
    }

    @Test
    void startViagem_correctlySetsPontoAtualAndDelay() {
        Linha l1 = new Linha(); l1.setId(1L);

        Veiculo v = new Veiculo();
        v.setId(10L);
        v.setLinha(l1);
        v.setTipo(TipoVeiculo.AUTOCARRO);

        Paragem p1 = new Paragem(); p1.setId(100L); p1.setNome("Start");
        PontosDePassagem pp1 = new PontosDePassagem();
        pp1.setId(50L);
        pp1.setOrdem(1);
        pp1.setParagem(p1);

        Trajeto t = new Trajeto();
        t.setId(20L);
        t.setLinha(l1);
        t.setPontosDePassagem(List.of(pp1));

        // Viagem departure scheduled for 10 minutes ago
        LocalTime scheduledTime = LocalTime.now().minusMinutes(10);
        pt.notub.network.entity.Servico servico = new pt.notub.network.entity.Servico();
        servico.setNome("UTEIS");
        Horario h1 = new Horario();
        h1.setId(1000L);
        h1.setPontoPassagem(pp1);
        h1.setGtfsTripId("GTFS-TRIP-123");
        h1.setServico(servico);
        h1.setHora(scheduledTime);

        when(veiculoRepository.findById(10L)).thenReturn(Optional.of(v));
        when(trajetoRepository.findById(20L)).thenReturn(Optional.of(t));
        when(horarioRepository.findByPontoPassagemIdAndGtfsTripId(50L, "GTFS-TRIP-123"))
                .thenReturn(Optional.of(h1));
        when(viagemVeiculoRepository.findActiveByVeiculoId(10L)).thenReturn(List.of());

        ViagemVeiculo savedEntity = new ViagemVeiculo();
        savedEntity.setId(500L);
        savedEntity.setVeiculo(v);
        savedEntity.setTrajeto(t);
        savedEntity.setServiceId("UTEIS");
        savedEntity.setGtfsTripId("GTFS-TRIP-123");
        savedEntity.setHoraPartidaPlaneada(scheduledTime);
        savedEntity.setPontoAtual(pp1);
        savedEntity.setStartTime(LocalDateTime.now());

        when(viagemVeiculoRepository.save(any(ViagemVeiculo.class))).thenReturn(savedEntity);

        StartViagemRequest req = new StartViagemRequest(10L, 20L, "UTEIS", "GTFS-TRIP-123");
        ViagemVeiculoDTO result = service.startViagem(req);

        assertNotNull(result);
        assertEquals(500L, result.getId());
        verify(veiculoRepository).save(v);
        assertTrue(v.getTempoAtraso() >= 10);
    }

    @Test
    void avancarViagem_progressesToNextStopAndUpdatesDelay() {
        Veiculo v = new Veiculo();
        v.setId(10L);
        v.setTempoAtraso(0);
        v.setTipo(TipoVeiculo.AUTOCARRO);

        Paragem p1 = new Paragem(); p1.setId(100L); p1.setNome("Start");
        PontosDePassagem pp1 = new PontosDePassagem();
        pp1.setId(50L);
        pp1.setOrdem(1);
        pp1.setParagem(p1);

        Paragem p2 = new Paragem(); p2.setId(200L); p2.setNome("End");
        PontosDePassagem pp2 = new PontosDePassagem();
        pp2.setId(60L);
        pp2.setOrdem(2);
        pp2.setParagem(p2);

        Trajeto t = new Trajeto();
        t.setId(20L);
        t.setPontosDePassagem(List.of(pp1, pp2));

        ViagemVeiculo vv = new ViagemVeiculo();
        vv.setId(500L);
        vv.setVeiculo(v);
        vv.setTrajeto(t);
        vv.setGtfsTripId("GTFS-TRIP-123");
        vv.setServiceId("UTEIS");
        vv.setPontoAtual(pp1);
        vv.setStartTime(LocalDateTime.now());

        when(viagemVeiculoRepository.findById(500L)).thenReturn(Optional.of(vv));

        // Schedule at stop 2 is 5 minutes ago
        Horario hor = new Horario();
        hor.setHora(LocalTime.now().minusMinutes(5));
        when(horarioRepository.findByPontoPassagemIdAndGtfsTripId(60L, "GTFS-TRIP-123"))
                .thenReturn(Optional.of(hor));

        AvancarViagemResponseDTO res = service.avancarViagem(500L);

        assertNotNull(res);
        assertEquals(60L, res.pontoAtualId());
        assertEquals("End", res.pontoAtualNome());
        assertTrue(res.isFinal());
        assertTrue(res.tempoAtraso() >= 5);
        assertEquals(pp2, vv.getPontoAtual());
        verify(veiculoRepository).save(v);
    }

    @Test
    void endViagem_updatesFinishTimeAndResetsDelay() {
        Veiculo v = new Veiculo();
        v.setId(10L);
        v.setTempoAtraso(12);
        v.setLotacaoAtual(5);
        v.setTipo(TipoVeiculo.AUTOCARRO);

        ViagemVeiculo vv = new ViagemVeiculo();
        vv.setId(500L);
        vv.setVeiculo(v);

        when(viagemVeiculoRepository.findById(500L)).thenReturn(Optional.of(vv));

        service.endViagem(500L);

        assertNotNull(vv.getFinishTime());
        assertEquals(0, v.getTempoAtraso());
        assertEquals(0, v.getLotacaoAtual());
        verify(viagemVeiculoRepository).save(vv);
        verify(veiculoRepository).save(v);
    }
}
