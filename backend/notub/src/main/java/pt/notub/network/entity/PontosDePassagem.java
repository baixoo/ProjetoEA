package pt.notub.network.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
public class PontosDePassagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int ordem;
    private LocalTime horaChegada;
    private int tempoDesdeInicio;

    @ManyToOne
    @JoinColumn(name = "paragem_id")
    private Paragem paragem;

    public PontosDePassagem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public LocalTime getHoraChegada() { return horaChegada; }
    public void setHoraChegada(LocalTime horaChegada) { this.horaChegada = horaChegada; }
    public int getTempoDesdeInicio() { return tempoDesdeInicio; }
    public void setTempoDesdeInicio(int tempoDesdeInicio) { this.tempoDesdeInicio = tempoDesdeInicio; }
    public Paragem getParagem() { return paragem; }
    public void setParagem(Paragem paragem) { this.paragem = paragem; }
}
