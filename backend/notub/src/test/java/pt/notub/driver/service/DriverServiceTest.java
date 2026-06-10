package pt.notub.driver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.driver.dto.DriverTrajetoDTO;
import pt.notub.network.entity.Direcao;
import pt.notub.network.entity.Linha;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.trip.repository.ViagemVeiculoRepository;
import pt.notub.vehicle.dto.VeiculoDTO;
import pt.notub.vehicle.entity.Autocarro;
import pt.notub.vehicle.repository.VeiculoRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ViagemVeiculoRepository viagemVeiculoRepository;

    @Mock
    private TrajetoRepository trajetoRepository;

    private DriverService service;

    @BeforeEach
    void setUp() {
        service = new DriverService(veiculoRepository, viagemVeiculoRepository, trajetoRepository);
    }

    @Test
    void getVeiculosComLinha_usesDerivedQuery() {
        Linha linha = new Linha();
        linha.setId(1L);
        linha.setNome("Linha 1");

        Autocarro autocarro = new Autocarro();
        autocarro.setId(10L);
        autocarro.setMatricula("AA-11-BB");
        autocarro.setLinha(linha);

        when(veiculoRepository.findByLinhaIsNotNull()).thenReturn(List.of(autocarro));

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
    void getTrajetos_withoutLineId_usesFindAll() {
        when(trajetoRepository.findAll()).thenReturn(List.of());

        List<DriverTrajetoDTO> result = service.getTrajetos(null);

        verify(trajetoRepository).findAll();
        verify(trajetoRepository, never()).findByLinhaId(anyLong());
        assertTrue(result.isEmpty());
    }
}
