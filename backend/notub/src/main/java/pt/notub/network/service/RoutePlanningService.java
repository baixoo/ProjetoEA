package pt.notub.network.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.network.dto.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RoutePlanningService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final RoutingIndexService indexService;
    private final RouteSearchEngine searchEngine;
    private final ScheduleQueryService scheduleQueryService;

    public RoutePlanningService(RoutingIndexService indexService,
                                RouteSearchEngine searchEngine,
                                ScheduleQueryService scheduleQueryService) {
        this.indexService = indexService;
        this.searchEngine = searchEngine;
        this.scheduleQueryService = scheduleQueryService;
    }

    public static String normalizeStopGroupKey(String stopName) {
        return RoutingIndexService.normalizeStopGroupKey(stopName);
    }

    public List<RotaDTO> planearRota(Long origemId, Long destinoId) {
        return planearRota(origemId, destinoId, LocalTime.now(), LocalDate.now().getDayOfWeek());
    }

    public List<RotaDTO> planearRota(Long origemId, Long destinoId, String time, String day) {
        return planearRota(origemId, destinoId, parseTimeOrDefault(time), parseDayOrDefault(day));
    }

    public List<RotaDTO> planearRota(Long origemId, Long destinoId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        String serviceName = resolveServiceId(dayOfWeek);
        RoutingIndexService.ServiceRoutingIndex index = indexService.getRoutingIndex(serviceName);
        int queryMinutes = queryTime.getHour() * 60 + queryTime.getMinute();
        return searchEngine.search(origemId, destinoId, queryMinutes, index);
    }

    public List<ProximoPasseDTO> findProximosPasses(Long trajetoId, Long paragemId, String time, String day) {
        return scheduleQueryService.findProximosPasses(trajetoId, paragemId, time, day);
    }

    public List<ProximoPasseDTO> findProximosPasses(Long trajetoId, Long paragemId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        return scheduleQueryService.findProximosPasses(trajetoId, paragemId, queryTime, dayOfWeek);
    }

    public ParagemProximasPassagensDTO findProximasPassagensPorParagem(Long paragemId, String time, String day) {
        return scheduleQueryService.findProximasPassagensPorParagem(paragemId, time, day);
    }

    public ParagemProximasPassagensDTO findProximasPassagensPorParagem(Long paragemId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        return scheduleQueryService.findProximasPassagensPorParagem(paragemId, queryTime, dayOfWeek);
    }

    public List<HorarioDTO> findHorarios(Long linhaId, String serviceId) {
        return scheduleQueryService.findHorarios(linhaId, serviceId);
    }

    public List<HorarioParagemDTO> findHorariosPorParagem(Long linhaId, String serviceId) {
        return scheduleQueryService.findHorariosPorParagem(linhaId, serviceId);
    }

    private String resolveServiceId(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SATURDAY -> "SAB";
            case SUNDAY -> "DOM";
            default -> "UTEIS";
        };
    }

    private LocalTime parseTimeOrDefault(String time) {
        if (time == null || time.isBlank()) {
            return LocalTime.now();
        }
        try {
            return LocalTime.parse(time, TIME_FMT);
        } catch (Exception e) {
            throw new PedidoInvalidoException("Hora invalida");
        }
    }

    private DayOfWeek parseDayOrDefault(String day) {
        if (day == null || day.isBlank()) {
            return LocalDate.now().getDayOfWeek();
        }
        try {
            return DayOfWeek.valueOf(day.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PedidoInvalidoException("Dia invalido");
        }
    }
}
