package pt.notub.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Carreira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String identificadorServico;

    @OneToMany(mappedBy = "carreira", cascade = CascadeType.ALL)
    private List<Trajeto> trajetos;

    public Carreira() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdentificadorServico() { return identificadorServico; }
    public void setIdentificadorServico(String identificadorServico) { this.identificadorServico = identificadorServico; }
    public List<Trajeto> getTrajetos() { return trajetos; }
    public void setTrajetos(List<Trajeto> trajetos) { this.trajetos = trajetos; }
}
