package pt.notub.trip.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.trip.service.ViagemService;
import pt.notub.trip.dto.CreateViagemVeiculoRequest;
import pt.notub.trip.dto.IniciarViagemRequest;
import pt.notub.trip.dto.TerminarViagemRequest;
import pt.notub.trip.dto.ViagemDTO;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.trip.entity.ViagemVeiculo;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
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
    public ResponseEntity<?> getParagemAtual(@PathVariable Long viagemVeiculoId) {
        Optional<ViagemVeiculo> viagemOpt = viagemService.getViagemVeiculoById(viagemVeiculoId);
        
        if (viagemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ViagemVeiculo viagem = viagemOpt.get();
        LocalTime agora = LocalTime.now();

        List<PontosDePassagem> pontos = viagem.getTrajeto()
            .getPontosDePassagem()
            .stream()
            .filter(p -> p.getHoraChegada() != null && p.getParagem() != null)
            .sorted(Comparator.comparingInt(PontosDePassagem::getOrdem))
            .toList();

        if (pontos.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LocalTime inicio = pontos.get(0).getHoraChegada();
        LocalTime fim = pontos.get(pontos.size() - 1).getHoraChegada();

        System.out.println("Hora atual: " + agora);
        System.out.println("Início da viagem: " + inicio);
        System.out.println("Fim da viagem: " + fim);

        if (agora.isBefore(inicio) || agora.isAfter(fim)) {
            return ResponseEntity.status(409).body(Map.of("Error", "Viagem não está a decorrer neste momento"));
        }

        PontosDePassagem maisProximo = pontos.stream()
            .min(Comparator.comparingLong(p ->
                Math.abs(ChronoUnit.MINUTES.between(p.getHoraChegada(), agora))
            ))
            .orElse(null);

        if (maisProximo == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("paragemId", maisProximo.getParagem().getId()));
    }

    // Função que me vai dar a zona maxima e mínima de uma determinada viagem
    @GetMapping("/veiculo/{viagemVeiculoId}/{paragemId}/zona_min_max")
    public ResponseEntity<?> getZonaMinMax(@PathVariable Long viagemVeiculoId, @PathVariable Long paragemId) {
        Optional<ViagemVeiculo> viagemOpt = viagemService.getViagemVeiculoById(viagemVeiculoId);
        
        if (viagemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ViagemVeiculo viagem = viagemOpt.get();

        List<PontosDePassagem> todosOsPontos = viagem.getTrajeto()
            .getPontosDePassagem()
            .stream()
            .filter(p -> p.getParagem() != null)
            .sorted(Comparator.comparingInt(PontosDePassagem::getOrdem))
            .toList();

        if (todosOsPontos.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        int indiceParagemAtual = -1;
        for (int i = 0; i < todosOsPontos.size(); i++) {
            if (todosOsPontos.get(i).getParagem().getId().equals(paragemId)) {
                indiceParagemAtual = i;
                break;
            }
        }

        if (indiceParagemAtual == -1) {
            return ResponseEntity.badRequest().body(Map.of("Error", "A paragem especificada não pertence ao trajeto desta viagem."));
        }

        List<PontosDePassagem> pontosRestantes = todosOsPontos.subList(indiceParagemAtual, todosOsPontos.size());
        
        int zonaMin = pontosRestantes.stream()
        .mapToInt(p -> p.getParagem().getZona().getNum())
        .min()
        .orElse(0);
        
        int zonaMax = pontosRestantes.stream()
        .mapToInt(p -> p.getParagem().getZona().getNum())
        .max()
        .orElse(0);

        for (int i = indiceParagemAtual; i < todosOsPontos.size(); i++) {
            System.out.println("Paragem: " + todosOsPontos.get(i).getParagem().getNome() + ", Zona: " + todosOsPontos.get(i).getParagem().getZona().getNum());
        }

        return ResponseEntity.ok(Map.of("zonaMin", zonaMin, "zonaMax", zonaMax));
    }
}
