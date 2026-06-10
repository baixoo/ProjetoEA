package pt.notub.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.vehicle.entity.Autocarro;
import pt.notub.vehicle.entity.Point;
import pt.notub.vehicle.entity.Veiculo;
import pt.notub.vehicle.dto.UpdateLotacaoRequest;
import pt.notub.vehicle.service.VeiculoService;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping({"/api/admin/veiculos", "/api/admin/vehicles"})
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
        Veiculo veiculo = veiculoService.getVeiculoById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado"));
        if (updated.getMatricula() != null) veiculo.setMatricula(updated.getMatricula());
        if (updated.getnLugares() != 0) veiculo.setnLugares(updated.getnLugares());
        return ResponseEntity.ok(veiculoService.saveVeiculo(veiculo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        veiculoService.deleteVeiculo(id);
        return ResponseEntity.ok("Veiculo eliminado");
    }

    @PutMapping({"/{id}/localizacao", "/{id}/location"})
    public ResponseEntity<?> updateLocalizacao(@PathVariable Long id, @RequestBody Point localizacao) {
        return ResponseEntity.ok(veiculoService.updateLocalizacao(id, localizacao));
    }

    @PutMapping({"/{id}/lotacao", "/{id}/occupancy"})
    public ResponseEntity<?> updateLotacao(@PathVariable Long id, @RequestBody UpdateLotacaoRequest pedido) {
        return ResponseEntity.ok(veiculoService.updateLotacao(id, pedido.lotacao()));
    }
}
