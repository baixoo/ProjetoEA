package pt.notub.ticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.points.service.ServicoPontos;
import pt.notub.tariff.entity.ModalidadePasse;
import pt.notub.ticket.repository.BilheteRepository;
import pt.notub.ticket.repository.PasseRepository;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;
import pt.notub.zone.entity.Zona;
import pt.notub.zone.repository.ZonaRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private BilheteRepository bilheteRepository;

    @Mock
    private PasseRepository passeRepository;

    @Mock
    private UtilizadorRepository utilizadorRepository;

    @Mock
    private ZonaRepository zonaRepository;

    @Mock
    private ServicoPontos servicoPontos;

    private TicketService service;

    @BeforeEach
    void setUp() {
        service = new TicketService(
                bilheteRepository,
                passeRepository,
                utilizadorRepository,
                zonaRepository,
                servicoPontos);
    }

    @Test
    void buyTickets_rejectsZoneWithNumberAboveThree() {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(1L);

        Zona zona = new Zona();
        zona.setId(4L);
        zona.setNum(4);

        when(utilizadorRepository.findByEmail("user@example.com")).thenReturn(Optional.of(utilizador));
        when(zonaRepository.findById(4L)).thenReturn(Optional.of(zona));

        assertThrows(PedidoInvalidoException.class,
                () -> service.buyTickets("user@example.com", 1, 4L));
    }

    @Test
    void buyPasse_rejectsZoneWithNumberAboveThree() {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(1L);

        Zona zona = new Zona();
        zona.setId(4L);
        zona.setNum(4);

        when(utilizadorRepository.findByEmail("user@example.com")).thenReturn(Optional.of(utilizador));
        when(zonaRepository.findById(4L)).thenReturn(Optional.of(zona));

        assertThrows(PedidoInvalidoException.class,
                () -> service.buyPasse("user@example.com", ModalidadePasse.H24, 4L));
    }
}
