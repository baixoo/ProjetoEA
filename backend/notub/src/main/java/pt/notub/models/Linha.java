package pt.notub.models;

import jakarta.persistence.*;

@Entity
public class Linha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String identificadorServico;

    public Linha() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getIdentificadorServico() { return identificadorServico; }
    public void setIdentificadorServico(String identificadorServico) { this.identificadorServico = identificadorServico; }
}
