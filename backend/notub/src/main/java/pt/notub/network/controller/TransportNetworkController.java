package pt.notub.network.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.network.dto.HorarioDTO;
import pt.notub.network.dto.HorarioParagemDTO;
import pt.notub.network.dto.LinhaDTO;
import pt.notub.network.dto.ParagemDTO;
import pt.notub.network.dto.ParagemProximasPassagensDTO;
import pt.notub.network.dto.PontoPassagemDTO;
import pt.notub.network.dto.ProximoPasseDTO;
import pt.notub.network.dto.RotaDTO;
import pt.notub.network.dto.TrajetoDTO;
import pt.notub.network.mapper.LinhaMapper;
import pt.notub.network.mapper.ParagemMapper;
import pt.notub.network.mapper.PontoPassagemMapper;
import pt.notub.network.mapper.TrajetoMapper;
import pt.notub.network.service.RoutePlanningService;
import pt.notub.network.service.TransportNetworkService;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/network")
public class TransportNetworkController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final TransportNetworkService networkService;
    private final RoutePlanningService routePlanningService;

    public TransportNetworkController(TransportNetworkService networkService, RoutePlanningService routePlanningService) {
        this.networkService = networkService;
        this.routePlanningService = routePlanningService;
    }

    @GetMapping({"/paragens", "/stops"})
    public ResponseEntity<List<ParagemDTO>> getParagens() {
        return ResponseEntity.ok(ParagemMapper.toDTOList(networkService.getAllParagens()));
    }

    @GetMapping({"/paragens/{id}", "/stops/{id}"})
    public ResponseEntity<ParagemDTO> getParagemById(@PathVariable Long id) {
        return networkService.getParagemById(id)
                .map(ParagemMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/linhas", "/lines"})
    public ResponseEntity<List<LinhaDTO>> getLinhas() {
        return ResponseEntity.ok(LinhaMapper.toDTOList(networkService.getAllLinhas(), networkService.getTrajetosByLinhaMap()));
    }

    @GetMapping({"/linhas/{id}", "/lines/{id}"})
    public ResponseEntity<LinhaDTO> getLinhaById(@PathVariable Long id) {
        return networkService.getLinhaById(id)
                .map(l -> LinhaMapper.toDTO(l, networkService.getTrajetosForLinha(id)))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/trajetos", "/routes"})
    public ResponseEntity<List<TrajetoDTO>> getTrajetos() {
        return ResponseEntity.ok(TrajetoMapper.toDTOList(networkService.getAllTrajetos()));
    }

    @GetMapping({"/trajetos/{id}", "/routes/{id}"})
    public ResponseEntity<TrajetoDTO> getTrajetoById(@PathVariable Long id) {
        return networkService.getTrajetoById(id)
                .map(TrajetoMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/linhas/{linhaId}/trajetos", "/lines/{linhaId}/routes"})
    public ResponseEntity<List<TrajetoDTO>> getTrajetosByLinha(@PathVariable Long linhaId) {
        return ResponseEntity.ok(TrajetoMapper.toDTOList(networkService.getTrajetosByLinha(linhaId)));
    }

    @GetMapping({"/trajetos/{trajetoId}/pontos", "/routes/{trajetoId}/points"})
    public ResponseEntity<List<PontoPassagemDTO>> getPontosByTrajeto(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(PontoPassagemMapper.toDTOList(networkService.getPontosByTrajeto(trajetoId)));
    }

    @GetMapping("/route")
    public ResponseEntity<List<RotaDTO>> planearRota(
            @RequestParam Long from,
            @RequestParam Long to,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String day) {

        LocalTime queryTime = time != null ? LocalTime.parse(time, TIME_FMT) : LocalTime.now();
        DayOfWeek dayOfWeek = day != null ? DayOfWeek.valueOf(day.toUpperCase()) : java.time.LocalDate.now().getDayOfWeek();

        List<RotaDTO> rotas = routePlanningService.planearRota(from, to, queryTime, dayOfWeek);
        if (rotas.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rotas);
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

        LocalTime queryTime = time != null ? LocalTime.parse(time, TIME_FMT) : LocalTime.now();
        DayOfWeek dayOfWeek = day != null ? DayOfWeek.valueOf(day.toUpperCase()) : java.time.LocalDate.now().getDayOfWeek();

        return ResponseEntity.ok(routePlanningService.findProximosPasses(trajetoId, paragemId, queryTime, dayOfWeek));
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

        LocalTime queryTime = time != null ? LocalTime.parse(time, TIME_FMT) : LocalTime.now();
        DayOfWeek dayOfWeek = day != null ? DayOfWeek.valueOf(day.toUpperCase()) : java.time.LocalDate.now().getDayOfWeek();

        ParagemProximasPassagensDTO response = routePlanningService.findProximasPassagensPorParagem(paragemId, queryTime, dayOfWeek);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/linhas/{linhaId}/horarios", "/lines/{linhaId}/schedules"})
    public ResponseEntity<List<HorarioDTO>> getHorarios(
            @PathVariable Long linhaId,
            @RequestParam(defaultValue = "UTEIS") String serviceId) {
        return ResponseEntity.ok(routePlanningService.findHorarios(linhaId, serviceId));
    }

    @GetMapping({"/linhas/{linhaId}/horarios-paragens", "/lines/{linhaId}/stop-schedules"})
    public ResponseEntity<List<HorarioParagemDTO>> getHorariosPorParagem(
            @PathVariable Long linhaId,
            @RequestParam(defaultValue = "UTEIS") String serviceId) {
        return ResponseEntity.ok(routePlanningService.findHorariosPorParagem(linhaId, serviceId));
    }
}
