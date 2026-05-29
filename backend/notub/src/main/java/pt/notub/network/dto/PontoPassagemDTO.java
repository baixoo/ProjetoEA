package pt.notub.network.dto;

import java.time.LocalTime;

public class PontoPassagemDTO {

    private Long id;
    private int ordem;
    private LocalTime horaChegada;
    private int tempoDesdeInicio;
    private ParagemDTO paragem;

    public PontoPassagemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public LocalTime getHoraChegada() { return horaChegada; }
    public void setHoraChegada(LocalTime horaChegada) { this.horaChegada = horaChegada; }
    public int getTempoDesdeInicio() { return tempoDesdeInicio; }
    public void setTempoDesdeInicio(int tempoDesdeInicio) { this.tempoDesdeInicio = tempoDesdeInicio; }
    public ParagemDTO getParagem() { return paragem; }
    public void setParagem(ParagemDTO paragem) { this.paragem = paragem; }
}
