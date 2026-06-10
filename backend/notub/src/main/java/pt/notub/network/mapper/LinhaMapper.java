package pt.notub.network.mapper;

import pt.notub.network.entity.Linha;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.dto.LinhaDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class LinhaMapper {

    private LinhaMapper() {}

    public static LinhaDTO toDTO(Linha l, List<Trajeto> trajetos) {
        if (l == null) return null;
        LinhaDTO dto = new LinhaDTO();
        dto.setId(l.getId());
        dto.setNome(l.getNome());
        dto.setIdentificadorServico(l.getIdentificadorServico());
        dto.setTrajetos(TrajetoMapper.toDTOList(trajetos));
        return dto;
    }

    public static LinhaDTO toDTO(Linha l, Map<Long, List<Trajeto>> trajetosByLinha) {
        return toDTO(l, trajetosByLinha.getOrDefault(l.getId(), List.of()));
    }

    public static List<LinhaDTO> toDTOList(List<Linha> linhas, Map<Long, List<Trajeto>> trajetosByLinha) {
        if (linhas == null) return null;
        return linhas.stream().map(l -> toDTO(l, trajetosByLinha)).collect(Collectors.toList());
    }
}
