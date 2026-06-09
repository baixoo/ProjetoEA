package pt.notub.network.dto;

import java.util.List;

public class RotaDTO {
    private int totalMinutos;
    private int trocas;
    private int totalCaminhadaMinutos;
    private boolean direta;
    private boolean caminho;
    private String horaPartida;
    private String horaChegada;
    private List<ZonaResumoDTO> zonas;
    private int nrZonas;
    private List<SegmentoDTO> segmentos;

    public RotaDTO() {}

    public int getTotalMinutos() { return totalMinutos; }
    public void setTotalMinutos(int totalMinutos) { this.totalMinutos = totalMinutos; }
    public int getTrocas() { return trocas; }
    public void setTrocas(int trocas) { this.trocas = trocas; }
    public int getTotalCaminhadaMinutos() { return totalCaminhadaMinutos; }
    public void setTotalCaminhadaMinutos(int totalCaminhadaMinutos) { this.totalCaminhadaMinutos = totalCaminhadaMinutos; }
    public boolean isDireta() { return direta; }
    public void setDireta(boolean direta) { this.direta = direta; }
    public boolean isCaminho() { return caminho; }
    public void setCaminho(boolean caminho) { this.caminho = caminho; }
    public String getHoraPartida() { return horaPartida; }
    public void setHoraPartida(String horaPartida) { this.horaPartida = horaPartida; }
    public String getHoraChegada() { return horaChegada; }
    public void setHoraChegada(String horaChegada) { this.horaChegada = horaChegada; }
    public List<ZonaResumoDTO> getZonas() { return zonas; }
    public void setZonas(List<ZonaResumoDTO> zonas) { this.zonas = zonas; }
    public int getNrZonas() { return nrZonas; }
    public void setNrZonas(int nrZonas) { this.nrZonas = nrZonas; }
    public List<SegmentoDTO> getSegmentos() { return segmentos; }
    public void setSegmentos(List<SegmentoDTO> segmentos) { this.segmentos = segmentos; }

    public static class ParagemDTO {
        private Long id;
        private String nome;
        private Double latitude;
        private Double longitude;

        public ParagemDTO() {}
        public ParagemDTO(Long id, String nome) { this.id = id; this.nome = nome; }
        public ParagemDTO(Long id, String nome, Double lat, Double lon) { this.id = id; this.nome = nome; this.latitude = lat; this.longitude = lon; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

    public static class SegmentoDTO {
        private Long trajetoId;
        private String linhaNome;
        private String direcao;
        private String destinoFinal;
        private ParagemDTO origem;
        private ParagemDTO destino;
        private List<ParagemDTO> paragens;
        private int duracaoMinutos;
        private int tempoCaminhadaMinutos;
        private int esperaMinutos;
        private String horaPartida;
        private String horaChegada;
        private List<ZonaResumoDTO> zonas;
        private int nrZonas;

        public SegmentoDTO() {}
        public Long getTrajetoId() { return trajetoId; }
        public void setTrajetoId(Long trajetoId) { this.trajetoId = trajetoId; }
        public String getLinhaNome() { return linhaNome; }
        public void setLinhaNome(String linhaNome) { this.linhaNome = linhaNome; }
        public String getDirecao() { return direcao; }
        public void setDirecao(String direcao) { this.direcao = direcao; }
        public String getDestinoFinal() { return destinoFinal; }
        public void setDestinoFinal(String destinoFinal) { this.destinoFinal = destinoFinal; }
        public ParagemDTO getOrigem() { return origem; }
        public void setOrigem(ParagemDTO origem) { this.origem = origem; }
        public ParagemDTO getDestino() { return destino; }
        public void setDestino(ParagemDTO destino) { this.destino = destino; }
        public List<ParagemDTO> getParagens() { return paragens; }
        public void setParagens(List<ParagemDTO> paragens) { this.paragens = paragens; }
        public int getDuracaoMinutos() { return duracaoMinutos; }
        public void setDuracaoMinutos(int duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
        public int getTempoCaminhadaMinutos() { return tempoCaminhadaMinutos; }
        public void setTempoCaminhadaMinutos(int tempoCaminhadaMinutos) { this.tempoCaminhadaMinutos = tempoCaminhadaMinutos; }
        public int getEsperaMinutos() { return esperaMinutos; }
        public void setEsperaMinutos(int esperaMinutos) { this.esperaMinutos = esperaMinutos; }
        public String getHoraPartida() { return horaPartida; }
        public void setHoraPartida(String horaPartida) { this.horaPartida = horaPartida; }
        public String getHoraChegada() { return horaChegada; }
        public void setHoraChegada(String horaChegada) { this.horaChegada = horaChegada; }
        public List<ZonaResumoDTO> getZonas() { return zonas; }
        public void setZonas(List<ZonaResumoDTO> zonas) { this.zonas = zonas; }
        public int getNrZonas() { return nrZonas; }
        public void setNrZonas(int nrZonas) { this.nrZonas = nrZonas; }
    }
}
