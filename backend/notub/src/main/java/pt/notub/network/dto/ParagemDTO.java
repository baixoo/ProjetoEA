package pt.notub.network.dto;

public class ParagemDTO {

    private Long id;
    private String nome;
    private PointDTO localizacao;

    public ParagemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public PointDTO getLocalizacao() { return localizacao; }
    public void setLocalizacao(PointDTO localizacao) { this.localizacao = localizacao; }
}
