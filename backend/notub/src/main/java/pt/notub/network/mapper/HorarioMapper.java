package pt.notub.network.mapper;

import pt.notub.network.dto.HorarioItemDTO;
import pt.notub.network.entity.Horario;

import java.util.List;
import java.util.stream.Collectors;

public final class HorarioMapper {
    private HorarioMapper() {}

    public static HorarioItemDTO toItemDTO(Horario horario) {
        if (horario == null) return null;
        HorarioItemDTO dto = new HorarioItemDTO();
        dto.setId(horario.getId());
        dto.setHora(horario.getHora());
        dto.setGtfsTripId(horario.getGtfsTripId());
        if (horario.getPontoPassagem() != null) {
            dto.setPontoPassagemId(horario.getPontoPassagem().getId());
        }
        dto.setServico(ServicoMapper.toDTO(horario.getServico()));
        return dto;
    }

    public static List<HorarioItemDTO> toItemDTOList(List<Horario> horarios) {
        if (horarios == null) return null;
        return horarios.stream().map(HorarioMapper::toItemDTO).collect(Collectors.toList());
    }
}
