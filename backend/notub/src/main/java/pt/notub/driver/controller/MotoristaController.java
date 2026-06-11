package pt.notub.driver.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.driver.dto.DriverTrajetoDTO;
import pt.notub.driver.dto.StartViagemRequest;
import pt.notub.driver.dto.ViagemScheduleDTO;
import pt.notub.driver.dto.AvancarViagemResponseDTO;
import pt.notub.driver.service.DriverService;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.vehicle.dto.VeiculoDTO;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
public class MotoristaController {

    private final DriverService driverService;

    public MotoristaController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping({"/veiculos", "/vehicles"})
    public ResponseEntity<List<VeiculoDTO>> getVeiculos() {
        return ResponseEntity.ok(driverService.getVeiculosComLinha());
    }

    @GetMapping({"/veiculos/{id}", "/vehicles/{id}"})
    public ResponseEntity<VeiculoDTO> getVeiculoById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getVeiculoById(id));
    }

    @GetMapping({"/veiculos/{veiculoId}/viagens", "/vehicles/{veiculoId}/trips"})
    public ResponseEntity<List<ViagemVeiculoDTO>> getViagensAtivas(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(driverService.getViagensAtivas(veiculoId));
    }

    @GetMapping({"/trajetos", "/routes"})
    public ResponseEntity<List<DriverTrajetoDTO>> getTrajetos(@RequestParam(required = false) Long linhaId) {
        return ResponseEntity.ok(driverService.getTrajetos(linhaId));
    }

    @GetMapping("/trajetos/{trajetoId}/horarios-disponiveis")
    public ResponseEntity<List<ViagemScheduleDTO>> getHorariosDisponiveis(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(driverService.getHorariosDisponiveis(trajetoId));
    }

    @PostMapping({"/viagens/start", "/trips/start"})
    public ResponseEntity<ViagemVeiculoDTO> startViagem(@RequestBody StartViagemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.startViagem(request));
    }

    @PostMapping({"/viagens/{id}/avancar", "/trips/{id}/advance"})
    public ResponseEntity<AvancarViagemResponseDTO> avancarViagem(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.avancarViagem(id));
    }

    @DeleteMapping({"/viagens/{id}/end", "/trips/{id}/end"})
    public ResponseEntity<Void> endViagem(@PathVariable Long id) {
        driverService.endViagem(id);
        return ResponseEntity.noContent().build();
    }
}
