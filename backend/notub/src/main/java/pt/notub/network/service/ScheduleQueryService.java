package pt.notub.network.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.dto.*;
import pt.notub.network.entity.*;
import pt.notub.network.mapper.HorarioMapper;
import pt.notub.network.repository.HorarioRepository;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.PontosDePassagemRepository;
import pt.notub.network.repository.TrajetoRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleQueryService {

    private static final int MAX_WAIT_MINUTES = 65;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final HorarioRepository horarioRepo;
    private final ParagemRepository paragemRepo;
    private final TrajetoRepository trajetoRepo;
    private final PontosDePassagemRepository pontosRepo;

    public ScheduleQueryService(HorarioRepository horarioRepo,
                                ParagemRepository paragemRepo,
                                TrajetoRepository trajetoRepo,
                                PontosDePassagemRepository pontosRepo) {
        this.horarioRepo = horarioRepo;
        this.paragemRepo = paragemRepo;
        this.trajetoRepo = trajetoRepo;
        this.pontosRepo = pontosRepo;
    }

    public List<HorarioDTO> findHorarios(Long linhaId, String serviceId) {
        serviceId = normalizeServiceId(serviceId);

        List<Trajeto> trajetos = trajetoRepo.findByLinhaId(linhaId);
        if (trajetos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Linha nao encontrada");
        }

        List<Horario> horarios = horarioRepo.findByLinhaAndServico(linhaId, serviceId);
        Map<Long, Map<String, List<Horario>>> horariosByTrajeto = new LinkedHashMap<>();
        for (Horario horario : horarios) {
            if (horario.getPontoPassagem() == null || horario.getPontoPassagem().getTrajeto() == null) continue;
            horariosByTrajeto
                    .computeIfAbsent(horario.getPontoPassagem().getTrajeto().getId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(horario.getGtfsTripId(), k -> new ArrayList<>())
                    .add(horario);
        }

        List<HorarioDTO> result = new ArrayList<>();
        for (Trajeto t : trajetos) {
            Map<String, List<Horario>> viagens = horariosByTrajeto.getOrDefault(t.getId(), Map.of());
            String destinoFinal = getDestinoFinal(t.getId());

            List<String> partidas = viagens.values().stream()
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0).getHora().format(TIME_FMT))
                    .sorted()
                    .collect(Collectors.toList());

            result.add(new HorarioDTO(t.getId(), t.getDirecao().name(), destinoFinal, partidas));
        }

        return result;
    }

    public List<HorarioParagemDTO> findHorariosPorParagem(Long linhaId, String serviceId) {
        serviceId = normalizeServiceId(serviceId);

        List<Trajeto> trajetos = trajetoRepo.findByLinhaId(linhaId);
        if (trajetos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Linha nao encontrada");
        }

        List<Horario> horarios = horarioRepo.findByLinhaAndServico(linhaId, serviceId);
        Map<Long, Map<Long, List<HorarioItemDTO>>> horariosByTrajeto = new LinkedHashMap<>();
        for (Horario horario : horarios) {
            if (horario.getPontoPassagem() == null || horario.getPontoPassagem().getTrajeto() == null) continue;
            if (horario.getPontoPassagem().getParagem() == null) continue;

            Long trajetoId = horario.getPontoPassagem().getTrajeto().getId();
            Long paragemId = horario.getPontoPassagem().getParagem().getId();
            horariosByTrajeto
                    .computeIfAbsent(trajetoId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(paragemId, k -> new ArrayList<>())
                    .add(HorarioMapper.toItemDTO(horario));
        }

        List<HorarioParagemDTO> result = new ArrayList<>();
        for (Trajeto t : trajetos) {
            List<PontosDePassagem> pontos = pontosRepo.findByTrajetoIdOrderByOrdemAsc(t.getId());
            Map<Long, List<HorarioItemDTO>> horariosPorParagem = horariosByTrajeto.getOrDefault(t.getId(), Map.of());

            HorarioParagemDTO dto = new HorarioParagemDTO();
            dto.setTrajetoId(t.getId());
            dto.setDirecao(t.getDirecao().name());
            dto.setDestinoFinal(getDestinoFinal(t.getId()));
            dto.setLinhaNome(t.getLinha() != null ? t.getLinha().getNome() : "");

            List<HorarioParagemDTO.ParagemHorarioDTO> paragensHorario = new ArrayList<>();
            for (PontosDePassagem p : pontos) {
                if (p.getParagem() == null) {
                    continue;
                }
                HorarioParagemDTO.ParagemHorarioDTO ph = new HorarioParagemDTO.ParagemHorarioDTO();
                ph.setParagemId(p.getParagem().getId());
                ph.setNome(p.getParagem().getNome());
                ph.setOrdem(p.getOrdem());
                List<HorarioItemDTO> stopHorarios = new ArrayList<>(horariosPorParagem.getOrDefault(p.getParagem().getId(), List.of()));
                stopHorarios.sort(Comparator.comparing(HorarioItemDTO::getHora));
                ph.setHorarios(stopHorarios);
                paragensHorario.add(ph);
            }
            dto.setParagens(paragensHorario);
            result.add(dto);
        }

        return result;
    }

    public List<ProximoPasseDTO> findProximosPasses(Long trajetoId, Long paragemId, String time, String day) {
        return findProximosPasses(trajetoId, paragemId, parseTimeOrDefault(time), parseDayOrDefault(day));
    }

    public List<ProximoPasseDTO> findProximosPasses(Long trajetoId, Long paragemId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        requireTrajeto(trajetoId);
        requireParagem(paragemId);

        String serviceId = resolveServiceId(dayOfWeek);
        List<Horario> schedules = horarioRepo.findByTrajetoParagemAndServico(trajetoId, paragemId, serviceId);

        int queryMinutes = queryTime.getHour() * 60 + queryTime.getMinute();
        List<ProximoPasseDTO> result = new ArrayList<>();

        for (Horario h : schedules) {
            int depTOD = h.getHora().getHour() * 60 + h.getHora().getMinute();
            int wait = depTOD - queryMinutes;
            if (wait < 0) wait += 1440;
            if (wait > MAX_WAIT_MINUTES) continue;
            result.add(new ProximoPasseDTO(h.getHora().format(TIME_FMT), wait));
            if (result.size() >= 10) break;
        }

        result.sort(Comparator.comparingInt(ProximoPasseDTO::getEsperaMinutos));
        return result;
    }

    public ParagemProximasPassagensDTO findProximasPassagensPorParagem(Long paragemId, String time, String day) {
        return findProximasPassagensPorParagem(paragemId, parseTimeOrDefault(time), parseDayOrDefault(day));
    }

    public ParagemProximasPassagensDTO findProximasPassagensPorParagem(Long paragemId, LocalTime queryTime, DayOfWeek dayOfWeek) {
        Paragem paragem = paragemRepo.findById(paragemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));

        List<PontosDePassagem> pontos = pontosRepo.findByParagemId(paragemId);

        Map<Long, ParagemProximasPassagensDTO.LinhaProximasPassagensDTO> linhasById = new TreeMap<>();
        for (PontosDePassagem ponto : pontos) {
            Trajeto trajeto = ponto.getTrajeto();
            if (trajeto == null) continue;
            final Trajeto tObj = trajetoRepo.findById(trajeto.getId()).orElse(null);
            if (tObj == null || tObj.getLinha() == null) {
                continue;
            }

            Long trajetoId = tObj.getId();
            ParagemProximasPassagensDTO.TrajetoProximasPassagensDTO trajetoDTO = new ParagemProximasPassagensDTO.TrajetoProximasPassagensDTO();
            trajetoDTO.setTrajetoId(trajetoId);
            trajetoDTO.setDirecao(tObj.getDirecao() != null ? tObj.getDirecao().name() : "");
            trajetoDTO.setDestinoFinal(getDestinoFinal(trajetoId));
            trajetoDTO.setProximosPasses(findProximosPasses(trajetoId, paragemId, queryTime, dayOfWeek));

            ParagemProximasPassagensDTO.LinhaProximasPassagensDTO linhaDTO = linhasById.computeIfAbsent(
                    tObj.getLinha().getId(),
                    id -> {
                        ParagemProximasPassagensDTO.LinhaProximasPassagensDTO dto = new ParagemProximasPassagensDTO.LinhaProximasPassagensDTO();
                        dto.setLinhaId(tObj.getLinha().getId());
                        dto.setLinhaNome(tObj.getLinha().getNome());
                        dto.setTrajetos(new ArrayList<>());
                        return dto;
                    }
            );
            linhaDTO.getTrajetos().add(trajetoDTO);
        }

        ParagemProximasPassagensDTO result = new ParagemProximasPassagensDTO();
        result.setParagemId(paragemId);
        result.setParagemNome(paragem.getNome());
        result.setLinhas(new ArrayList<>(linhasById.values()));
        return result;
    }

    private String getDestinoFinal(Long trajetoId) {
        List<PontosDePassagem> sorted = pontosRepo.findByTrajetoIdOrderByOrdemAsc(trajetoId);
        if (!sorted.isEmpty()) {
            Paragem last = sorted.get(sorted.size() - 1).getParagem();
            return last != null ? last.getNome() : "";
        }
        return "";
    }

    private void requireTrajeto(Long trajetoId) {
        if (!trajetoRepo.existsById(trajetoId)) {
            throw new RecursoNaoEncontradoException("Trajeto nao encontrado");
        }
    }

    private void requireParagem(Long paragemId) {
        if (!paragemRepo.existsById(paragemId)) {
            throw new RecursoNaoEncontradoException("Paragem nao encontrada");
        }
    }

    private String normalizeServiceId(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            return "UTEIS";
        }
        return serviceId.trim().toUpperCase();
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
