package pt.projetoea.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.projetoea.models.ModalidadePasse;
import pt.projetoea.models.Tarifa;
import pt.projetoea.models.TipoPerfil;
import pt.projetoea.services.TarifaService;

import java.util.List;

@RestController
@RequestMapping("/api/tarifas")
public class TarifaController {

    private final TarifaService tarifaService;

    public TarifaController(TarifaService tarifaService) {
        this.tarifaService = tarifaService;
    }

    @GetMapping
    public ResponseEntity<List<Tarifa>> getAllTarifas() {
        return ResponseEntity.ok(tarifaService.getAllTarifas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTarifaById(@PathVariable Long id) {
        return tarifaService.getTarifaById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/perfil/{perfil}")
    public ResponseEntity<List<Tarifa>> getTarifasByPerfil(@PathVariable String perfil) {
        try {
            TipoPerfil tipoPerfil = TipoPerfil.valueOf(perfil.toUpperCase());
            return ResponseEntity.ok(tarifaService.getTarifasByPerfil(tipoPerfil));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/modalidade/{modalidade}")
    public ResponseEntity<List<Tarifa>> getTarifasByModalidade(@PathVariable String modalidade) {
        try {
            ModalidadePasse mod = ModalidadePasse.valueOf(modalidade.toUpperCase());
            return ResponseEntity.ok(tarifaService.getTarifasByModalidade(mod));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/perfil/{perfil}/modalidade/{modalidade}")
    public ResponseEntity<?> getTarifaByPerfilAndModalidade(@PathVariable String perfil, @PathVariable String modalidade) {
        try {
            TipoPerfil tipoPerfil = TipoPerfil.valueOf(perfil.toUpperCase());
            ModalidadePasse mod = ModalidadePasse.valueOf(modalidade.toUpperCase());
            return tarifaService.getTarifaByPerfilAndModalidade(tipoPerfil, mod)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Tarifa> createTarifa(@RequestBody Tarifa tarifa) {
        return ResponseEntity.ok(tarifaService.createTarifa(tarifa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTarifa(@PathVariable Long id, @RequestBody Tarifa tarifa) {
        return ResponseEntity.ok(tarifaService.updateTarifa(id, tarifa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTarifa(@PathVariable Long id) {
        tarifaService.deleteTarifa(id);
        return ResponseEntity.ok("Tarifa deleted");
    }
}
