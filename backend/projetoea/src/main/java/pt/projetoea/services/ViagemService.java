package pt.projetoea.services;

import org.springframework.stereotype.Service;
import pt.projetoea.models.*;
import pt.projetoea.repositories.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ViagemService {

    private final ViagemUtilizadorRepository viagemUtilizadorRepository;
    private final ViagemVeiculoRepository viagemVeiculoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ParagemRepository paragemRepository;
    private final TituloTransporteRepository tituloTransporteRepository;

    public ViagemService(ViagemUtilizadorRepository viagemUtilizadorRepository,
                         ViagemVeiculoRepository viagemVeiculoRepository,
                         UtilizadorRepository utilizadorRepository,
                         ParagemRepository paragemRepository,
                         TituloTransporteRepository tituloTransporteRepository) {
        this.viagemUtilizadorRepository = viagemUtilizadorRepository;
        this.viagemVeiculoRepository = viagemVeiculoRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.paragemRepository = paragemRepository;
        this.tituloTransporteRepository = tituloTransporteRepository;
    }

    // ---- ViagemUtilizador ----

    public List<ViagemUtilizador> getAllViagensUtilizador() {
        return viagemUtilizadorRepository.findAll();
    }

    public Optional<ViagemUtilizador> getViagemUtilizadorById(Long id) {
        return viagemUtilizadorRepository.findById(id);
    }

    public ViagemUtilizador iniciarViagem(Long tituloId, Long paragemEntradaId, Long viagemVeiculoId) {
        TituloTransporte titulo = tituloTransporteRepository.findById(tituloId)
                .orElseThrow(() -> new RuntimeException("Titulo not found"));
        Paragem paragemEntrada = paragemRepository.findById(paragemEntradaId)
                .orElseThrow(() -> new RuntimeException("Paragem not found"));
        ViagemVeiculo viagemVeiculo = viagemVeiculoRepository.findById(viagemVeiculoId)
                .orElseThrow(() -> new RuntimeException("ViagemVeiculo not found"));

        ViagemUtilizador viagem = new ViagemUtilizador();
        viagem.setTitulo(titulo);
        viagem.setParagemEntrada(paragemEntrada);
        viagem.setViagemVeiculo(viagemVeiculo);
        viagem.setInicio(LocalDateTime.now());
        viagem.setEstado(EstadoViagem.EM_CURSO);

        return viagemUtilizadorRepository.save(viagem);
    }

    public ViagemUtilizador terminarViagem(Long viagemId, Long paragemSaidaId) {
        ViagemUtilizador viagem = viagemUtilizadorRepository.findById(viagemId)
                .orElseThrow(() -> new RuntimeException("Viagem not found"));
        Paragem paragemSaida = paragemRepository.findById(paragemSaidaId)
                .orElseThrow(() -> new RuntimeException("Paragem not found"));

        viagem.setParagemSaida(paragemSaida);
        viagem.setFim(LocalDateTime.now());
        viagem.setEstado(EstadoViagem.CONCLUIDA);

        return viagemUtilizadorRepository.save(viagem);
    }

    // ---- ViagemVeiculo ----

    public List<ViagemVeiculo> getAllViagensVeiculo() {
        return viagemVeiculoRepository.findAll();
    }

    public Optional<ViagemVeiculo> getViagemVeiculoById(Long id) {
        return viagemVeiculoRepository.findById(id);
    }

    public List<ViagemVeiculo> getViagensByVeiculo(Long veiculoId) {
        return viagemVeiculoRepository.findByVeiculoId(veiculoId);
    }

    public List<ViagemVeiculo> getViagensByTrajeto(Long trajetoId) {
        return viagemVeiculoRepository.findByTrajetoId(trajetoId);
    }

    public ViagemVeiculo createViagemVeiculo(ViagemVeiculo viagemVeiculo) {
        return viagemVeiculoRepository.save(viagemVeiculo);
    }
}
