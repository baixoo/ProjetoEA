package pt.notub.common.mapper;

import pt.notub.models.ViagemUtilizador;
import pt.notub.models.ViagemVeiculo;
import pt.notub.trip.dto.ViagemDTO;
import pt.notub.trip.dto.ViagemVeiculoDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class ViagemMapper {

    private ViagemMapper() {}

    public static ViagemDTO toDTO(ViagemUtilizador vu) {
        if (vu == null) return null;
        ViagemDTO dto = new ViagemDTO();
        dto.setId(vu.getId());
        dto.setInicio(vu.getInicio());
        dto.setFim(vu.getFim());
        dto.setEstado(vu.getEstado());
        dto.setParagemEntrada(ParagemMapper.toDTO(vu.getParagemEntrada()));
        dto.setParagemSaida(ParagemMapper.toDTO(vu.getParagemSaida()));
        dto.setViagemVeiculo(ViagemMapper.toVVDTO(vu.getViagemVeiculo()));
        return dto;
    }

    public static ViagemVeiculoDTO toVVDTO(ViagemVeiculo vv) {
        if (vv == null) return null;

        ViagemVeiculoDTO dto = new ViagemVeiculoDTO();
        
        dto.setId(vv.getId());
        dto.setTrajeto(TrajetoMapper.toDTO(vv.getTrajeto()));
        dto.setVeiculo(VeiculoMapper.toDTO(vv.getVeiculo())); 
        
        dto.setData(vv.getData());
        dto.setStartTime(vv.getStartTime());
        dto.setFinishTime(vv.getFinishTime());
        dto.setTripId(vv.getTripId());

        return dto;
    }

    public static List<ViagemDTO> toDTOList(List<ViagemUtilizador> viagens) {
        if (viagens == null) return null;
        return viagens.stream().map(ViagemMapper::toDTO).collect(Collectors.toList());
    }

    public static ViagemVeiculoDTO toVeiculoDTO(ViagemVeiculo vv) {
        if (vv == null) return null;
        ViagemVeiculoDTO dto = new ViagemVeiculoDTO();
        dto.setId(vv.getId());
        dto.setTripId(vv.getTripId());
        dto.setVeiculo(VeiculoMapper.toDTO(vv.getVeiculo()));
        dto.setTrajeto(TrajetoMapper.toDTO(vv.getTrajeto()));
        return dto;
    }

    public static List<ViagemVeiculoDTO> toVeiculoDTOList(List<ViagemVeiculo> viagens) {
        if (viagens == null) return null;
        return viagens.stream().map(ViagemMapper::toVeiculoDTO).collect(Collectors.toList());
    }
}
