package pt.projetoea.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.projetoea.models.ViagemUtilizador;
import pt.projetoea.models.ViagemVeiculo;
import pt.projetoea.services.ViagemService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/viagens")
public class ViagemController {

    private final ViagemService viagemService;

    public ViagemController(ViagemService viagemService) {
        this.viagemService = viagemService;
    }

    // ---- ViagemUtilizador ----

    @GetMapping("/utilizador")
    public ResponseEntity<List<ViagemUtilizador>> getAllViagensUtilizador() {
        return ResponseEntity.ok(viagemService.getAllViagensUtilizador());
    }

    @GetMapping("/utilizador/{id}")
    public ResponseEntity<?> getViagemUtilizadorById(@PathVariable Long id) {
        return viagemService.getViagemUtilizadorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/utilizador/iniciar")
    public ResponseEntity<?> iniciarViagem(@RequestBody Map<String, Long> request) {
        Long tituloId = request.get("tituloId");
        Long paragemEntradaId = request.get("paragemEntradaId");
        Long viagemVeiculoId = request.get("viagemVeiculoId");
        ViagemUtilizador viagem = viagemService.iniciarViagem(tituloId, paragemEntradaId, viagemVeiculoId);
        return ResponseEntity.ok(viagem);
    }

    @PutMapping("/utilizador/{id}/terminar")
    public ResponseEntity<?> terminarViagem(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long paragemSaidaId = request.get("paragemSaidaId");
        ViagemUtilizador viagem = viagemService.terminarViagem(id, paragemSaidaId);
        return ResponseEntity.ok(viagem);
    }

    // ---- ViagemVeiculo ----

    @GetMapping("/veiculo")
    public ResponseEntity<List<ViagemVeiculo>> getAllViagensVeiculo() {
        return ResponseEntity.ok(viagemService.getAllViagensVeiculo());
    }

    @GetMapping("/veiculo/{id}")
    public ResponseEntity<?> getViagemVeiculoById(@PathVariable Long id) {
        return viagemService.getViagemVeiculoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/veiculo/por-veiculo/{veiculoId}")
    public ResponseEntity<List<ViagemVeiculo>> getViagensByVeiculo(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(viagemService.getViagensByVeiculo(veiculoId));
    }

    @GetMapping("/veiculo/por-trajeto/{trajetoId}")
    public ResponseEntity<List<ViagemVeiculo>> getViagensByTrajeto(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(viagemService.getViagensByTrajeto(trajetoId));
    }

    @PostMapping("/veiculo")
    public ResponseEntity<ViagemVeiculo> createViagemVeiculo(@RequestBody ViagemVeiculo viagemVeiculo) {
        return ResponseEntity.ok(viagemService.createViagemVeiculo(viagemVeiculo));
    }
}
