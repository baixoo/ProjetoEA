package pt.notub.points.dto;

import java.time.LocalDateTime;

public class HistoricoPontosDTO {

    private Long id;
    private int pontos;
    private String tipo;
    private String descricao;
    private LocalDateTime dataHora;

    public HistoricoPontosDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getPontos() { return pontos; }
    public void setPontos(int pontos) { this.pontos = pontos; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
