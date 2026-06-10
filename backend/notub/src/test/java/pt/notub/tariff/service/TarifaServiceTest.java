package pt.notub.tariff.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.tariff.dto.TarifaRequest;
import pt.notub.tariff.repository.TarifaRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TarifaServiceTest {

    @Mock
    private TarifaRepository tarifaRepository;

    private TarifaService service;

    @BeforeEach
    void setUp() {
        service = new TarifaService(tarifaRepository);
    }

    @Test
    void calcularTarifa_rejectsZonaAboveThree() {
        assertThrows(PedidoInvalidoException.class,
                () -> service.calcularTarifa("BILHETE", "ADULTO", null, 4));
    }

    @Test
    void createTarifa_rejectsZonaAboveThree() {
        assertThrows(PedidoInvalidoException.class,
                () -> service.createTarifa(new TarifaRequest(1.0f, "ADULTO", null, 4)));
    }
}
