package pt.notub.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.admin.dto.UpdateRoleRequest;
import pt.notub.user.dto.UserDTO;
import pt.notub.user.service.UtilizadorService;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/utilizadores", "/api/admin/users"})
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUtilizadorController {

    private final UtilizadorService utilizadorService;

    public AdminUtilizadorController(UtilizadorService utilizadorService) {
        this.utilizadorService = utilizadorService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAll() {
        return ResponseEntity.ok(utilizadorService.getAllUtilizadores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(utilizadorService.getUtilizadorById(id));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserDTO> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest pedido) {
        return ResponseEntity.ok(utilizadorService.updateRole(id, pedido.role()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        utilizadorService.deleteUtilizador(id);
        return ResponseEntity.noContent().build();
    }
}
