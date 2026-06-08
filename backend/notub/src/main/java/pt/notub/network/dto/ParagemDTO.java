package pt.notub.network.dto;

public class ParagemDTO {

    private Long id;
    private String nome;
    private PointDTO localizacao;
    private Integer zonaNum;
    private String zonaNome;

    public ParagemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public PointDTO getLocalizacao() { return localizacao; }
    public void setLocalizacao(PointDTO localizacao) { this.localizacao = localizacao; }
    public Integer getZonaNum() { return zonaNum; }
    public void setZonaNum(Integer zonaNum) { this.zonaNum = zonaNum; }
    public String getZonaNome() { return zonaNome; }
    public void setZonaNome(String zonaNome) { this.zonaNome = zonaNome; }
}
