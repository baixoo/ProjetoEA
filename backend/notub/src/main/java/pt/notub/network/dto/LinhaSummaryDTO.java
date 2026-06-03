package pt.notub.network.dto;

public class LinhaSummaryDTO {

    private Long id;
    private String nome;

    public LinhaSummaryDTO() {}

    public LinhaSummaryDTO(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
