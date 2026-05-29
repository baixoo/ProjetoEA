package pt.notub.zone;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.ZonaMapper;
import pt.notub.models.Zona;
import pt.notub.zone.ZonaService;
import pt.notub.zone.dto.ZonaDTO;

import java.util.List;

@RestController
@RequestMapping("/api/zonas")
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @GetMapping
    public ResponseEntity<List<ZonaDTO>> getAllZonas() {
        return ResponseEntity.ok(ZonaMapper.toDTOList(zonaService.getAllZonas()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZonaDTO> getZonaById(@PathVariable Long id) {
        return zonaService.getZonaById(id)
                .map(ZonaMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<ZonaDTO> getZonaByNome(@PathVariable String nome) {
        return zonaService.getZonaByNome(nome)
                .map(ZonaMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ZonaDTO> createZona(@RequestBody Zona zona) {
        return ResponseEntity.ok(ZonaMapper.toDTO(zonaService.createZona(zona)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZonaDTO> updateZona(@PathVariable Long id, @RequestBody Zona zona) {
        return ResponseEntity.ok(ZonaMapper.toDTO(zonaService.updateZona(id, zona)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteZona(@PathVariable Long id) {
        zonaService.deleteZona(id);
        return ResponseEntity.ok("Zona deleted");
    }
}
