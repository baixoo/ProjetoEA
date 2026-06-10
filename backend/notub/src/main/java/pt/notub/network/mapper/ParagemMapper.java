package pt.notub.network.mapper;

import pt.notub.network.entity.Paragem;
import pt.notub.vehicle.entity.Point;
import pt.notub.network.dto.ParagemDTO;
import pt.notub.network.dto.PointDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class ParagemMapper {

    private ParagemMapper() {}

    public static ParagemDTO toDTO(Paragem p) {
        if (p == null) return null;
        ParagemDTO dto = new ParagemDTO();
        dto.setId(p.getId());
        dto.setNome(p.getNome());
        dto.setLocalizacao(toPointDTO(p.getLocalizacao()));
        if (p.getZona() != null) {
            dto.setZonaNum(p.getZona().getNum());
            dto.setZonaNome(p.getZona().getNome());
        }
        return dto;
    }

    public static List<ParagemDTO> toDTOList(List<Paragem> paragens) {
        if (paragens == null) return null;
        return paragens.stream().map(ParagemMapper::toDTO).collect(Collectors.toList());
    }

    static PointDTO toPointDTO(Point p) {
        if (p == null) return null;
        return new PointDTO(p.getLatitude(), p.getLongitude());
    }
}
