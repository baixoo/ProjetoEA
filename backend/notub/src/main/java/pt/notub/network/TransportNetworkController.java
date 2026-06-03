package pt.notub.network;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.*;
import pt.notub.network.dto.RotaDTO;
import pt.notub.models.*;
import pt.notub.network.dto.*;
import pt.notub.network.RoutePlanningService;
import pt.notub.network.TransportNetworkService;

import java.util.List;

@RestController
@RequestMapping("/api/network")
public class TransportNetworkController {

    private final TransportNetworkService networkService;
    private final RoutePlanningService routePlanningService;

    public TransportNetworkController(TransportNetworkService networkService, RoutePlanningService routePlanningService) {
        this.networkService = networkService;
        this.routePlanningService = routePlanningService;
    }

    @GetMapping("/paragens")
    public ResponseEntity<List<ParagemDTO>> getParagens() {
        return ResponseEntity.ok(ParagemMapper.toDTOList(networkService.getAllParagens()));
    }

    @GetMapping("/paragens/{id}")
    public ResponseEntity<ParagemDTO> getParagemById(@PathVariable Long id) {
        return networkService.getParagemById(id)
                .map(ParagemMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/linhas")
    public ResponseEntity<List<LinhaDTO>> getLinhas() {
        return ResponseEntity.ok(LinhaMapper.toDTOList(networkService.getAllLinhas(), networkService.getTrajetosByLinhaMap()));
    }

    @GetMapping("/linhas/{id}")
    public ResponseEntity<LinhaDTO> getLinhaById(@PathVariable Long id) {
        return networkService.getLinhaById(id)
                .map(l -> LinhaMapper.toDTO(l, networkService.getTrajetosForLinha(id)))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/trajetos")
    public ResponseEntity<List<TrajetoDTO>> getTrajetos() {
        return ResponseEntity.ok(TrajetoMapper.toDTOList(networkService.getAllTrajetos()));
    }

    @GetMapping("/trajetos/{id}")
    public ResponseEntity<TrajetoDTO> getTrajetoById(@PathVariable Long id) {
        return networkService.getTrajetoById(id)
                .map(TrajetoMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/linhas/{linhaId}/trajetos")
    public ResponseEntity<List<TrajetoDTO>> getTrajetosByLinha(@PathVariable Long linhaId) {
        return ResponseEntity.ok(TrajetoMapper.toDTOList(networkService.getTrajetosByLinha(linhaId)));
    }

    @GetMapping("/trajetos/{trajetoId}/pontos")
    public ResponseEntity<List<PontoPassagemDTO>> getPontosByTrajeto(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(PontoPassagemMapper.toDTOList(networkService.getPontosByTrajeto(trajetoId)));
    }

    @GetMapping("/route")
    public ResponseEntity<RotaDTO> planearRota(
            @RequestParam Long from,
            @RequestParam Long to) {
        RotaDTO rota = routePlanningService.planearRota(from, to);
        if (rota == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rota);
    }
}
