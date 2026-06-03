package pt.notub.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.exception.RecursoNaoEncontradoException;
import pt.notub.models.TipoPapel;
import pt.notub.models.Utilizador;
import pt.notub.repositories.UtilizadorRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/utilizadores")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUtilizadorController {

    private final UtilizadorRepository utilizadorRepository;

    public AdminUtilizadorController(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @GetMapping
    public ResponseEntity<List<Utilizador>> getAll() {
        return ResponseEntity.ok(utilizadorRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return utilizadorRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> pedido) {
        Utilizador utilizador = utilizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        TipoPapel novoRole = TipoPapel.valueOf(pedido.get("role").toUpperCase());
        utilizador.setRole(novoRole);
        utilizadorRepository.save(utilizador);
        return ResponseEntity.ok(utilizador);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        utilizadorRepository.deleteById(id);
        return ResponseEntity.ok("Utilizador eliminado");
    }
}
