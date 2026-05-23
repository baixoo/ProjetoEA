package pt.notub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.models.Point;
import pt.notub.models.Veiculo;
import pt.notub.services.VeiculoService;

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
    public ResponseEntity<Veiculo> createVeiculo(@RequestBody Veiculo veiculo) {
        return ResponseEntity.ok(veiculoService.saveVeiculo(veiculo));
    }

    @GetMapping
    public ResponseEntity<List<Veiculo>> getAllVeiculos() {
        return ResponseEntity.ok(veiculoService.getAllVeiculos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVeiculoById(@PathVariable Long id) {
        return veiculoService.getVeiculoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/matricula/{matricula}")
    public ResponseEntity<?> getVeiculoByMatricula(@PathVariable String matricula) {
        return veiculoService.getVeiculoByMatricula(matricula)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/localizacao")
    public ResponseEntity<?> updateLocalizacao(@PathVariable Long id, @RequestBody Point localizacao) {
        Veiculo veiculo = veiculoService.updateLocalizacao(id, localizacao);
        return ResponseEntity.ok(veiculo);
    }

    @PutMapping("/{id}/lotacao")
    public ResponseEntity<?> updateLotacao(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Veiculo veiculo = veiculoService.updateLotacao(id, request.get("lotacao"));
        return ResponseEntity.ok(veiculo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVeiculo(@PathVariable Long id) {
        veiculoService.deleteVeiculo(id);
        return ResponseEntity.ok("Veiculo deleted");
    }
}
