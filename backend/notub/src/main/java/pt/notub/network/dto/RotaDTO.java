package pt.notub.network.dto;

import java.util.List;

public class RotaDTO {
    private int totalMinutos;
    private int trocas;
    private List<SegmentoDTO> segmentos;

    public RotaDTO() {}

    public RotaDTO(int totalMinutos, int trocas, List<SegmentoDTO> segmentos) {
        this.totalMinutos = totalMinutos;
        this.trocas = trocas;
        this.segmentos = segmentos;
    }

    public int getTotalMinutos() { return totalMinutos; }
    public void setTotalMinutos(int totalMinutos) { this.totalMinutos = totalMinutos; }
    public int getTrocas() { return trocas; }
    public void setTrocas(int trocas) { this.trocas = trocas; }
    public List<SegmentoDTO> getSegmentos() { return segmentos; }
    public void setSegmentos(List<SegmentoDTO> segmentos) { this.segmentos = segmentos; }

    public static class ParagemDTO {
        private Long id;
        private String nome;

        public ParagemDTO() {}
        public ParagemDTO(Long id, String nome) { this.id = id; this.nome = nome; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
    }

    public static class SegmentoDTO {
        private String linhaNome;
        private String direcao;
        private ParagemDTO origem;
        private ParagemDTO destino;
        private List<ParagemDTO> paragens;
        private int duracaoMinutos;

        public SegmentoDTO() {}
        public String getLinhaNome() { return linhaNome; }
        public void setLinhaNome(String linhaNome) { this.linhaNome = linhaNome; }
        public String getDirecao() { return direcao; }
        public void setDirecao(String direcao) { this.direcao = direcao; }
        public ParagemDTO getOrigem() { return origem; }
        public void setOrigem(ParagemDTO origem) { this.origem = origem; }
        public ParagemDTO getDestino() { return destino; }
        public void setDestino(ParagemDTO destino) { this.destino = destino; }
        public List<ParagemDTO> getParagens() { return paragens; }
        public void setParagens(List<ParagemDTO> paragens) { this.paragens = paragens; }
        public int getDuracaoMinutos() { return duracaoMinutos; }
        public void setDuracaoMinutos(int duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    }
}
