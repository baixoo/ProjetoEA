package pt.notub.tariff.controller;

import pt.notub.tariff.service.TarifaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.tariff.dto.TarifaDTO;

import java.util.List;

@RestController
@RequestMapping({"/api/tarifas", "/api/tariffs"})
public class TarifaController {

    private final TarifaService tarifaService;

    public TarifaController(TarifaService tarifaService) {
        this.tarifaService = tarifaService;
    }

    @GetMapping
    public ResponseEntity<List<TarifaDTO>> getAllTarifas() {
        return ResponseEntity.ok(tarifaService.getAllTarifas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarifaDTO> getTarifaById(@PathVariable Long id) {
        return ResponseEntity.ok(tarifaService.getTarifaById(id));
    }

    @GetMapping({"/tipo-utilizador/{tipoUtilizador}", "/user-type/{tipoUtilizador}"})
    public ResponseEntity<List<TarifaDTO>> getTarifasByTipoUtilizador(@PathVariable String tipoUtilizador) {
        return ResponseEntity.ok(tarifaService.getTarifasByTipoUtilizador(tipoUtilizador));
    }

    @GetMapping({"/modalidade/{modalidade}", "/pass-type/{modalidade}"})
    public ResponseEntity<List<TarifaDTO>> getTarifasByModalidade(@PathVariable String modalidade) {
        return ResponseEntity.ok(tarifaService.getTarifasByModalidade(modalidade));
    }

    @GetMapping({"/tipo-utilizador/{tipoUtilizador}/modalidade/{modalidade}", "/user-type/{tipoUtilizador}/pass-type/{modalidade}"})
    public ResponseEntity<TarifaDTO> getTarifaByTipoUtilizadorAndModalidade(@PathVariable String tipoUtilizador, @PathVariable String modalidade) {
        return ResponseEntity.ok(tarifaService.getTarifaByTipoUtilizadorAndModalidade(tipoUtilizador, modalidade));
    }

    @GetMapping({"/calculadora", "/calculator"})
    public ResponseEntity<TarifaDTO> calcularTarifa(
            @RequestParam(required = false) String tipoProduto,
            @RequestParam(required = false) String tipoUtilizador,
            @RequestParam(required = false) String modalidade,
            @RequestParam int nrZonas) {
        return ResponseEntity.ok(tarifaService.calcularTarifa(tipoProduto, tipoUtilizador, modalidade, nrZonas));
    }
}
