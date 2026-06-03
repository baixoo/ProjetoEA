package pt.notub.vehicle;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.VeiculoMapper;
import pt.notub.models.Point;
import pt.notub.models.Veiculo;
import pt.notub.vehicle.VeiculoService;
import pt.notub.vehicle.dto.VeiculoDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @PostMapping
    public ResponseEntity<VeiculoDTO> createVeiculo(@RequestBody Veiculo veiculo) {
        return ResponseEntity.ok(VeiculoMapper.toDTO(veiculoService.saveVeiculo(veiculo)));
    }

    @GetMapping
    public ResponseEntity<List<VeiculoDTO>> getAllVeiculos() {
        return ResponseEntity.ok(VeiculoMapper.toDTOList(veiculoService.getAllVeiculos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoDTO> getVeiculoById(@PathVariable Long id) {
        return veiculoService.getVeiculoById(id)
                .map(VeiculoMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/matricula/{matricula}")
    public ResponseEntity<VeiculoDTO> getVeiculoByMatricula(@PathVariable String matricula) {
        return veiculoService.getVeiculoByMatricula(matricula)
                .map(VeiculoMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/localizacao")
    public ResponseEntity<VeiculoDTO> updateLocalizacao(@PathVariable Long id, @RequestBody Point localizacao) {
        return ResponseEntity.ok(VeiculoMapper.toDTO(veiculoService.updateLocalizacao(id, localizacao)));
    }

    @PutMapping("/{id}/lotacao")
    public ResponseEntity<VeiculoDTO> updateLotacao(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        return ResponseEntity.ok(VeiculoMapper.toDTO(veiculoService.updateLotacao(id, request.get("lotacao"))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVeiculo(@PathVariable Long id) {
        veiculoService.deleteVeiculo(id);
        return ResponseEntity.ok("Veiculo deleted");
    }
}
