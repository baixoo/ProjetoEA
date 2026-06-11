package pt.notub.network.mapper;

import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.dto.PontoPassagemDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class PontoPassagemMapper {

    private PontoPassagemMapper() {}

    public static PontoPassagemDTO toDTO(PontosDePassagem pdp) {
        if (pdp == null) return null;
        PontoPassagemDTO dto = new PontoPassagemDTO();
        dto.setId(pdp.getId());
        if (pdp.getTrajeto() != null) {
            dto.setTrajetoId(pdp.getTrajeto().getId());
        }
        dto.setOrdem(pdp.getOrdem());
        dto.setParagem(ParagemMapper.toDTO(pdp.getParagem()));
        return dto;
    }

    public static List<PontoPassagemDTO> toDTOList(List<PontosDePassagem> pontos) {
        if (pontos == null) return null;
        return pontos.stream().map(PontoPassagemMapper::toDTO).collect(Collectors.toList());
    }
}
