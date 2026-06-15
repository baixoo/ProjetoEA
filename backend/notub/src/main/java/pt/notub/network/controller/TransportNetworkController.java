package pt.notub.network.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.network.dto.HorarioDTO;
import pt.notub.network.dto.HorarioParagemDTO;
import pt.notub.network.dto.LinhaDTO;
import pt.notub.network.dto.LinhaSummaryDTO;
import pt.notub.network.dto.ParagemDTO;
import pt.notub.network.dto.ParagemProximasPassagensDTO;
import pt.notub.network.dto.PontoPassagemDTO;
import pt.notub.network.dto.ProximoPasseDTO;
import pt.notub.network.dto.RotaDTO;
import pt.notub.network.dto.TrajetoDTO;
import pt.notub.network.service.RoutePlanningService;
import pt.notub.network.service.TransportNetworkService;

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

    @GetMapping({"/paragens", "/stops"})
    public ResponseEntity<List<ParagemDTO>> getParagens() {
        return ResponseEntity.ok(networkService.getAllParagens());
    }

    @GetMapping({"/paragens/{id}", "/stops/{id}"})
    public ResponseEntity<ParagemDTO> getParagemById(@PathVariable Long id) {
        return ResponseEntity.ok(networkService.getParagemById(id));
    }

    @GetMapping({"/linhas", "/lines"})
    public ResponseEntity<List<LinhaDTO>> getLinhas() {
        return ResponseEntity.ok(networkService.getAllLinhas());
    }

    @GetMapping({"/linhas/resumo", "/lines/summary"})
    public ResponseEntity<List<LinhaSummaryDTO>> getLinhaSummaries() {
        return ResponseEntity.ok(networkService.getLinhaSummaries());
    }

    @GetMapping({"/linhas/{id}", "/lines/{id}"})
    public ResponseEntity<LinhaDTO> getLinhaById(@PathVariable Long id) {
        return ResponseEntity.ok(networkService.getLinhaById(id));
    }

    @GetMapping({"/trajetos", "/routes"})
    public ResponseEntity<List<TrajetoDTO>> getTrajetos() {
        return ResponseEntity.ok(networkService.getAllTrajetos());
    }

    @GetMapping({"/trajetos/{id}", "/routes/{id}"})
    public ResponseEntity<TrajetoDTO> getTrajetoById(@PathVariable Long id) {
        return ResponseEntity.ok(networkService.getTrajetoById(id));
    }

    @GetMapping({"/linhas/{linhaId}/trajetos", "/lines/{linhaId}/routes"})
    public ResponseEntity<List<TrajetoDTO>> getTrajetosByLinha(@PathVariable Long linhaId) {
        return ResponseEntity.ok(networkService.getTrajetosByLinha(linhaId));
    }

    @GetMapping({"/trajetos/{trajetoId}/pontos", "/routes/{trajetoId}/points"})
    public ResponseEntity<List<PontoPassagemDTO>> getPontosByTrajeto(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(networkService.getPontosByTrajeto(trajetoId));
    }

    @GetMapping("/route")
    public ResponseEntity<List<RotaDTO>> planearRota(
            @RequestParam Long from,
            @RequestParam Long to,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String day) {
        return ResponseEntity.ok(routePlanningService.planearRota(from, to, time, day));
    }

    @GetMapping({
            "/trajetos/{trajetoId}/proximos-passes",
            "/trajetos/{trajetoId}/proximas-passagens",
            "/routes/{trajetoId}/next-passes"
    })
    public ResponseEntity<List<ProximoPasseDTO>> getProximosPasses(
            @PathVariable Long trajetoId,
            @RequestParam Long paragemId,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String day) {
        return ResponseEntity.ok(routePlanningService.findProximosPasses(trajetoId, paragemId, time, day));
    }

    @GetMapping({
            "/paragens/{paragemId}/proximos-passes",
            "/paragens/{paragemId}/proximas-passagens",
            "/stops/{paragemId}/next-passes"
    })
    public ResponseEntity<ParagemProximasPassagensDTO> getProximasPassagensPorParagem(
            @PathVariable Long paragemId,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String day) {
        return ResponseEntity.ok(routePlanningService.findProximasPassagensPorParagem(paragemId, time, day));
    }

    @GetMapping({"/linhas/{linhaId}/horarios", "/lines/{linhaId}/schedules"})
    public ResponseEntity<List<HorarioDTO>> getHorarios(
            @PathVariable Long linhaId,
            @RequestParam(required = false) String serviceId) {
        return ResponseEntity.ok(routePlanningService.findHorarios(linhaId, serviceId));
    }

    @GetMapping({"/linhas/{linhaId}/horarios-paragens", "/lines/{linhaId}/stop-schedules"})
    public ResponseEntity<List<HorarioParagemDTO>> getHorariosPorParagem(
            @PathVariable Long linhaId,
            @RequestParam(required = false) String serviceId) {
        return ResponseEntity.ok(routePlanningService.findHorariosPorParagem(linhaId, serviceId));
    }
}
