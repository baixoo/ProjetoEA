package pt.notub.trip.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pt.notub.user.entity.Utilizador;

@Entity
@Table(name = "monitorizacao")
public class Monitorizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "viagem_veiculo_id", nullable = false)
    private ViagemVeiculo viagemVeiculo;

    @ManyToOne
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    public Monitorizacao() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ViagemVeiculo getViagemVeiculo() {
        return viagemVeiculo;
    }

    public void setViagemVeiculo(ViagemVeiculo viagemVeiculo) {
        this.viagemVeiculo = viagemVeiculo;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }

    public void addSubscription() {
        
    }
}