package pt.notub.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.admin.dto.AdminStatsResponse;
import pt.notub.payment.repository.TransacaoRepository;
import pt.notub.ticket.repository.BilheteRepository;
import pt.notub.ticket.repository.PasseRepository;
import pt.notub.trip.entity.EstadoViagem;
import pt.notub.trip.repository.ViagemUtilizadorRepository;
import pt.notub.user.repository.UtilizadorRepository;
import pt.notub.vehicle.repository.VeiculoRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoAdminTest {

    @Mock
    private UtilizadorRepository utilizadorRepository;

    @Mock
    private BilheteRepository bilheteRepository;

    @Mock
    private PasseRepository passeRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ViagemUtilizadorRepository viagemUtilizadorRepository;

    @Test
    void getStats_usesCountQueryForActiveTrips() {
        when(utilizadorRepository.count()).thenReturn(10L);
        when(bilheteRepository.count()).thenReturn(20L);
        when(passeRepository.count()).thenReturn(30L);
        when(transacaoRepository.count()).thenReturn(40L);
        when(veiculoRepository.count()).thenReturn(50L);
        when(viagemUtilizadorRepository.countByEstado(EstadoViagem.ATIVA)).thenReturn(5L);

        ServicoAdmin service = new ServicoAdmin(
                utilizadorRepository, bilheteRepository, passeRepository, transacaoRepository, veiculoRepository, viagemUtilizadorRepository);

        AdminStatsResponse stats = service.getStats();

        assertEquals(10L, stats.utilizadores());
        assertEquals(20L, stats.bilhetes());
        assertEquals(30L, stats.passes());
        assertEquals(40L, stats.transacoes());
        assertEquals(50L, stats.veiculos());
        assertEquals(5L, stats.viagens());
        verify(viagemUtilizadorRepository).countByEstado(EstadoViagem.ATIVA);
        verify(viagemUtilizadorRepository, never()).findByEstado(any());
    }
}
