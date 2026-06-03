package pt.notub.common.mapper;

import pt.notub.models.Tarifa;
import pt.notub.tariff.dto.TarifaDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class TarifaMapper {

    private TarifaMapper() {}

    public static TarifaDTO toDTO(Tarifa t) {
        if (t == null) return null;
        TarifaDTO dto = new TarifaDTO();
        dto.setId(t.getId());
        dto.setValor(t.getValor());
        dto.setTipoUtilizador(t.getTipoUtilizador());
        dto.setModalidade(t.getModalidade());
        dto.setNrZonas(t.getNrZonas());
        return dto;
    }

    public static List<TarifaDTO> toDTOList(List<Tarifa> tarifas) {
        if (tarifas == null) return null;
        return tarifas.stream().map(TarifaMapper::toDTO).collect(Collectors.toList());
    }
}
