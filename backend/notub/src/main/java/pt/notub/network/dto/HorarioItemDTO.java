package pt.notub.network.dto;

import java.time.LocalTime;

public class HorarioItemDTO {
    private Long id;
    private LocalTime hora;
    private Long pontoPassagemId;
    private String gtfsTripId;
    private ServicoDTO servico;

    public HorarioItemDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public Long getPontoPassagemId() {
        return pontoPassagemId;
    }

    public void setPontoPassagemId(Long pontoPassagemId) {
        this.pontoPassagemId = pontoPassagemId;
    }

    public String getGtfsTripId() {
        return gtfsTripId;
    }

    public void setGtfsTripId(String gtfsTripId) {
        this.gtfsTripId = gtfsTripId;
    }

    public ServicoDTO getServico() {
        return servico;
    }

    public void setServico(ServicoDTO servico) {
        this.servico = servico;
    }
}
