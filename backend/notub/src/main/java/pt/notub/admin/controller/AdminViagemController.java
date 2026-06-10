package pt.notub.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.trip.dto.CreateViagemVeiculoRequest;
import pt.notub.trip.dto.ViagemVeiculoDTO;
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
    public ResponseEntity<ViagemVeiculoDTO> create(@RequestBody CreateViagemVeiculoRequest viagemVeiculo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(viagemService.createViagemVeiculo(viagemVeiculo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        viagemService.deleteViagemVeiculo(id);
        return ResponseEntity.noContent().build();
    }
}
