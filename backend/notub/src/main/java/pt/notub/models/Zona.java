package pt.notub.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @OneToMany
    @JoinColumn(name = "zona_id")
    private List<Paragem> paragens;

    public Zona() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public List<Paragem> getParagens() { return paragens; }
    public void setParagens(List<Paragem> paragens) { this.paragens = paragens; }
}
