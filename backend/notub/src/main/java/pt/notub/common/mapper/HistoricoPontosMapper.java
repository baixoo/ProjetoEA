package pt.notub.common.mapper;

import pt.notub.models.HistoricoPontos;
import pt.notub.points.dto.HistoricoPontosDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class HistoricoPontosMapper {

    private HistoricoPontosMapper() {}

    public static HistoricoPontosDTO toDTO(HistoricoPontos hp) {
        if (hp == null) return null;
        HistoricoPontosDTO dto = new HistoricoPontosDTO();
        dto.setId(hp.getId());
        dto.setPontos(hp.getPontos());
        dto.setTipo(hp.getTipo());
        dto.setDescricao(hp.getDescricao());
        dto.setDataHora(hp.getDataHora());
        return dto;
    }

    public static List<HistoricoPontosDTO> toDTOList(List<HistoricoPontos> historico) {
        if (historico == null) return null;
        return historico.stream().map(HistoricoPontosMapper::toDTO).collect(Collectors.toList());
    }
}
