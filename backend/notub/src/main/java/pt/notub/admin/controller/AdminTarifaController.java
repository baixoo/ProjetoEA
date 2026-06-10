package pt.notub.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.tariff.dto.TarifaDTO;
import pt.notub.tariff.dto.TarifaRequest;
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
    public ResponseEntity<TarifaDTO> create(@RequestBody TarifaRequest tarifa) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarifaService.createTarifa(tarifa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TarifaDTO> update(@PathVariable Long id, @RequestBody TarifaRequest updated) {
        return ResponseEntity.ok(tarifaService.updateTarifa(id, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tarifaService.deleteTarifa(id);
        return ResponseEntity.noContent().build();
    }
}
