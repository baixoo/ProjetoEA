package pt.notub.trip.mapper;

import java.util.List;
import java.util.stream.Collectors;

import pt.notub.network.mapper.ParagemMapper;
import pt.notub.network.mapper.TrajetoMapper;
import pt.notub.ticket.mapper.TituloMapper;
import pt.notub.trip.dto.ViagemDTO;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.trip.entity.ViagemUtilizador;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.vehicle.mapper.VeiculoMapper;

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

        dto.setTitulo(TituloMapper.toDTO(vu.getTitulo()));

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
        // dto.setTripId(vv.getTripId());

        dto.setServiceId(vv.getServiceId());
        dto.setGtfsTripId(vv.getGtfsTripId());
        dto.setHoraPartidaPlaneada(vv.getHoraPartidaPlaneada());

        if (vv.getPontoAtual() != null) {
            dto.setPontoAtualId(vv.getPontoAtual().getId());
        }

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
        // dto.setTripId(vv.getTripId());
        dto.setVeiculo(VeiculoMapper.toDTO(vv.getVeiculo()));
        dto.setTrajeto(TrajetoMapper.toDTO(vv.getTrajeto()));

        dto.setData(vv.getData());
        dto.setStartTime(vv.getStartTime());
        dto.setFinishTime(vv.getFinishTime());

        dto.setServiceId(vv.getServiceId());
        dto.setGtfsTripId(vv.getGtfsTripId());
        dto.setHoraPartidaPlaneada(vv.getHoraPartidaPlaneada());

        if (vv.getPontoAtual() != null) {
            dto.setPontoAtualId(vv.getPontoAtual().getId());
        }
        return dto;
    }

    public static List<ViagemVeiculoDTO> toVeiculoDTOList(List<ViagemVeiculo> viagens) {
        if (viagens == null) return null;
        return viagens.stream().map(ViagemMapper::toVeiculoDTO).collect(Collectors.toList());
    }
}
