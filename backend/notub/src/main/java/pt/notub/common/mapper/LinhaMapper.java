package pt.notub.common.mapper;

import pt.notub.models.Linha;
import pt.notub.network.dto.LinhaDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class LinhaMapper {

    private LinhaMapper() {}

    public static LinhaDTO toDTO(Linha l) {
        if (l == null) return null;
        LinhaDTO dto = new LinhaDTO();
        dto.setId(l.getId());
        dto.setNome(l.getNome());
        dto.setIdentificadorServico(l.getIdentificadorServico());
        dto.setTrajetos(TrajetoMapper.toDTOList(l.getTrajetos()));
        return dto;
    }

    public static List<LinhaDTO> toDTOList(List<Linha> linhas) {
        if (linhas == null) return null;
        return linhas.stream().map(LinhaMapper::toDTO).collect(Collectors.toList());
    }
}
