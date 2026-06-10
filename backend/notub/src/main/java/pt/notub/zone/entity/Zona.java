package pt.notub.zone.entity;

import jakarta.persistence.*;

@Entity
public class Zona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int num;

    private String nome;

    public Zona() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getNum() { return num; }
    public void setNum(int num) { this.num = num; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
