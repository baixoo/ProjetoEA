package pt.notub.trip.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.driver.service.NotificacaoValidacaoService;
import pt.notub.network.entity.Horario;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.HorarioRepository;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.points.service.ServicoPontos;
import pt.notub.ticket.repository.TituloTransporteRepository;
import pt.notub.trip.dto.ParagemAtualDTO;
import pt.notub.trip.dto.ZonaMinMaxDTO;
import pt.notub.trip.entity.Viagem;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.repository.ViagemUtilizadorRepository;
import pt.notub.trip.repository.ViagemVeiculoRepository;
import pt.notub.trip.repository.ViagemRepository;
import pt.notub.validation.service.GestorValidacao;
import pt.notub.vehicle.repository.VeiculoRepository;
import pt.notub.zone.entity.Zona;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemServiceTest {

    @Mock
    private ViagemUtilizadorRepository viagemUtilizadorRepository;

    @Mock
    private ViagemVeiculoRepository viagemVeiculoRepository;

    @Mock
    private ParagemRepository paragemRepository;

    @Mock
    private HorarioRepository horarioRepository;

    @Mock
    private TituloTransporteRepository tituloTransporteRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private TrajetoRepository trajetoRepository;

    @Mock
    private ViagemRepository viagemRepository;

    @Mock
    private ServicoPontos servicoPontos;

    @Mock
    private GestorValidacao gestorValidacao;

    @Mock
    private NotificacaoValidacaoService notificacaoService;

    private ViagemService service;

    @BeforeEach
    void setUp() {
        service = new ViagemService(
                viagemUtilizadorRepository,
                viagemVeiculoRepository,
                paragemRepository,
                horarioRepository,
                tituloTransporteRepository,
                veiculoRepository,
                trajetoRepository,
                viagemRepository,
                servicoPontos,
                gestorValidacao,
                notificacaoService);
    }

    @Test
    void getParagemAtual_returnsNearestStopDuringTrip() {
        LocalTime agora = LocalTime.now();
        String serviceId = "UTEIS";
        String tripId = "trip-1";
        ViagemVeiculo viagem = viagemComPontos(
                ponto(1, 10L, 1),
                ponto(2, 20L, 2),
                ponto(3, 30L, 3));
        when(viagemVeiculoRepository.findById(1L)).thenReturn(Optional.of(viagem));

        Trajeto trajeto = viagem.getTrajeto();
        Viagem schedule = new Viagem();
        schedule.setTrajeto(trajeto);
        schedule.setServiceId(serviceId);
        schedule.setHoraPartida(agora.minusMinutes(20));
        schedule.setGtfsTripId(tripId);
        when(viagemRepository.findByTrajetoId(trajeto.getId())).thenReturn(List.of(schedule));
        when(horarioRepository.findByTrajetoAndServico(trajeto.getId(), serviceId)).thenReturn(List.of(
                horario(1, 10L, 1, agora.minusMinutes(20), tripId),
                horario(2, 20L, 2, agora.minusMinutes(1), tripId),
                horario(3, 30L, 3, agora.plusMinutes(20), tripId)
        ));

        ParagemAtualDTO result = service.getParagemAtual(1L);

        assertEquals(20L, result.paragemId());
    }

    @Test
    void getZonaMinMax_usesStopsFromRequestedStopOnward() {
        ViagemVeiculo viagem = viagemComPontos(
                ponto(2, 20L, 3, null),
                ponto(1, 10L, 5, null),
                ponto(3, 30L, 1, null));
        when(viagemVeiculoRepository.findById(1L)).thenReturn(Optional.of(viagem));

        ZonaMinMaxDTO result = service.getZonaMinMax(1L, 20L);

        assertEquals(1, result.zonaMin());
        assertEquals(3, result.zonaMax());
    }

    @Test
    void getZonaMinMax_unknownStop_throwsBadRequestException() {
        ViagemVeiculo viagem = viagemComPontos(
                ponto(1, 10L, 5, null),
                ponto(2, 20L, 3, null));
        when(viagemVeiculoRepository.findById(1L)).thenReturn(Optional.of(viagem));

        assertThrows(PedidoInvalidoException.class, () -> service.getZonaMinMax(1L, 99L));
    }

    private ViagemVeiculo viagemComPontos(PontosDePassagem... pontos) {
        Trajeto trajeto = new Trajeto();
        trajeto.setId(100L);
        trajeto.setPontosDePassagem(List.of(pontos));

        ViagemVeiculo viagem = new ViagemVeiculo();
        viagem.setTrajeto(trajeto);
        return viagem;
    }

    private PontosDePassagem ponto(int ordem, Long paragemId, int zonaNum) {
        Zona zona = new Zona();
        zona.setNum(zonaNum);

        Paragem paragem = new Paragem();
        paragem.setId(paragemId);
        paragem.setZona(zona);

        PontosDePassagem ponto = new PontosDePassagem();
        ponto.setOrdem(ordem);
        ponto.setParagem(paragem);
        return ponto;
    }

    private PontosDePassagem ponto(int ordem, Long paragemId, int zonaNum, LocalTime ignoredHora) {
        return ponto(ordem, paragemId, zonaNum);
    }

    private Horario horario(int ordem, Long paragemId, int zonaNum, LocalTime hora, String tripId) {
        Horario horario = new Horario();
        horario.setId((long) ordem);
        horario.setHora(hora);
        horario.setGtfsTripId(tripId);

        Trajeto trajeto = new Trajeto();
        trajeto.setId(100L);

        Zona zona = new Zona();
        zona.setNum(zonaNum);

        Paragem paragem = new Paragem();
        paragem.setId(paragemId);
        paragem.setZona(zona);

        PontosDePassagem ponto = new PontosDePassagem();
        ponto.setId((long) ordem);
        ponto.setOrdem(ordem);
        ponto.setTrajeto(trajeto);
        ponto.setParagem(paragem);

        horario.setPontoPassagem(ponto);
        return horario;
    }
}
