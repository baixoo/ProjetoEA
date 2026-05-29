package pt.notub.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Linha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String identificadorServico;

    @OneToMany(mappedBy = "linha", cascade = CascadeType.ALL)
    private List<Trajeto> trajetos;

    public Linha() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getIdentificadorServico() { return identificadorServico; }
    public void setIdentificadorServico(String identificadorServico) { this.identificadorServico = identificadorServico; }
    public List<Trajeto> getTrajetos() { return trajetos; }
    public void setTrajetos(List<Trajeto> trajetos) { this.trajetos = trajetos; }
}
