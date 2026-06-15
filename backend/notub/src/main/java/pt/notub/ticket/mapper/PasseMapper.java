package pt.notub.ticket.mapper;

import pt.notub.zone.mapper.ZonaMapper;

import pt.notub.ticket.entity.Passe;
import pt.notub.ticket.dto.PasseDTO;

public final class PasseMapper {

    private PasseMapper() {}

    public static PasseDTO toDTO(Passe p) {
        if (p == null) return null;
        PasseDTO dto = new PasseDTO();
        dto.setId(p.getId());
        dto.setInicio(p.getInicio());
        dto.setFim(p.getFim());
        dto.setModalidade(p.getModalidade());
        dto.setZona(ZonaMapper.toDTO(p.getZona()));
        dto.setTipo(p.getTipo());
        return dto;
    }
}
