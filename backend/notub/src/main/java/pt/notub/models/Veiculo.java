package pt.notub.models;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
public abstract class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matricula;
    private int nLugares;
    private int lotacaoAtual;

    @Embedded
    private Point localizacaoAtual;

    @ManyToOne
    @JoinColumn(name = "linha_id")
    private Linha linha;

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
    public Linha getLinha() { return linha; }
    public void setLinha(Linha linha) { this.linha = linha; }
}
