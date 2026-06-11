package pt.notub.vehicle.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.entity.Linha;
import pt.notub.network.repository.LinhaRepository;
import pt.notub.vehicle.dto.VeiculoDTO;
import pt.notub.vehicle.dto.UpdateLocalizacaoRequest;
import pt.notub.vehicle.dto.VeiculoRequest;
import pt.notub.vehicle.entity.TipoVeiculo;
import pt.notub.vehicle.entity.Point;
import pt.notub.vehicle.entity.Veiculo;
import pt.notub.vehicle.mapper.VeiculoMapper;
import pt.notub.vehicle.repository.VeiculoRepository;

import java.util.List;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final LinhaRepository linhaRepository;

    public VeiculoService(VeiculoRepository veiculoRepository, LinhaRepository linhaRepository) {
        this.veiculoRepository = veiculoRepository;
        this.linhaRepository = linhaRepository;
    }

    public List<VeiculoDTO> getAllVeiculos() {
        return VeiculoMapper.toDTOList(veiculoRepository.findAll());
    }

    public VeiculoDTO getVeiculoById(Long id) {
        return VeiculoMapper.toDTO(findVeiculo(id));
    }

    public VeiculoDTO getVeiculoByMatricula(String matricula) {
        Veiculo veiculo = veiculoRepository.findByMatricula(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado"));
        return VeiculoMapper.toDTO(veiculo);
    }

    public VeiculoDTO createVeiculo(VeiculoRequest request) {
        Veiculo veiculo = new Veiculo();
        veiculo.setTipo(TipoVeiculo.AUTOCARRO);
        applyRequest(veiculo, request, true);
        return VeiculoMapper.toDTO(veiculoRepository.save(veiculo));
    }

    public VeiculoDTO updateVeiculo(Long id, VeiculoRequest request) {
        Veiculo veiculo = findVeiculo(id);
        applyRequest(veiculo, request, false);
        return VeiculoMapper.toDTO(veiculoRepository.save(veiculo));
    }

    public VeiculoDTO updateLocalizacao(Long id, UpdateLocalizacaoRequest request) {
        Veiculo veiculo = findVeiculo(id);
        if (request.latitude() == null || request.longitude() == null) {
            throw new PedidoInvalidoException("Localizacao invalida");
        }
        veiculo.setLocalizacaoAtual(new Point(request.latitude(), request.longitude()));
        return VeiculoMapper.toDTO(veiculoRepository.save(veiculo));
    }

    public VeiculoDTO updateLotacao(Long id, int lotacao) {
        Veiculo veiculo = findVeiculo(id);
        veiculo.setLotacaoAtual(lotacao);
        return VeiculoMapper.toDTO(veiculoRepository.save(veiculo));
    }

    public void deleteVeiculo(Long id) {
        veiculoRepository.deleteById(id);
    }

    private Veiculo findVeiculo(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado"));
    }

    private void applyRequest(Veiculo veiculo, VeiculoRequest request, boolean isCreate) {
        if (request.matricula() != null) {
            veiculo.setMatricula(request.matricula());
        } else if (isCreate) {
            throw new PedidoInvalidoException("Matricula invalida");
        }

        if (request.nLugares() != null) {
            veiculo.setnLugares(request.nLugares());
        } else if (isCreate) {
            throw new PedidoInvalidoException("Numero de lugares invalido");
        }

        if (request.lotacaoAtual() != null) {
            veiculo.setLotacaoAtual(request.lotacaoAtual());
        } else if (isCreate) {
            veiculo.setLotacaoAtual(0);
        }

        if (request.localizacaoAtual() != null) {
            veiculo.setLocalizacaoAtual(request.localizacaoAtual());
        }

        if (request.tempoAtraso() != null) {
            veiculo.setTempoAtraso(request.tempoAtraso());
        } else if (isCreate) {
            veiculo.setTempoAtraso(0);
        }

        if (request.linhaId() != null) {
            Linha linha = linhaRepository.findById(request.linhaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada"));
            veiculo.setLinha(linha);
        }
    }
}
