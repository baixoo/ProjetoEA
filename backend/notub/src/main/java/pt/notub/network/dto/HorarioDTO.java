package pt.notub.network.dto;

import java.util.List;

public class HorarioDTO {
    private Long trajetoId;
    private String direcao;
    private String destinoFinal;
    private List<String> partidas;

    public HorarioDTO() {}
    public HorarioDTO(Long trajetoId, String direcao, String destinoFinal, List<String> partidas) {
        this.trajetoId = trajetoId;
        this.direcao = direcao;
        this.destinoFinal = destinoFinal;
        this.partidas = partidas;
    }

    public Long getTrajetoId() { return trajetoId; }
    public void setTrajetoId(Long trajetoId) { this.trajetoId = trajetoId; }
    public String getDirecao() { return direcao; }
    public void setDirecao(String direcao) { this.direcao = direcao; }
    public String getDestinoFinal() { return destinoFinal; }
    public void setDestinoFinal(String destinoFinal) { this.destinoFinal = destinoFinal; }
    public List<String> getPartidas() { return partidas; }
    public void setPartidas(List<String> partidas) { this.partidas = partidas; }
}
