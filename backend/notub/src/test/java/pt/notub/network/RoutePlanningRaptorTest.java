package pt.notub.network;

import org.junit.jupiter.api.Test;
import pt.notub.models.Direcao;
import pt.notub.models.Linha;
import pt.notub.models.Paragem;
import pt.notub.models.Point;
import pt.notub.models.PontosDePassagem;
import pt.notub.models.Trajeto;
import pt.notub.models.Viagem;
import pt.notub.network.dto.RotaDTO;
import pt.notub.repositories.PontosDePassagemRepository;
import pt.notub.repositories.TrajetoRepository;
import pt.notub.repositories.ViagemRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutePlanningRaptorTest {

    @Test
    void testBoardsEarlierTripAtLaterStopInSameRouteScan() {
        Linha linha = mkLinha(1L, "L1");

        Paragem hubA = mkParagem(1L, "HUB", 41.0000, -8.0000);
        Paragem hubB = mkParagem(2L, "HUB", 41.0010, -8.0000);
        Paragem destino = mkParagem(3L, "DEST", 41.0200, -8.0000);

        Trajeto trajeto = mkTrajeto(10L, Direcao.IDA, linha);
        List<PontosDePassagem> pontos = List.of(
                mkPonto(100L, 0, 0, trajeto, hubA),
                mkPonto(101L, 1, 10, trajeto, hubB),
                mkPonto(102L, 2, 20, trajeto, destino)
        );
        trajeto.setPontosDePassagem(pontos);

        RoutePlanningService service = buildService(
                List.of(trajeto),
                pontos,
                List.of(
                        mkViagem(1000L, trajeto, "UTEIS", LocalTime.of(8, 5)),
                        mkViagem(1001L, trajeto, "UTEIS", LocalTime.of(8, 15))
                )
        );

        List<RotaDTO> rotas = service.planearRota(hubA.getId(), destino.getId(), LocalTime.of(8, 9), DayOfWeek.MONDAY);

        assertFalse(rotas.isEmpty());
        RotaDTO rota = rotas.get(0);
        assertEquals("08:25", rota.getHoraChegada());
        assertEquals(hubB.getId(), rota.getSegmentos().get(0).getOrigem().getId());
    }

    @Test
    void testWalkTransferFeedsTheNextRound() {
        Linha linha1 = mkLinha(1L, "L1");
        Linha linha2 = mkLinha(2L, "L2");

        Paragem origem = mkParagem(10L, "ORIGIN", 41.0000, -8.0000);
        Paragem transferA = mkParagem(11L, "TRANSFER_A", 41.0040, -8.0000);
        Paragem transferB = mkParagem(12L, "TRANSFER_B", 41.0080, -8.0000);
        Paragem destino = mkParagem(13L, "DEST", 41.0200, -8.0000);

        Trajeto primeiro = mkTrajeto(20L, Direcao.IDA, linha1);
        List<PontosDePassagem> pontosPrimeiro = List.of(
                mkPonto(200L, 0, 0, primeiro, origem),
                mkPonto(201L, 1, 10, primeiro, transferA)
        );
        primeiro.setPontosDePassagem(pontosPrimeiro);

        Trajeto segundo = mkTrajeto(21L, Direcao.IDA, linha2);
        List<PontosDePassagem> pontosSegundo = List.of(
                mkPonto(210L, 0, 0, segundo, transferB),
                mkPonto(211L, 1, 10, segundo, destino)
        );
        segundo.setPontosDePassagem(pontosSegundo);

        List<PontosDePassagem> todosOsPontos = new ArrayList<>();
        todosOsPontos.addAll(pontosPrimeiro);
        todosOsPontos.addAll(pontosSegundo);

        RoutePlanningService service = buildService(
                List.of(primeiro, segundo),
                todosOsPontos,
                List.of(
                        mkViagem(2000L, primeiro, "UTEIS", LocalTime.of(8, 0)),
                        mkViagem(2001L, segundo, "UTEIS", LocalTime.of(8, 21))
                )
        );

        List<RotaDTO> rotas = service.planearRota(origem.getId(), destino.getId(), LocalTime.of(8, 0), DayOfWeek.MONDAY);

        assertFalse(rotas.isEmpty());
        RotaDTO rota = rotas.get(0);
        assertEquals(1, rota.getTrocas());
        assertEquals("08:31", rota.getHoraChegada());
        assertEquals(3, rota.getSegmentos().size());
        assertEquals("A pe", rota.getSegmentos().get(1).getLinhaNome());
        assertTrue(rota.getSegmentos().stream().anyMatch(seg -> seg.getTrajetoId() != null && seg.getTrajetoId().equals(segundo.getId())));
    }

    private static RoutePlanningService buildService(List<Trajeto> trajetos,
                                                     List<PontosDePassagem> pontos,
                                                     List<Viagem> viagens) {
        PontosDePassagemRepository pontosRepo = mock(PontosDePassagemRepository.class);
        TrajetoRepository trajetoRepo = mock(TrajetoRepository.class);
        ViagemRepository viagemRepo = mock(ViagemRepository.class);

        when(pontosRepo.findAll()).thenReturn(pontos);
        when(trajetoRepo.findAll()).thenReturn(trajetos);
        when(viagemRepo.findAll()).thenReturn(viagens);

        return new RoutePlanningService(pontosRepo, trajetoRepo, viagemRepo);
    }

    private static Linha mkLinha(long id, String nome) {
        Linha linha = new Linha();
        linha.setId(id);
        linha.setNome(nome);
        return linha;
    }

    private static Paragem mkParagem(long id, String nome, double lat, double lon) {
        Paragem paragem = new Paragem();
        paragem.setId(id);
        paragem.setNome(nome);
        paragem.setLocalizacao(new Point(lat, lon));
        return paragem;
    }

    private static Trajeto mkTrajeto(long id, Direcao direcao, Linha linha) {
        Trajeto trajeto = new Trajeto();
        trajeto.setId(id);
        trajeto.setDirecao(direcao);
        trajeto.setLinha(linha);
        return trajeto;
    }

    private static PontosDePassagem mkPonto(long id, int ordem, int tempo, Trajeto trajeto, Paragem paragem) {
        PontosDePassagem ponto = new PontosDePassagem();
        ponto.setId(id);
        ponto.setOrdem(ordem);
        ponto.setTempoDesdeInicio(tempo);
        ponto.setParagem(paragem);
        return ponto;
    }

    private static Viagem mkViagem(long id, Trajeto trajeto, String serviceId, LocalTime horaPartida) {
        Viagem viagem = new Viagem();
        viagem.setId(id);
        viagem.setTrajeto(trajeto);
        viagem.setServiceId(serviceId);
        viagem.setHoraPartida(horaPartida);
        return viagem;
    }
}
