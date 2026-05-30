package pt.notub.models;

import jakarta.persistence.*;

@Entity
public class ViagemVeiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tripId;

    @ManyToOne
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @ManyToOne
    @JoinColumn(name = "trajeto_id")
    private Trajeto trajeto;

    public ViagemVeiculo() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    public Trajeto getTrajeto() { return trajeto; }
    public void setTrajeto(Trajeto trajeto) { this.trajeto = trajeto; }
}
