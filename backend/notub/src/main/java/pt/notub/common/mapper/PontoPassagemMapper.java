package pt.notub.common.mapper;

import pt.notub.models.PontosDePassagem;
import pt.notub.network.dto.PontoPassagemDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class PontoPassagemMapper {

    private PontoPassagemMapper() {}

    public static PontoPassagemDTO toDTO(PontosDePassagem pdp) {
        if (pdp == null) return null;
        PontoPassagemDTO dto = new PontoPassagemDTO();
        dto.setId(pdp.getId());
        dto.setOrdem(pdp.getOrdem());
        dto.setHoraChegada(pdp.getHoraChegada());
        dto.setTempoDesdeInicio(pdp.getTempoDesdeInicio());
        dto.setParagem(ParagemMapper.toDTO(pdp.getParagem()));
        return dto;
    }

    public static List<PontoPassagemDTO> toDTOList(List<PontosDePassagem> pontos) {
        if (pontos == null) return null;
        return pontos.stream().map(PontoPassagemMapper::toDTO).collect(Collectors.toList());
    }
}
