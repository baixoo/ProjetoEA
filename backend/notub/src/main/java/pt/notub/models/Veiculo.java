package pt.notub.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matricula;
    private int nLugares;
    private int lotacaoAtual;

    @Embedded
    private Point localizacaoAtual;

    @OneToMany(mappedBy = "veiculo", cascade = CascadeType.ALL)
    private List<ViagemVeiculo> viagem;

    public Veiculo() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public int getnLugares() { return nLugares; }
    public void setnLugares(int nLugares) { this.nLugares = nLugares; }
    public int getLotacaoAtual() { return lotacaoAtual; }
    public void setLotacaoAtual(int lotacaoAtual) { this.lotacaoAtual = lotacaoAtual; }
    public Point getLocalizacaoAtual() { return localizacaoAtual; }
    public void setLocalizacaoAtual(Point localizacaoAtual) { this.localizacaoAtual = localizacaoAtual; }
    public List<ViagemVeiculo> getViagem() { return viagem; }
    public void setViagem(List<ViagemVeiculo> viagem) { this.viagem = viagem; }
}
