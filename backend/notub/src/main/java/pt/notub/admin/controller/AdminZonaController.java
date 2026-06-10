package pt.notub.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.zone.dto.ZonaDTO;
import pt.notub.zone.dto.ZonaRequest;
import pt.notub.zone.service.ZonaService;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping({"/api/admin/zonas", "/api/admin/zones"})
public class AdminZonaController {

    private final ZonaService zonaService;

    public AdminZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @PostMapping
    public ResponseEntity<ZonaDTO> create(@RequestBody ZonaRequest zona) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zonaService.createZona(zona));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZonaDTO> update(@PathVariable Long id, @RequestBody ZonaRequest updated) {
        return ResponseEntity.ok(zonaService.updateZona(id, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        zonaService.deleteZona(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/{id}/paragens/{paragemId}", "/{id}/stops/{paragemId}"})
    public ResponseEntity<ZonaDTO> addParagem(@PathVariable Long id, @PathVariable Long paragemId) {
        return ResponseEntity.ok(zonaService.addParagem(id, paragemId));
    }

    @DeleteMapping({"/{id}/paragens/{paragemId}", "/{id}/stops/{paragemId}"})
    public ResponseEntity<Void> removeParagem(@PathVariable Long id, @PathVariable Long paragemId) {
        zonaService.removeParagem(paragemId);
        return ResponseEntity.noContent().build();
    }
}
