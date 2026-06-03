package pt.notub.common.mapper;

import pt.notub.models.Trajeto;
import pt.notub.network.dto.LinhaSummaryDTO;
import pt.notub.network.dto.TrajetoDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class TrajetoMapper {

    private TrajetoMapper() {}

    public static TrajetoDTO toDTO(Trajeto t) {
        if (t == null) return null;
        TrajetoDTO dto = new TrajetoDTO();
        dto.setId(t.getId());
        dto.setDirecao(t.getDirecao());
        if (t.getLinha() != null) {
            dto.setLinha(new LinhaSummaryDTO(t.getLinha().getId(), t.getLinha().getNome()));
        }
        dto.setPontosDePassagem(PontoPassagemMapper.toDTOList(t.getPontosDePassagem()));
        return dto;
    }

    public static List<TrajetoDTO> toDTOList(List<Trajeto> trajetos) {
        if (trajetos == null) return null;
        return trajetos.stream().map(TrajetoMapper::toDTO).collect(Collectors.toList());
    }
}
