package pt.notub.trip;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.ViagemMapper;
import pt.notub.models.Utilizador;
import pt.notub.models.ViagemUtilizador;
import pt.notub.models.ViagemVeiculo;
import pt.notub.security.AuthenticatedUser;
import pt.notub.trip.ViagemService;
import pt.notub.trip.dto.ViagemDTO;
import pt.notub.trip.dto.ViagemVeiculoDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/viagens")
public class ViagemController {

    private final ViagemService viagemService;

    public ViagemController(ViagemService viagemService) {
        this.viagemService = viagemService;
    }

    @GetMapping("/utilizador")
    public ResponseEntity<List<ViagemDTO>> getAllViagensUtilizador(
            @AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(ViagemMapper.toDTOList(viagemService.getViagensByUtilizador(utilizador.getId())));
    }

    @GetMapping("/utilizador/{id}")
    public ResponseEntity<ViagemDTO> getViagemUtilizadorById(@PathVariable Long id) {
        return viagemService.getViagemUtilizadorById(id)
                .map(ViagemMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/utilizador/iniciar")
    public ResponseEntity<ViagemDTO> iniciarViagem(@RequestBody Map<String, Long> request) {
        Long tituloId = request.get("tituloId");
        Long paragemEntradaId = request.get("paragemEntradaId");
        Long viagemVeiculoId = request.get("viagemVeiculoId");
        ViagemUtilizador viagem = viagemService.iniciarViagem(tituloId, paragemEntradaId, viagemVeiculoId);
        return ResponseEntity.ok(ViagemMapper.toDTO(viagem));
    }

    @PutMapping("/utilizador/{id}/terminar")
    public ResponseEntity<ViagemDTO> terminarViagem(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long paragemSaidaId = request.get("paragemSaidaId");
        ViagemUtilizador viagem = viagemService.terminarViagem(id, paragemSaidaId);
        return ResponseEntity.ok(ViagemMapper.toDTO(viagem));
    }

    @GetMapping("/veiculo")
    public ResponseEntity<List<ViagemVeiculoDTO>> getAllViagensVeiculo() {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTOList(viagemService.getAllViagensVeiculo()));
    }

    @GetMapping("/veiculo/{id}")
    public ResponseEntity<ViagemVeiculoDTO> getViagemVeiculoById(@PathVariable Long id) {
        return viagemService.getViagemVeiculoById(id)
                .map(ViagemMapper::toVeiculoDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/veiculo/por-veiculo/{veiculoId}")
    public ResponseEntity<List<ViagemVeiculoDTO>> getViagensByVeiculo(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTOList(viagemService.getViagensByVeiculo(veiculoId)));
    }

    @GetMapping("/veiculo/por-trajeto/{trajetoId}")
    public ResponseEntity<List<ViagemVeiculoDTO>> getViagensByTrajeto(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTOList(viagemService.getViagensByTrajeto(trajetoId)));
    }

    @PostMapping("/veiculo")
    public ResponseEntity<ViagemVeiculoDTO> createViagemVeiculo(@RequestBody ViagemVeiculo viagemVeiculo) {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTO(viagemService.createViagemVeiculo(viagemVeiculo)));
    }
}
