package pt.notub.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;

@Entity
public class Zona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int num;

    private String nome;

    @JsonIgnore
    @OneToMany
    @JoinColumn(name = "zona_id")
    private List<Paragem> paragens;

    public Zona() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getNum() { return num; }
    public void setNum(int num) { this.num = num; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public List<Paragem> getParagens() { return paragens; }
    public void setParagens(List<Paragem> paragens) { this.paragens = paragens; }
}
