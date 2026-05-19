package pt.notub.models;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
public class SequenciaParagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int ordem;
    private LocalDateTime horaChegada;
    private Duration tempoDesdeInicio;

    @ManyToOne
    @JoinColumn(name = "trajeto_id")
    private Trajeto trajeto;

    @ManyToOne
    @JoinColumn(name = "paragem_id")
    private Paragem paragem;

    public SequenciaParagem() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public LocalDateTime getHoraChegada() { return horaChegada; }
    public void setHoraChegada(LocalDateTime horaChegada) { this.horaChegada = horaChegada; }
    public Duration getTempoDesdeInicio() { return tempoDesdeInicio; }
    public void setTempoDesdeInicio(Duration tempoDesdeInicio) { this.tempoDesdeInicio = tempoDesdeInicio; }
    public Trajeto getTrajeto() { return trajeto; }
    public void setTrajeto(Trajeto trajeto) { this.trajeto = trajeto; }
    public Paragem getParagem() { return paragem; }
    public void setParagem(Paragem paragem) { this.paragem = paragem; }
}
