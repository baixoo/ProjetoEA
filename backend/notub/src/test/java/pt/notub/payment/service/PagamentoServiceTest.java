package pt.notub.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.payment.dto.CheckoutRequest;
import pt.notub.payment.provider.PaymentProcessorFactory;
import pt.notub.payment.repository.TransacaoRepository;
import pt.notub.tariff.repository.TarifaRepository;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private UtilizadorRepository utilizadorRepository;

    @Mock
    private TarifaRepository tarifaRepository;

    @Mock
    private PaymentProcessorFactory processorFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PagamentoService service;

    @BeforeEach
    void setUp() {
        service = new PagamentoService(
                transacaoRepository,
                utilizadorRepository,
                tarifaRepository,
                processorFactory,
                eventPublisher,
                "http://localhost");
    }

    @Test
    void iniciarCheckout_rejectsZoneAboveThree() {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(1L);

        when(utilizadorRepository.findByEmail("user@example.com")).thenReturn(Optional.of(utilizador));

        CheckoutRequest request = new CheckoutRequest();
        request.setTipoProduto("BILHETE");
        request.setZonaId(4L);

        assertThrows(PedidoInvalidoException.class,
                () -> service.iniciarCheckout("user@example.com", request));
    }
}
