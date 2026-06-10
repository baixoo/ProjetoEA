package pt.notub.zone.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.network.entity.Paragem;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.zone.dto.ZonaDTO;
import pt.notub.zone.dto.ZonaRequest;
import pt.notub.zone.entity.Zona;
import pt.notub.zone.repository.ZonaRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZonaServiceTest {

    @Mock
    private ZonaRepository zonaRepository;

    @Mock
    private ParagemRepository paragemRepository;

    private ZonaService service;

    @BeforeEach
    void setUp() {
        service = new ZonaService(zonaRepository, paragemRepository);
    }

    @Test
    void updateZona_updatesNumberAndName() {
        Zona existing = new Zona();
        existing.setId(1L);
        existing.setNome("Old");
        existing.setNum(1);

        ZonaRequest updated = new ZonaRequest("New", 3);

        when(zonaRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(zonaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ZonaDTO result = service.updateZona(1L, updated);

        assertEquals("New", result.getNome());
        assertEquals(3, result.getNum());
        verify(zonaRepository).save(existing);
    }

    @Test
    void getAllZonas_sortsByNumero() {
        Zona zona3 = new Zona();
        zona3.setId(3L);
        zona3.setNum(3);
        zona3.setNome("C");

        Zona zona1 = new Zona();
        zona1.setId(1L);
        zona1.setNum(1);
        zona1.setNome("A");

        when(zonaRepository.findAll()).thenReturn(java.util.List.of(zona3, zona1));

        var result = service.getAllZonas();

        assertEquals(1, result.get(0).getNum());
        assertEquals(3, result.get(1).getNum());
    }

    @Test
    void createZona_rejectsNumeroAboveThree() {
        assertThrows(PedidoInvalidoException.class,
                () -> service.createZona(new ZonaRequest("Zona", 4)));
    }

    @Test
    void addParagem_assignsZona() {
        Zona zona = new Zona();
        zona.setId(1L);
        zona.setNome("Centro");
        zona.setNum(3);

        Paragem paragem = new Paragem();
        paragem.setId(2L);

        when(zonaRepository.findById(1L)).thenReturn(Optional.of(zona));
        when(paragemRepository.findById(2L)).thenReturn(Optional.of(paragem));
        when(paragemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ZonaDTO result = service.addParagem(1L, 2L);

        assertSame(zona, paragem.getZona());
        assertEquals("Centro", result.getNome());
        assertEquals(3, result.getNum());
        verify(paragemRepository).save(paragem);
    }

    @Test
    void removeParagem_clearsZona() {
        Zona zona = new Zona();
        zona.setId(1L);

        Paragem paragem = new Paragem();
        paragem.setId(2L);
        paragem.setZona(zona);

        when(paragemRepository.findById(2L)).thenReturn(Optional.of(paragem));
        when(paragemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.removeParagem(2L);

        assertNull(paragem.getZona());
        verify(paragemRepository).save(paragem);
    }
}
