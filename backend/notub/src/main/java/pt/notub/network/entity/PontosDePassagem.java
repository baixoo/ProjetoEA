package pt.notub.network.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ponto_passagem")
public class PontosDePassagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int ordem;

    @ManyToOne
    @JoinColumn(name = "trajeto_id")
    private Trajeto trajeto;

    @ManyToOne
    @JoinColumn(name = "paragem_id")
    private Paragem paragem;

    public PontosDePassagem() {}

    public Long getId() { return id; }

    public Trajeto getTrajeto() {
        return trajeto;
    }

    public void setTrajeto(Trajeto trajeto) {
        this.trajeto = trajeto;
    }

    public void setId(Long id) { this.id = id; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public Paragem getParagem() { return paragem; }
    public void setParagem(Paragem paragem) { this.paragem = paragem; }
}
