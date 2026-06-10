package pt.notub.vehicle.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.vehicle.mapper.VeiculoMapper;
import pt.notub.vehicle.entity.Point;
import pt.notub.vehicle.entity.Veiculo;
import pt.notub.vehicle.service.VeiculoService;
import pt.notub.vehicle.dto.UpdateLotacaoRequest;
import pt.notub.vehicle.dto.VeiculoDTO;

import java.util.List;

@RestController
@RequestMapping({"/api/veiculos", "/api/vehicles"})
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @PostMapping
    public ResponseEntity<VeiculoDTO> createVeiculo(@RequestBody Veiculo veiculo) {
        return ResponseEntity.ok(VeiculoMapper.toDTO(veiculoService.saveVeiculo(veiculo)));
    }

    @GetMapping
    public ResponseEntity<List<VeiculoDTO>> getAllVeiculos() {
        return ResponseEntity.ok(VeiculoMapper.toDTOList(veiculoService.getAllVeiculos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoDTO> getVeiculoById(@PathVariable Long id) {
        return veiculoService.getVeiculoById(id)
                .map(VeiculoMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/matricula/{matricula}", "/license-plate/{matricula}"})
    public ResponseEntity<VeiculoDTO> getVeiculoByMatricula(@PathVariable String matricula) {
        return veiculoService.getVeiculoByMatricula(matricula)
                .map(VeiculoMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping({"/{id}/localizacao", "/{id}/location"})
    public ResponseEntity<VeiculoDTO> updateLocalizacao(@PathVariable Long id, @RequestBody Point localizacao) {
        return ResponseEntity.ok(VeiculoMapper.toDTO(veiculoService.updateLocalizacao(id, localizacao)));
    }

    @PutMapping({"/{id}/lotacao", "/{id}/occupancy"})
    public ResponseEntity<VeiculoDTO> updateLotacao(@PathVariable Long id, @RequestBody UpdateLotacaoRequest request) {
        return ResponseEntity.ok(VeiculoMapper.toDTO(veiculoService.updateLotacao(id, request.lotacao())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVeiculo(@PathVariable Long id) {
        veiculoService.deleteVeiculo(id);
        return ResponseEntity.ok("Veiculo deleted");
    }
}
