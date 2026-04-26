package pt.projetoea.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.projetoea.models.Zona;
import pt.projetoea.services.ZonaService;

import java.util.List;

@RestController
@RequestMapping("/api/zonas")
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @GetMapping
    public ResponseEntity<List<Zona>> getAllZonas() {
        return ResponseEntity.ok(zonaService.getAllZonas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getZonaById(@PathVariable Long id) {
        return zonaService.getZonaById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<?> getZonaByNome(@PathVariable String nome) {
        return zonaService.getZonaByNome(nome)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Zona> createZona(@RequestBody Zona zona) {
        return ResponseEntity.ok(zonaService.createZona(zona));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateZona(@PathVariable Long id, @RequestBody Zona zona) {
        return ResponseEntity.ok(zonaService.updateZona(id, zona));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteZona(@PathVariable Long id) {
        zonaService.deleteZona(id);
        return ResponseEntity.ok("Zona deleted");
    }
}
