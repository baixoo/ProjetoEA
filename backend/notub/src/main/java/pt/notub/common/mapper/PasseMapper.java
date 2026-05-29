package pt.notub.common.mapper;

import pt.notub.models.Passe;
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
        dto.setZonas(ZonaMapper.toDTOList(p.getZonas()));
        return dto;
    }
}
