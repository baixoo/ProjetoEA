package pt.notub.services;

import org.springframework.stereotype.Service;
import pt.notub.models.Veiculo;
import pt.notub.models.Point;
import pt.notub.repositories.VeiculoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;

    public VeiculoService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    public List<Veiculo> getAllVeiculos() {
        return veiculoRepository.findAll();
    }

    public Optional<Veiculo> getVeiculoById(Long id) {
        return veiculoRepository.findById(id);
    }

    public Optional<Veiculo> getVeiculoByMatricula(String matricula) {
        return veiculoRepository.findByMatricula(matricula);
    }

    public Veiculo updateLocalizacao(Long id, Point localizacao) {
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veiculo not found"));
        veiculo.setLocalizacaoAtual(localizacao);
        return veiculoRepository.save(veiculo);
    }

    public Veiculo updateLotacao(Long id, int lotacao) {
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veiculo not found"));
        veiculo.setLotacaoAtual(lotacao);
        return veiculoRepository.save(veiculo);
    }

    public Veiculo saveVeiculo(Veiculo veiculo) {
        return veiculoRepository.save(veiculo);
    }

    public void deleteVeiculo(Long id) {
        veiculoRepository.deleteById(id);
    }
}
