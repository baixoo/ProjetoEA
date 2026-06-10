package pt.notub.trip.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pt.notub.network.dto.TrajetoDTO;
import pt.notub.vehicle.dto.VeiculoDTO;

public class ViagemVeiculoDTO {

    private Long id;
    // private String tripId;
    private VeiculoDTO veiculo;
    private TrajetoDTO trajeto;

    private LocalDate data;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    public ViagemVeiculoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // public String getTripId() { return tripId; }
    // public void setTripId(String tripId) { this.tripId = tripId; }
    public VeiculoDTO getVeiculo() { return veiculo; }
    public void setVeiculo(VeiculoDTO veiculo) { this.veiculo = veiculo; }
    public TrajetoDTO getTrajeto() { return trajeto; }
    public void setTrajeto(TrajetoDTO trajeto) { this.trajeto = trajeto; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }
}
