package pt.notub.models;

import jakarta.persistence.*;

@Entity
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private float valor;

    @Enumerated(EnumType.STRING)
    private TipoPerfil perfil;

    @Enumerated(EnumType.STRING)
    private ModalidadePasse modalidade;

    public Tarifa() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public float getValor() { return valor; }
    public void setValor(float valor) { this.valor = valor; }
    public TipoPerfil getPerfil() { return perfil; }
    public void setPerfil(TipoPerfil perfil) { this.perfil = perfil; }
    public ModalidadePasse getModalidade() { return modalidade; }
    public void setModalidade(ModalidadePasse modalidade) { this.modalidade = modalidade; }
}
