package pt.notub.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.zone.entity.Zona;
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
    public ResponseEntity<Zona> create(@RequestBody Zona zona) {
        return ResponseEntity.ok(zonaService.createZona(zona));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Zona updated) {
        return ResponseEntity.ok(zonaService.updateZona(id, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        zonaService.deleteZona(id);
        return ResponseEntity.ok("Zona eliminada");
    }

    @PostMapping({"/{id}/paragens/{paragemId}", "/{id}/stops/{paragemId}"})
    public ResponseEntity<?> addParagem(@PathVariable Long id, @PathVariable Long paragemId) {
        return ResponseEntity.ok(zonaService.addParagem(id, paragemId));
    }

    @DeleteMapping({"/{id}/paragens/{paragemId}", "/{id}/stops/{paragemId}"})
    public ResponseEntity<?> removeParagem(@PathVariable Long id, @PathVariable Long paragemId) {
        zonaService.removeParagem(paragemId);
        return ResponseEntity.ok("Paragem removida da zona");
    }
}
