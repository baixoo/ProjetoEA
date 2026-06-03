package pt.notub.trip.dto;

import pt.notub.models.EstadoViagem;
import pt.notub.network.dto.ParagemDTO;

import java.time.LocalDateTime;

public class ViagemDTO {

    private Long id;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private EstadoViagem estado;
    private ParagemDTO paragemEntrada;
    private ParagemDTO paragemSaida;

    public ViagemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public EstadoViagem getEstado() { return estado; }
    public void setEstado(EstadoViagem estado) { this.estado = estado; }
    public ParagemDTO getParagemEntrada() { return paragemEntrada; }
    public void setParagemEntrada(ParagemDTO paragemEntrada) { this.paragemEntrada = paragemEntrada; }
    public ParagemDTO getParagemSaida() { return paragemSaida; }
    public void setParagemSaida(ParagemDTO paragemSaida) { this.paragemSaida = paragemSaida; }
}
