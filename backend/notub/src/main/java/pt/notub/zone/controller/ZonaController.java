package pt.notub.zone.controller;

import pt.notub.zone.service.ZonaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.zone.dto.ZonaDTO;

import java.util.List;

@RestController
@RequestMapping({"/api/zonas", "/api/zones"})
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @GetMapping
    public ResponseEntity<List<ZonaDTO>> getAllZonas() {
        return ResponseEntity.ok(zonaService.getAllZonas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZonaDTO> getZonaById(@PathVariable Long id) {
        return ResponseEntity.ok(zonaService.getZonaById(id));
    }

    @GetMapping({"/nome/{nome}", "/name/{nome}"})
    public ResponseEntity<ZonaDTO> getZonaByNome(@PathVariable String nome) {
        return ResponseEntity.ok(zonaService.getZonaByNome(nome));
    }
}
