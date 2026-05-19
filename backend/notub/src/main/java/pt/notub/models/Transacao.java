package pt.notub.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dataHora;

    private String referenciaExterna;

    @Enumerated(EnumType.STRING)
    private EstadoPagamento estadoPagamento;

    @ManyToOne
    @JoinColumn(name = "titulo_id")
    private TituloTransporte titulo;

    public Transacao() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) { this.referenciaExterna = referenciaExterna; }
    public EstadoPagamento getEstadoPagamento() { return estadoPagamento; }
    public void setEstadoPagamento(EstadoPagamento estadoPagamento) { this.estadoPagamento = estadoPagamento; }
    public TituloTransporte getTitulo() { return titulo; }
    public void setTitulo(TituloTransporte titulo) { this.titulo = titulo; }
}
