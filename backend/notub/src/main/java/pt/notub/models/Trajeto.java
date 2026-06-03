package pt.notub.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Trajeto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Direcao direcao;

    @ManyToOne
    @JoinColumn(name = "linha_id")
    private Linha linha;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trajeto_id")
    private List<PontosDePassagem> pontosDePassagem;

    public Trajeto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Direcao getDirecao() { return direcao; }
    public void setDirecao(Direcao direcao) { this.direcao = direcao; }
    public Linha getLinha() { return linha; }
    public void setLinha(Linha linha) { this.linha = linha; }
    public List<PontosDePassagem> getPontosDePassagem() { return pontosDePassagem; }
    public void setPontosDePassagem(List<PontosDePassagem> pontosDePassagem) { this.pontosDePassagem = pontosDePassagem; }
}
