package pt.notub.network.dto;

import java.util.List;
import java.util.Map;

public class HorarioParagemDTO {
    private Long trajetoId;
    private String direcao;
    private String destinoFinal;
    private String linhaNome;
    private List<ParagemHorarioDTO> paragens;

    public HorarioParagemDTO() {}

    public Long getTrajetoId() { return trajetoId; }
    public void setTrajetoId(Long trajetoId) { this.trajetoId = trajetoId; }
    public String getDirecao() { return direcao; }
    public void setDirecao(String direcao) { this.direcao = direcao; }
    public String getDestinoFinal() { return destinoFinal; }
    public void setDestinoFinal(String destinoFinal) { this.destinoFinal = destinoFinal; }
    public String getLinhaNome() { return linhaNome; }
    public void setLinhaNome(String linhaNome) { this.linhaNome = linhaNome; }
    public List<ParagemHorarioDTO> getParagens() { return paragens; }
    public void setParagens(List<ParagemHorarioDTO> paragens) { this.paragens = paragens; }

    public static class ParagemHorarioDTO {
        private Long paragemId;
        private String nome;
        private int tempoDesdeInicio;
        private Map<Integer, List<Integer>> horarios;

        public ParagemHorarioDTO() {}

        public Long getParagemId() { return paragemId; }
        public void setParagemId(Long paragemId) { this.paragemId = paragemId; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public int getTempoDesdeInicio() { return tempoDesdeInicio; }
        public void setTempoDesdeInicio(int tempoDesdeInicio) { this.tempoDesdeInicio = tempoDesdeInicio; }
        public Map<Integer, List<Integer>> getHorarios() { return horarios; }
        public void setHorarios(Map<Integer, List<Integer>> horarios) { this.horarios = horarios; }
    }
}
