package pt.projetoea.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pt.projetoea.models.Utilizador;
import pt.projetoea.security.UserDetailsImpl;
import pt.projetoea.services.UtilizadorService;

import java.util.List;

@RestController
@RequestMapping("/api/utilizadores")
public class UtilizadorController {

    private final UtilizadorService utilizadorService;

    public UtilizadorController(UtilizadorService utilizadorService) {
        this.utilizadorService = utilizadorService;
    }

    @GetMapping
    public ResponseEntity<List<Utilizador>> getAllUtilizadores() {
        return ResponseEntity.ok(utilizadorService.getAllUtilizadores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUtilizadorById(@PathVariable Long id) {
        return utilizadorService.getUtilizadorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/perfil")
    public ResponseEntity<?> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
        return utilizadorService.getUtilizadorByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> updateMyProfile(@RequestBody Utilizador updated) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
        Utilizador utilizador = utilizadorService.updateUtilizador(email, updated);
        return ResponseEntity.ok(utilizador);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUtilizador(@PathVariable Long id) {
        utilizadorService.deleteUtilizador(id);
        return ResponseEntity.ok("Utilizador deleted");
    }
}
