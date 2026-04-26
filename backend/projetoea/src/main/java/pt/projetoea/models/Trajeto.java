package pt.projetoea.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Trajeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String direcao;

    @ManyToOne
    @JoinColumn(name = "carreira_id")
    private Carreira carreira;

    @OneToMany(mappedBy = "trajeto", cascade = CascadeType.ALL)
    private List<ViagemVeiculo> viagem;

    @OneToMany(mappedBy = "trajeto", cascade = CascadeType.ALL)
    private List<SequenciaParagem> sequencias;

    public Trajeto() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDirecao() { return direcao; }
    public void setDirecao(String direcao) { this.direcao = direcao; }
    public Carreira getCarreira() { return carreira; }
    public void setCarreira(Carreira carreira) { this.carreira = carreira; }
    public List<ViagemVeiculo> getViagem() { return viagem; }
    public void setViagem(List<ViagemVeiculo> viagem) { this.viagem = viagem; }
    public List<SequenciaParagem> getSequencias() { return sequencias; }
    public void setSequencias(List<SequenciaParagem> sequencias) { this.sequencias = sequencias; }
}
