package pt.notub.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ViagemUtilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime inicio;
    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    private EstadoViagem estado;

    @ManyToOne
    @JoinColumn(name = "titulo_id")
    private TituloTransporte titulo;

    @ManyToOne
    @JoinColumn(name = "viagem_veiculo_id")
    private ViagemVeiculo viagemVeiculo;

    @ManyToOne
    @JoinColumn(name = "paragem_entrada_id")
    private Paragem paragemEntrada;

    @ManyToOne
    @JoinColumn(name = "paragem_saida_id")
    private Paragem paragemSaida;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "coima_id", referencedColumnName = "id")
    private Coima coima;

    public ViagemUtilizador() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public EstadoViagem getEstado() { return estado; }
    public void setEstado(EstadoViagem estado) { this.estado = estado; }
    public TituloTransporte getTitulo() { return titulo; }
    public void setTitulo(TituloTransporte titulo) { this.titulo = titulo; }
    public ViagemVeiculo getViagemVeiculo() { return viagemVeiculo; }
    public void setViagemVeiculo(ViagemVeiculo viagemVeiculo) { this.viagemVeiculo = viagemVeiculo; }
    public Paragem getParagemEntrada() { return paragemEntrada; }
    public void setParagemEntrada(Paragem paragemEntrada) { this.paragemEntrada = paragemEntrada; }
    public Paragem getParagemSaida() { return paragemSaida; }
    public void setParagemSaida(Paragem paragemSaida) { this.paragemSaida = paragemSaida; }
    public Coima getCoima() { return coima; }
    public void setCoima(Coima coima) { this.coima = coima; }
}
