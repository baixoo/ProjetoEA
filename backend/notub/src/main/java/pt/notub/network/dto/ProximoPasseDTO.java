package pt.notub.network.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProximoPasseDTO {
    private String hora;
    private int esperaMinutos;
    private String horaPrevista;
    private Integer lotacaoAtual;
    private Integer nLugares;
    private Integer tempoAtraso;

    public ProximoPasseDTO() {}
    public ProximoPasseDTO(String hora, int esperaMinutos) {
        this.hora = hora;
        this.esperaMinutos = esperaMinutos;
    }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    public int getEsperaMinutos() { return esperaMinutos; }
    public void setEsperaMinutos(int esperaMinutos) { this.esperaMinutos = esperaMinutos; }

    public String getHoraPrevista() { return horaPrevista; }
    public void setHoraPrevista(String horaPrevista) { this.horaPrevista = horaPrevista; }
    public Integer getLotacaoAtual() { return lotacaoAtual; }
    public void setLotacaoAtual(Integer lotacaoAtual) { this.lotacaoAtual = lotacaoAtual; }

    @JsonProperty("nLugares")
    public Integer getnLugares() { return nLugares; }

    @JsonProperty("nLugares")
    public void setnLugares(Integer nLugares) { this.nLugares = nLugares; }

    public Integer getTempoAtraso() { return tempoAtraso; }
    public void setTempoAtraso(Integer tempoAtraso) { this.tempoAtraso = tempoAtraso; }
}
