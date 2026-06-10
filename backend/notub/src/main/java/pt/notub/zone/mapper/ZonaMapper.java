package pt.notub.zone.mapper;

import pt.notub.zone.entity.Zona;
import pt.notub.zone.dto.ZonaDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class ZonaMapper {

    private ZonaMapper() {}

    public static ZonaDTO toDTO(Zona zona) {
        if (zona == null) return null;
        return new ZonaDTO(zona.getId(), zona.getNum(), zona.getNome());
    }

    public static List<ZonaDTO> toDTOList(List<Zona> zonas) {
        if (zonas == null) return null;
        return zonas.stream().map(ZonaMapper::toDTO).collect(Collectors.toList());
    }
}
