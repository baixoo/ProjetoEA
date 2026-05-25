package pt.notub.controllers.Admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.models.Autocarro;
import pt.notub.models.Point;
import pt.notub.models.Veiculo;
import pt.notub.services.VeiculoService;

import java.util.Map;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping("/api/admin/veiculos")
public class AdminVeiculoController {

    private final VeiculoService veiculoService;

    public AdminVeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @PostMapping
    public ResponseEntity<Veiculo> create(@RequestBody Autocarro veiculo) {
        return ResponseEntity.ok(veiculoService.saveVeiculo(veiculo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Autocarro updated) {
        Veiculo veiculo = veiculoService.getVeiculoById(id).orElseThrow(() -> new RuntimeException("Veiculo nao encontrado"));
        if (updated.getMatricula() != null) veiculo.setMatricula(updated.getMatricula());
        if (updated.getnLugares() != 0) veiculo.setnLugares(updated.getnLugares());
        return ResponseEntity.ok(veiculoService.saveVeiculo(veiculo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        veiculoService.deleteVeiculo(id);
        return ResponseEntity.ok("Veiculo eliminado");
    }

    @PutMapping("/{id}/localizacao")
    public ResponseEntity<?> updateLocalizacao(@PathVariable Long id, @RequestBody Point localizacao) {
        return ResponseEntity.ok(veiculoService.updateLocalizacao(id, localizacao));
    }

    @PutMapping("/{id}/lotacao")
    public ResponseEntity<?> updateLotacao(@PathVariable Long id, @RequestBody Map<String, Integer> pedido) {
        return ResponseEntity.ok(veiculoService.updateLotacao(id, pedido.get("lotacao")));
    }
}
