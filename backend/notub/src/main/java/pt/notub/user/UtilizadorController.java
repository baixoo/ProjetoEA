package pt.notub.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pt.notub.common.mapper.UserMapper;
import pt.notub.models.Utilizador;
import pt.notub.security.AuthenticatedUser;
import pt.notub.user.UtilizadorService;
import pt.notub.user.dto.UserDTO;

import java.util.List;

@RestController
@RequestMapping("/api/utilizadores")
public class UtilizadorController {

    private final UtilizadorService utilizadorService;

    public UtilizadorController(UtilizadorService utilizadorService) {
        this.utilizadorService = utilizadorService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUtilizadores() {
        return ResponseEntity.ok(UserMapper.toDTOList(utilizadorService.getAllUtilizadores()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUtilizadorById(@PathVariable Long id) {
        return utilizadorService.getUtilizadorById(id)
                .map(UserMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/perfil")
    public ResponseEntity<UserDTO> getMyProfile(@AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(UserMapper.toDTO(utilizador));
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> updateMyProfile(@AuthenticatedUser Utilizador utilizador, @Valid @RequestBody Utilizador updated) {
        try {
            return ResponseEntity.ok(UserMapper.toDTO(utilizadorService.updateUtilizador(utilizador.getEmail(), updated)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUtilizador(@PathVariable Long id) {
        utilizadorService.deleteUtilizador(id);
        return ResponseEntity.ok("Utilizador deleted");
    }
}
