package pt.notub.vehicle.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.vehicle.dto.UpdateLotacaoRequest;
import pt.notub.vehicle.dto.UpdateLocalizacaoRequest;
import pt.notub.vehicle.dto.VeiculoRequest;
import pt.notub.vehicle.dto.VeiculoDTO;
import pt.notub.vehicle.service.VeiculoService;

import java.util.List;

@RestController
@RequestMapping({"/api/veiculos", "/api/vehicles"})
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @PostMapping
    public ResponseEntity<VeiculoDTO> createVeiculo(@RequestBody VeiculoRequest veiculo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veiculoService.createVeiculo(veiculo));
    }

    @GetMapping
    public ResponseEntity<List<VeiculoDTO>> getAllVeiculos() {
        return ResponseEntity.ok(veiculoService.getAllVeiculos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoDTO> getVeiculoById(@PathVariable Long id) {
        return ResponseEntity.ok(veiculoService.getVeiculoById(id));
    }

    @GetMapping({"/matricula/{matricula}", "/license-plate/{matricula}"})
    public ResponseEntity<VeiculoDTO> getVeiculoByMatricula(@PathVariable String matricula) {
        return ResponseEntity.ok(veiculoService.getVeiculoByMatricula(matricula));
    }

    @PutMapping({"/{id}/localizacao", "/{id}/location"})
    public ResponseEntity<VeiculoDTO> updateLocalizacao(@PathVariable Long id, @RequestBody UpdateLocalizacaoRequest localizacao) {
        return ResponseEntity.ok(veiculoService.updateLocalizacao(id, localizacao));
    }

    @PutMapping({"/{id}/lotacao", "/{id}/occupancy"})
    public ResponseEntity<VeiculoDTO> updateLotacao(@PathVariable Long id, @RequestBody UpdateLotacaoRequest request) {
        return ResponseEntity.ok(veiculoService.updateLotacao(id, request.lotacao()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVeiculo(@PathVariable Long id) {
        veiculoService.deleteVeiculo(id);
        return ResponseEntity.noContent().build();
    }
}
