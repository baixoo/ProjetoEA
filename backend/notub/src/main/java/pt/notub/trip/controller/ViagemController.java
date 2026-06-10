package pt.notub.trip.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.trip.mapper.ViagemMapper;
import pt.notub.user.entity.Utilizador;
import pt.notub.trip.entity.ViagemUtilizador;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.trip.service.ViagemService;
import pt.notub.trip.dto.IniciarViagemRequest;
import pt.notub.trip.dto.TerminarViagemRequest;
import pt.notub.trip.dto.ViagemDTO;
import pt.notub.trip.dto.ViagemVeiculoDTO;

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
            @AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(ViagemMapper.toDTOList(viagemService.getViagensByUtilizador(utilizador.getId())));
    }

    @GetMapping({"/utilizador/{id}", "/user/{id}"})
    public ResponseEntity<ViagemDTO> getViagemUtilizadorById(@PathVariable Long id) {
        return viagemService.getViagemUtilizadorById(id)
                .map(ViagemMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping({"/utilizador/iniciar", "/user/start"})
    public ResponseEntity<ViagemDTO> iniciarViagem(@RequestBody IniciarViagemRequest request) {
        ViagemUtilizador viagem = viagemService.iniciarViagem(
                request.tituloId(), request.paragemEntradaId(), request.viagemVeiculoId());
        return ResponseEntity.ok(ViagemMapper.toDTO(viagem));
    }

    @PutMapping({"/utilizador/{id}/terminar", "/user/{id}/end"})
    public ResponseEntity<ViagemDTO> terminarViagem(@PathVariable Long id, @RequestBody TerminarViagemRequest request) {
        ViagemUtilizador viagem = viagemService.terminarViagem(id, request.paragemSaidaId());
        return ResponseEntity.ok(ViagemMapper.toDTO(viagem));
    }

    @GetMapping({"/veiculo", "/vehicle"})
    public ResponseEntity<List<ViagemVeiculoDTO>> getAllViagensVeiculo() {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTOList(viagemService.getAllViagensVeiculo()));
    }

    @GetMapping({"/veiculo/{id}", "/vehicle/{id}"})
    public ResponseEntity<ViagemVeiculoDTO> getViagemVeiculoById(@PathVariable Long id) {
        return viagemService.getViagemVeiculoById(id)
                .map(ViagemMapper::toVeiculoDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/veiculo/por-veiculo/{veiculoId}", "/vehicle/by-vehicle/{veiculoId}"})
    public ResponseEntity<List<ViagemVeiculoDTO>> getViagensByVeiculo(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTOList(viagemService.getViagensByVeiculo(veiculoId)));
    }

    @GetMapping({"/veiculo/por-trajeto/{trajetoId}", "/vehicle/by-route/{trajetoId}"})
    public ResponseEntity<List<ViagemVeiculoDTO>> getViagensByTrajeto(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTOList(viagemService.getViagensByTrajeto(trajetoId)));
    }

    @PostMapping({"/veiculo", "/vehicle"})
    public ResponseEntity<ViagemVeiculoDTO> createViagemVeiculo(@RequestBody ViagemVeiculo viagemVeiculo) {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTO(viagemService.createViagemVeiculo(viagemVeiculo)));
    }
}
