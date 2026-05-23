package pt.notub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.models.ViagemVeiculo;
import pt.notub.services.ViagemService;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping("/api/admin/viagens/veiculo")
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
