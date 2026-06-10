package pt.notub.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.vehicle.dto.UpdateLotacaoRequest;
import pt.notub.vehicle.dto.UpdateLocalizacaoRequest;
import pt.notub.vehicle.dto.VeiculoDTO;
import pt.notub.vehicle.dto.VeiculoRequest;
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
    public ResponseEntity<VeiculoDTO> create(@RequestBody VeiculoRequest veiculo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veiculoService.createVeiculo(veiculo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoDTO> update(@PathVariable Long id, @RequestBody VeiculoRequest updated) {
        return ResponseEntity.ok(veiculoService.updateVeiculo(id, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        veiculoService.deleteVeiculo(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping({"/{id}/localizacao", "/{id}/location"})
    public ResponseEntity<VeiculoDTO> updateLocalizacao(@PathVariable Long id, @RequestBody UpdateLocalizacaoRequest localizacao) {
        return ResponseEntity.ok(veiculoService.updateLocalizacao(id, localizacao));
    }

    @PutMapping({"/{id}/lotacao", "/{id}/occupancy"})
    public ResponseEntity<VeiculoDTO> updateLotacao(@PathVariable Long id, @RequestBody UpdateLotacaoRequest pedido) {
        return ResponseEntity.ok(veiculoService.updateLotacao(id, pedido.lotacao()));
    }
}
