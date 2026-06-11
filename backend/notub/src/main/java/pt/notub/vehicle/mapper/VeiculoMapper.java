package pt.notub.vehicle.mapper;

import pt.notub.vehicle.entity.Veiculo;
import pt.notub.network.dto.PointDTO;
import pt.notub.vehicle.dto.VeiculoDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class VeiculoMapper {

    private VeiculoMapper() {}

    public static VeiculoDTO toDTO(Veiculo v) {
        if (v == null) return null;
        VeiculoDTO dto = new VeiculoDTO();
        dto.setId(v.getId());
        dto.setMatricula(v.getMatricula());
        dto.setnLugares(v.getnLugares());
        dto.setLotacaoAtual(v.getLotacaoAtual());
        dto.setTempoAtraso(v.getTempoAtraso());
        dto.setLocalizacaoAtual(toPointDTO(v.getLocalizacaoAtual()));
        dto.setTipo(v.getClass().getSimpleName());
        if (v.getLinha() != null) {
            dto.setLinhaId(v.getLinha().getId());
            dto.setLinhaNome(v.getLinha().getNome());
        }
        return dto;
    }

    public static List<VeiculoDTO> toDTOList(List<Veiculo> veiculos) {
        if (veiculos == null) return null;
        return veiculos.stream().map(VeiculoMapper::toDTO).collect(Collectors.toList());
    }

    private static PointDTO toPointDTO(pt.notub.vehicle.entity.Point p) {
        if (p == null) return null;
        return new PointDTO(p.getLatitude(), p.getLongitude());
    }
}
