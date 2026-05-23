package pt.notub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.models.ModalidadePasse;
import pt.notub.models.Tarifa;
import pt.notub.models.TipoUtilizador;
import pt.notub.services.TarifaService;

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

    @GetMapping("/tipo-utilizador/{tipoUtilizador}")
    public ResponseEntity<List<Tarifa>> getTarifasByTipoUtilizador(@PathVariable String tipoUtilizador) {
        try {
            TipoUtilizador tipo = TipoUtilizador.valueOf(tipoUtilizador.toUpperCase());
            return ResponseEntity.ok(tarifaService.getTarifasByTipoUtilizador(tipo));
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

    @GetMapping("/tipo-utilizador/{tipoUtilizador}/modalidade/{modalidade}")
    public ResponseEntity<?> getTarifaByTipoUtilizadorAndModalidade(@PathVariable String tipoUtilizador, @PathVariable String modalidade) {
        try {
            TipoUtilizador tipo = TipoUtilizador.valueOf(tipoUtilizador.toUpperCase());
            ModalidadePasse mod = ModalidadePasse.valueOf(modalidade.toUpperCase());
            return tarifaService.getTarifaByTipoUtilizadorAndModalidade(tipo, mod)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/calculadora")
    public ResponseEntity<?> calcularTarifa(
            @RequestParam(required = false) String tipoUtilizador,
            @RequestParam(required = false) String modalidade,
            @RequestParam int nrZonas) {
        TipoUtilizador tipo = null;
        if (tipoUtilizador != null) {
            try {
                tipo = TipoUtilizador.valueOf(tipoUtilizador.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Tipo de utilizador invalido");
            }
        }
        ModalidadePasse mod = null;
        if (modalidade != null) {
            try {
                mod = ModalidadePasse.valueOf(modalidade.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Modalidade invalida");
            }
        }
        return tarifaService.calcularTarifa(tipo, mod, nrZonas)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
