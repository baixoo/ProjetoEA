package pt.notub.driver.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.driver.dto.DriverTrajetoDTO;
import pt.notub.driver.dto.StartViagemRequest;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.mapper.ViagemMapper;
import pt.notub.trip.repository.ViagemVeiculoRepository;
import pt.notub.vehicle.dto.VeiculoDTO;
import pt.notub.vehicle.entity.Veiculo;
import pt.notub.vehicle.mapper.VeiculoMapper;
import pt.notub.vehicle.repository.VeiculoRepository;

@Service
public class DriverService {

    private final VeiculoRepository veiculoRepository;
    private final ViagemVeiculoRepository viagemVeiculoRepository;
    private final TrajetoRepository trajetoRepository;

    public DriverService(VeiculoRepository veiculoRepository,
                         ViagemVeiculoRepository viagemVeiculoRepository,
                         TrajetoRepository trajetoRepository) {
        this.veiculoRepository = veiculoRepository;
        this.viagemVeiculoRepository = viagemVeiculoRepository;
        this.trajetoRepository = trajetoRepository;
    }

    public List<VeiculoDTO> getVeiculosComLinha() {
        return VeiculoMapper.toDTOList(veiculoRepository.findByLinhaIsNotNull());
    }

    public List<ViagemVeiculoDTO> getViagensAtivas(Long veiculoId) {
        return ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findByVeiculoId(veiculoId));
    }

    public List<DriverTrajetoDTO> getTrajetos(Long linhaId) {
        List<Trajeto> trajetos = linhaId != null
                ? trajetoRepository.findByLinhaId(linhaId)
                : trajetoRepository.findAll();

        return trajetos.stream().map(this::toDriverTrajetoDTO).toList();
    }

    public ViagemVeiculoDTO startViagem(StartViagemRequest request) {
        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado"));
        Trajeto trajeto = trajetoRepository.findById(request.trajetoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trajeto nao encontrado"));

        ViagemVeiculo viagem = new ViagemVeiculo();
        viagem.setVeiculo(veiculo);
        viagem.setTrajeto(trajeto);
        // viagem.setTripId("TRIP-" + request.veiculoId() + "-" + System.currentTimeMillis());

        return ViagemMapper.toVeiculoDTO(viagemVeiculoRepository.save(viagem));
    }

    public void endViagem(Long id) {
        ViagemVeiculo viagem = viagemVeiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Viagem nao encontrada"));
        viagemVeiculoRepository.delete(viagem);
    }

    private DriverTrajetoDTO toDriverTrajetoDTO(Trajeto trajeto) {
        String linhaNome = trajeto.getLinha() != null ? trajeto.getLinha().getNome() : "Sem linha";
        String direcao = trajeto.getDirecao() != null ? trajeto.getDirecao().name() : "?";
        String primeiraParagem = "";
        String ultimaParagem = "";

        if (trajeto.getPontosDePassagem() != null && !trajeto.getPontosDePassagem().isEmpty()) {
            List<PontosDePassagem> sorted = trajeto.getPontosDePassagem().stream()
                    .sorted(Comparator.comparingInt(PontosDePassagem::getOrdem))
                    .toList();
            if (sorted.getFirst().getParagem() != null) {
                primeiraParagem = sorted.getFirst().getParagem().getNome();
            }
            if (sorted.getLast().getParagem() != null) {
                ultimaParagem = sorted.getLast().getParagem().getNome();
            }
        }

        return new DriverTrajetoDTO(trajeto.getId(), linhaNome, direcao, primeiraParagem, ultimaParagem);
    }
}
