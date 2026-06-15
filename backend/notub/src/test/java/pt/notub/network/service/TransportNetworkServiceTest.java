package pt.notub.network.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.network.dto.LinhaDTO;
import pt.notub.network.dto.LinhaRequest;
import pt.notub.network.dto.LinhaSummaryDTO;
import pt.notub.network.entity.Linha;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.repository.LinhaRepository;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.PontosDePassagemRepository;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.zone.repository.ZonaRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransportNetworkServiceTest {

    @Mock
    private ParagemRepository paragemRepository;

    @Mock
    private LinhaRepository linhaRepository;

    @Mock
    private TrajetoRepository trajetoRepository;

    @Mock
    private PontosDePassagemRepository pontosDePassagemRepository;

    @Mock
    private ZonaRepository zonaRepository;

    private TransportNetworkService service;

    @BeforeEach
    void setUp() {
        service = new TransportNetworkService(
                paragemRepository,
                linhaRepository,
                trajetoRepository,
                pontosDePassagemRepository,
                zonaRepository);
    }

    @Test
    void getTrajetosByLinhaMap_usesDerivedQuery() {
        Linha linha = new Linha();
        linha.setId(1L);

        Trajeto trajeto = new Trajeto();
        trajeto.setId(10L);
        trajeto.setLinha(linha);

        when(trajetoRepository.findByLinhaIsNotNull()).thenReturn(List.of(trajeto));

        var result = service.getTrajetosByLinhaMap();

        verify(trajetoRepository).findByLinhaIsNotNull();
        verify(trajetoRepository, never()).findAll();
        assertEquals(1, result.size());
        assertEquals(1, result.get(1L).size());
        assertEquals(10L, result.get(1L).get(0).getId());
    }

    @Test
    void getLinhaSummaries_returnsOnlyEssentialFields() {
        LinhaSummaryDTO summary = new LinhaSummaryDTO(7L, "Linha 7");
        when(linhaRepository.findAllSummaries()).thenReturn(List.of(summary));

        var result = service.getLinhaSummaries();

        verify(linhaRepository).findAllSummaries();
        verify(linhaRepository, never()).findAll();
        verifyNoInteractions(trajetoRepository);
        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getId());
        assertEquals("Linha 7", result.get(0).getNome());
    }

    @Test
    void updateLinha_updatesFields() {
        Linha existing = new Linha();
        existing.setId(5L);
        existing.setNome("Old");
        existing.setIdentificadorServico("A");

        LinhaRequest updated = new LinhaRequest("New", "B");

        when(linhaRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(trajetoRepository.findByLinhaId(5L)).thenReturn(List.of());
        when(linhaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LinhaDTO result = service.updateLinha(5L, updated);

        assertEquals("New", result.getNome());
        assertEquals("B", result.getIdentificadorServico());
        verify(linhaRepository).save(existing);
    }
}
