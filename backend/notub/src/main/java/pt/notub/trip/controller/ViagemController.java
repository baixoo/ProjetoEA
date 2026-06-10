package pt.notub.trip.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;
import pt.notub.trip.dto.CreateViagemVeiculoRequest;
import pt.notub.trip.dto.IniciarViagemRequest;
import pt.notub.trip.dto.ParagemAtualDTO;
import pt.notub.trip.dto.TerminarViagemRequest;
import pt.notub.trip.dto.ViagemDTO;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.trip.dto.ZonaMinMaxDTO;
import pt.notub.trip.service.ViagemService;

import java.util.List;

@RestController
@RequestMapping({"/api/viagens", "/api/trips"})
public class ViagemController {

    private final ViagemService viagemService;

    public ViagemController(ViagemService viagemService) {
        this.viagemService = viagemService;
    }

    @GetMapping({"/utilizador", "/user"})
    public ResponseEntity<List<ViagemDTO>> getAllViagensUtilizador(
            @AuthenticatedUser AuthenticatedUserContext utilizador) {
        return ResponseEntity.ok(viagemService.getViagensByUtilizador(utilizador.id()));
    }

    @GetMapping({"/utilizador/{id}", "/user/{id}"})
    public ResponseEntity<ViagemDTO> getViagemUtilizadorById(@PathVariable Long id) {
        return ResponseEntity.ok(viagemService.getViagemUtilizadorById(id));
    }

    @PostMapping({"/utilizador/iniciar", "/user/start"})
    public ResponseEntity<ViagemDTO> iniciarViagem(@RequestBody IniciarViagemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(viagemService.iniciarViagem(
                request.tituloId(), request.paragemEntradaId(), request.viagemVeiculoId()));
    }

    @PutMapping({"/utilizador/{id}/terminar", "/user/{id}/end"})
    public ResponseEntity<ViagemDTO> terminarViagem(@PathVariable Long id, @RequestBody TerminarViagemRequest request) {
        return ResponseEntity.ok(viagemService.terminarViagem(id, request.paragemSaidaId()));
    }

    @GetMapping({"/veiculo", "/vehicle"})
    public ResponseEntity<List<ViagemVeiculoDTO>> getAllViagensVeiculo() {
        return ResponseEntity.ok(viagemService.getAllViagensVeiculo());
    }

    @GetMapping({"/veiculo/{id}", "/vehicle/{id}"})
    public ResponseEntity<ViagemVeiculoDTO> getViagemVeiculoById(@PathVariable Long id) {
        return ResponseEntity.ok(viagemService.getViagemVeiculoById(id));
    }

    @GetMapping({"/veiculo/por-veiculo/{veiculoId}", "/vehicle/by-vehicle/{veiculoId}"})
    public ResponseEntity<List<ViagemVeiculoDTO>> getViagensByVeiculo(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(viagemService.getViagensByVeiculo(veiculoId));
    }

    @GetMapping({"/veiculo/por-trajeto/{trajetoId}", "/vehicle/by-route/{trajetoId}"})
    public ResponseEntity<List<ViagemVeiculoDTO>> getViagensByTrajeto(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(viagemService.getViagensByTrajeto(trajetoId));
    }

    @PostMapping({"/veiculo", "/vehicle"})
    public ResponseEntity<ViagemVeiculoDTO> createViagemVeiculo(@RequestBody CreateViagemVeiculoRequest viagemVeiculo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(viagemService.createViagemVeiculo(viagemVeiculo));
    }

    @GetMapping("/veiculo/{viagemVeiculoId}/paragem-atual")
    public ResponseEntity<ParagemAtualDTO> getParagemAtual(@PathVariable Long viagemVeiculoId) {
        return ResponseEntity.ok(viagemService.getParagemAtual(viagemVeiculoId));
    }

    @GetMapping("/veiculo/{viagemVeiculoId}/{paragemId}/zona_min_max")
    public ResponseEntity<ZonaMinMaxDTO> getZonaMinMax(@PathVariable Long viagemVeiculoId,
                                                       @PathVariable Long paragemId) {
        return ResponseEntity.ok(viagemService.getZonaMinMax(viagemVeiculoId, paragemId));
    }
}
