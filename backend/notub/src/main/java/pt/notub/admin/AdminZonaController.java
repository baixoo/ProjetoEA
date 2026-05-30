package pt.notub.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.exception.RecursoNaoEncontradoException;
import pt.notub.models.Paragem;
import pt.notub.models.Zona;
import pt.notub.repositories.ParagemRepository;
import pt.notub.repositories.ZonaRepository;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping("/api/admin/zonas")
public class AdminZonaController {

    private final ZonaRepository zonaRepository;
    private final ParagemRepository paragemRepository;

    public AdminZonaController(ZonaRepository zonaRepository, ParagemRepository paragemRepository) {
        this.zonaRepository = zonaRepository;
        this.paragemRepository = paragemRepository;
    }

    @PostMapping
    public ResponseEntity<Zona> create(@RequestBody Zona zona) {
        return ResponseEntity.ok(zonaRepository.save(zona));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Zona updated) {
        Zona zona = zonaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        if (updated.getNome() != null) zona.setNome(updated.getNome());
        if (updated.getNum() != 0) zona.setNum(updated.getNum());
        return ResponseEntity.ok(zonaRepository.save(zona));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        zonaRepository.deleteById(id);
        return ResponseEntity.ok("Zona eliminada");
    }

    @PostMapping("/{id}/paragens/{paragemId}")
    public ResponseEntity<?> addParagem(@PathVariable Long id, @PathVariable Long paragemId) {
        Zona zona = zonaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        Paragem paragem = paragemRepository.findById(paragemId).orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
        zona.getParagens().add(paragem);
        zonaRepository.save(zona);
        return ResponseEntity.ok(zona);
    }

    @DeleteMapping("/{id}/paragens/{paragemId}")
    public ResponseEntity<?> removeParagem(@PathVariable Long id, @PathVariable Long paragemId) {
        Zona zona = zonaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        zona.getParagens().removeIf(p -> p.getId().equals(paragemId));
        zonaRepository.save(zona);
        return ResponseEntity.ok(zona);
    }
}
