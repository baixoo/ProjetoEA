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

    @Enumerated(EnumType.STRING)
    private MetodoPagamento metodoPagamento;

    @Enumerated(EnumType.STRING)
    private TipoProduto tipoProduto;

    private Double valor;
    private Integer quantidade;
    private String modalidade;
    private Long zonaId;
    private int mesInicio;
    private int anoInicio;

    @Column(unique = true)
    private String token;

    @Column(columnDefinition = "TEXT")
    private String stripeSessionId;

    @ManyToOne
    @JoinColumn(name = "titulo_id")
    private TituloTransporte titulo;

    @ManyToOne
    @JoinColumn(name = "utilizador_id")
    private Utilizador utilizador;

    public Transacao() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) { this.referenciaExterna = referenciaExterna; }
    public EstadoPagamento getEstadoPagamento() { return estadoPagamento; }
    public void setEstadoPagamento(EstadoPagamento estadoPagamento) { this.estadoPagamento = estadoPagamento; }
    public MetodoPagamento getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(MetodoPagamento metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public TipoProduto getTipoProduto() { return tipoProduto; }
    public void setTipoProduto(TipoProduto tipoProduto) { this.tipoProduto = tipoProduto; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
    public Long getZonaId() { return zonaId; }
    public void setZonaId(Long zonaId) { this.zonaId = zonaId; }
    public int getMesInicio() { return mesInicio; }
    public void setMesInicio(int mesInicio) { this.mesInicio = mesInicio; }
    public int getAnoInicio() { return anoInicio; }
    public void setAnoInicio(int anoInicio) { this.anoInicio = anoInicio; }
    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }
    public TituloTransporte getTitulo() { return titulo; }
    public void setTitulo(TituloTransporte titulo) { this.titulo = titulo; }
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador utilizador) { this.utilizador = utilizador; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
