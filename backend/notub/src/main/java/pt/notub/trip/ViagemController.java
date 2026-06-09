package pt.notub.trip;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.ViagemMapper;
import pt.notub.models.Utilizador;
import pt.notub.models.ViagemUtilizador;
import pt.notub.models.ViagemVeiculo;
import pt.notub.models.PontosDePassagem;
import pt.notub.security.AuthenticatedUser;
import pt.notub.trip.ViagemService;
import pt.notub.trip.dto.ViagemDTO;
import pt.notub.trip.dto.ViagemVeiculoDTO;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;

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
