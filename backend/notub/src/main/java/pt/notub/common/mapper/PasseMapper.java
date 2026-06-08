package pt.notub.common.mapper;

import pt.notub.models.Passe;
import pt.notub.ticket.dto.PasseDTO;
import pt.notub.zone.dto.ZonaDTO;

import java.util.List;
import java.util.stream.Collectors;

public class PasseMapper {

    public static PasseDTO toDTO(Passe passe) {
        if (passe == null) return null;
        PasseDTO dto = new PasseDTO();
        dto.setId(passe.getId());
        dto.setInicio(passe.getInicio());
        dto.setFim(passe.getFim());
        dto.setModalidade(passe.getModalidade());
        if (passe.getZona() != null) {
            ZonaDTO zonaDTO = new ZonaDTO();
            zonaDTO.setId(passe.getZona().getId());
            zonaDTO.setNum(passe.getZona().getNum());
            dto.setZona(zonaDTO);
        }
        return dto;
    }

    public static List<PasseDTO> toDTOList(List<Passe> passes) {
        return passes.stream().map(PasseMapper::toDTO).collect(Collectors.toList());
    }
}
