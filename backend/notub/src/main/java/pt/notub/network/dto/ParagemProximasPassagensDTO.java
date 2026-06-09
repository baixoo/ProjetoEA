package pt.notub.network.dto;

import java.util.List;

public class ParagemProximasPassagensDTO {
    private Long paragemId;
    private String paragemNome;
    private List<LinhaProximasPassagensDTO> linhas;

    public Long getParagemId() { return paragemId; }
    public void setParagemId(Long paragemId) { this.paragemId = paragemId; }
    public String getParagemNome() { return paragemNome; }
    public void setParagemNome(String paragemNome) { this.paragemNome = paragemNome; }
    public List<LinhaProximasPassagensDTO> getLinhas() { return linhas; }
    public void setLinhas(List<LinhaProximasPassagensDTO> linhas) { this.linhas = linhas; }

    public static class LinhaProximasPassagensDTO {
        private Long linhaId;
        private String linhaNome;
        private List<TrajetoProximasPassagensDTO> trajetos;

        public Long getLinhaId() { return linhaId; }
        public void setLinhaId(Long linhaId) { this.linhaId = linhaId; }
        public String getLinhaNome() { return linhaNome; }
        public void setLinhaNome(String linhaNome) { this.linhaNome = linhaNome; }
        public List<TrajetoProximasPassagensDTO> getTrajetos() { return trajetos; }
        public void setTrajetos(List<TrajetoProximasPassagensDTO> trajetos) { this.trajetos = trajetos; }
    }

    public static class TrajetoProximasPassagensDTO {
        private Long trajetoId;
        private String direcao;
        private String destinoFinal;
        private List<ProximoPasseDTO> proximosPasses;

        public Long getTrajetoId() { return trajetoId; }
        public void setTrajetoId(Long trajetoId) { this.trajetoId = trajetoId; }
        public String getDirecao() { return direcao; }
        public void setDirecao(String direcao) { this.direcao = direcao; }
        public String getDestinoFinal() { return destinoFinal; }
        public void setDestinoFinal(String destinoFinal) { this.destinoFinal = destinoFinal; }
        public List<ProximoPasseDTO> getProximosPasses() { return proximosPasses; }
        public void setProximosPasses(List<ProximoPasseDTO> proximosPasses) { this.proximosPasses = proximosPasses; }
    }
}
