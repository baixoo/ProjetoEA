package pt.notub.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.service.ViagemService;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping({"/api/admin/viagens/veiculo", "/api/admin/trips/vehicle"})
public class AdminViagemController {

    private final ViagemService viagemService;

    public AdminViagemController(ViagemService viagemService) {
        this.viagemService = viagemService;
    }

    @PostMapping
    public ResponseEntity<ViagemVeiculo> create(@RequestBody ViagemVeiculo viagemVeiculo) {
        return ResponseEntity.ok(viagemService.createViagemVeiculo(viagemVeiculo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        viagemService.deleteViagemVeiculo(id);
        return ResponseEntity.ok("Viagem de veiculo eliminada");
    }
}
