package pt.notub.network.dto;

public class ServicoDTO {
    private Long id;
    private String nome;

    public ServicoDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
