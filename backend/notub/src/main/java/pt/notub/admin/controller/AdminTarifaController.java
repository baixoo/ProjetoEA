package pt.notub.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.tariff.entity.Tarifa;
import pt.notub.tariff.service.TarifaService;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping({"/api/admin/tarifas", "/api/admin/tariffs"})
public class AdminTarifaController {

    private final TarifaService tarifaService;

    public AdminTarifaController(TarifaService tarifaService) {
        this.tarifaService = tarifaService;
    }

    @PostMapping
    public ResponseEntity<Tarifa> create(@RequestBody Tarifa tarifa) {
        return ResponseEntity.ok(tarifaService.createTarifa(tarifa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Tarifa updated) {
        return ResponseEntity.ok(tarifaService.updateTarifa(id, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        tarifaService.deleteTarifa(id);
        return ResponseEntity.ok("Tarifa eliminada");
    }
}
