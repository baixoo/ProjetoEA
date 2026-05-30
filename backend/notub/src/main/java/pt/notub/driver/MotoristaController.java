package pt.notub.driver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.VeiculoMapper;
import pt.notub.common.mapper.ViagemMapper;
import pt.notub.exception.RecursoNaoEncontradoException;
import pt.notub.models.Trajeto;
import pt.notub.models.Veiculo;
import pt.notub.models.ViagemVeiculo;
import pt.notub.repositories.TrajetoRepository;
import pt.notub.repositories.VeiculoRepository;
import pt.notub.repositories.ViagemVeiculoRepository;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.vehicle.dto.VeiculoDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class MotoristaController {

    private final VeiculoRepository veiculoRepository;
    private final ViagemVeiculoRepository viagemVeiculoRepository;
    private final TrajetoRepository trajetoRepository;

    public MotoristaController(VeiculoRepository veiculoRepository,
                               ViagemVeiculoRepository viagemVeiculoRepository,
                               TrajetoRepository trajetoRepository) {
        this.veiculoRepository = veiculoRepository;
        this.viagemVeiculoRepository = viagemVeiculoRepository;
        this.trajetoRepository = trajetoRepository;
    }

    @GetMapping("/veiculos")
    public ResponseEntity<List<VeiculoDTO>> getVeiculos() {
        return ResponseEntity.ok(VeiculoMapper.toDTOList(veiculoRepository.findAll()));
    }

    @GetMapping("/veiculos/{veiculoId}/viagens")
    public ResponseEntity<List<ViagemVeiculoDTO>> getViagensAtivas(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findByVeiculoId(veiculoId)));
    }

    @GetMapping("/trajetos")
    public ResponseEntity<List<Map<String, Object>>> getTrajetos() {
        List<Trajeto> trajetos = trajetoRepository.findAll();
        List<Map<String, Object>> result = trajetos.stream().map(t -> {
            String linhaNome = t.getLinha() != null ? t.getLinha().getNome() : "Sem linha";
            String direcao = t.getDirecao() != null ? t.getDirecao().name() : "?";
            return Map.<String, Object>of(
                "id", t.getId(),
                "linha", linhaNome,
                "direcao", direcao
            );
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/viagens/start")
    public ResponseEntity<ViagemVeiculoDTO> startViagem(@RequestBody Map<String, Long> request) {
        Long veiculoId = request.get("veiculoId");
        Long trajetoId = request.get("trajetoId");
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado"));
        Trajeto trajeto = trajetoRepository.findById(trajetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trajeto nao encontrado"));
        ViagemVeiculo vv = new ViagemVeiculo();
        vv.setVeiculo(veiculo);
        vv.setTrajeto(trajeto);
        vv.setTripId("TRIP-" + veiculoId + "-" + System.currentTimeMillis());
        vv = viagemVeiculoRepository.save(vv);
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTO(vv));
    }

    @DeleteMapping("/viagens/{id}/end")
    public ResponseEntity<?> endViagem(@PathVariable Long id) {
        ViagemVeiculo vv = viagemVeiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Viagem nao encontrada"));
        viagemVeiculoRepository.delete(vv);
        return ResponseEntity.ok(Map.of("mensagem", "Viagem terminada"));
    }
}
