package pt.notub.trip.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import pt.notub.network.entity.Trajeto;
import pt.notub.vehicle.entity.Veiculo;


@Entity
public class ViagemVeiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // private String tripId;

    @ManyToOne
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @ManyToOne
    @JoinColumn(name = "trajeto_id")
    private Trajeto trajeto;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    public ViagemVeiculo() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // public String getTripId() { return tripId; }
    // public void setTripId(String tripId) { this.tripId = tripId; }
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    public Trajeto getTrajeto() { return trajeto; }
    public void setTrajeto(Trajeto trajeto) { this.trajeto = trajeto; }
    public LocalDate getData() { return startTime != null ? startTime.toLocalDate() : null;
    }
    public void setData(LocalDate data) {
        if (data != null) {
            if (this.startTime != null) {
                this.startTime = LocalDateTime.of(data, this.startTime.toLocalTime());
            } else {
                this.startTime = LocalDateTime.of(data, LocalDateTime.now().toLocalTime());
            }
        }
    }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime;}
}
