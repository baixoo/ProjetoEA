package pt.notub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.models.Utilizador;
import pt.notub.security.AuthenticatedUser;
import pt.notub.services.UtilizadorService;

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
    public ResponseEntity<?> getMyProfile(@AuthenticatedUser Utilizador utilizador) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(utilizador);
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> updateMyProfile(@AuthenticatedUser Utilizador utilizador, @RequestBody Utilizador updated) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(utilizadorService.updateUtilizador(utilizador.getEmail(), updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUtilizador(@PathVariable Long id) {
        utilizadorService.deleteUtilizador(id);
        return ResponseEntity.ok("Utilizador deleted");
    }
}
