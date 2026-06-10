package pt.notub.user.controller;

import pt.notub.user.service.UtilizadorService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;
import pt.notub.user.dto.UpdateUserProfileRequest;
import pt.notub.user.dto.UserDTO;

import java.util.List;

@RestController
@RequestMapping({"/api/utilizadores", "/api/users"})
public class UtilizadorController {

    private final UtilizadorService utilizadorService;

    public UtilizadorController(UtilizadorService utilizadorService) {
        this.utilizadorService = utilizadorService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUtilizadores() {
        return ResponseEntity.ok(utilizadorService.getAllUtilizadores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUtilizadorById(@PathVariable Long id) {
        return ResponseEntity.ok(utilizadorService.getUtilizadorById(id));
    }

    @GetMapping({"/perfil", "/profile"})
    public ResponseEntity<UserDTO> getMyProfile(@AuthenticatedUser AuthenticatedUserContext utilizador) {
        return ResponseEntity.ok(utilizadorService.getUtilizadorById(utilizador.id()));
    }

    @PutMapping({"/perfil", "/profile"})
    public ResponseEntity<UserDTO> updateMyProfile(@AuthenticatedUser AuthenticatedUserContext utilizador,
                                                   @Valid @RequestBody UpdateUserProfileRequest updated) {
        return ResponseEntity.ok(utilizadorService.updateUtilizador(utilizador.email(), updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUtilizador(@PathVariable Long id) {
        utilizadorService.deleteUtilizador(id);
        return ResponseEntity.noContent().build();
    }
}
