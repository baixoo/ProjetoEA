package pt.notub.common.mapper;

import pt.notub.models.Bilhete;
import pt.notub.ticket.dto.BilheteDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class BilheteMapper {

    private BilheteMapper() {}

    public static BilheteDTO toDTO(Bilhete b) {
        if (b == null) return null;
        BilheteDTO dto = new BilheteDTO();
        dto.setId(b.getId());
        dto.setUsado(b.isUsado());
        dto.setZona(ZonaMapper.toDTO(b.getZona()));
        return dto;
    }

    public static List<BilheteDTO> toDTOList(List<Bilhete> bilhetes) {
        if (bilhetes == null) return null;
        return bilhetes.stream().map(BilheteMapper::toDTO).collect(Collectors.toList());
    }
}
