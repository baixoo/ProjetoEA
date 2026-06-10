package pt.notub.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.admin.dto.UpdateRoleRequest;
import pt.notub.user.entity.Utilizador;
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
    public ResponseEntity<List<Utilizador>> getAll() {
        return ResponseEntity.ok(utilizadorService.getAllUtilizadores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return utilizadorService.getUtilizadorById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest pedido) {
        return ResponseEntity.ok(utilizadorService.updateRole(id, pedido.role()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        utilizadorService.deleteUtilizador(id);
        return ResponseEntity.ok("Utilizador eliminado");
    }
}
