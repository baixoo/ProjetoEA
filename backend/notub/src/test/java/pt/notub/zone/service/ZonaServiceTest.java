package pt.notub.zone.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.network.entity.Paragem;
import pt.notub.network.repository.ParagemRepository;
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
        existing.setNum(5);

        Zona updated = new Zona();
        updated.setNome("New");
        updated.setNum(7);

        when(zonaRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(zonaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Zona result = service.updateZona(1L, updated);

        assertEquals("New", result.getNome());
        assertEquals(7, result.getNum());
        verify(zonaRepository).save(existing);
    }

    @Test
    void addParagem_assignsZona() {
        Zona zona = new Zona();
        zona.setId(1L);

        Paragem paragem = new Paragem();
        paragem.setId(2L);

        when(zonaRepository.findById(1L)).thenReturn(Optional.of(zona));
        when(paragemRepository.findById(2L)).thenReturn(Optional.of(paragem));
        when(paragemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Zona result = service.addParagem(1L, 2L);

        assertSame(zona, result);
        assertSame(zona, paragem.getZona());
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
