package pt.notub.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Utilizador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String primeiroNome;
    private String ultimoNome;
    @Column(unique = true)
    private String nif;
    @Column(unique = true)
    private String email;

    private String password;
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    private TipoUtilizador tipoUtilizador;

    @Column
    private int nrPontos = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoPapel role = TipoPapel.UTILIZADOR;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AuthMethod authMethod;

    public Utilizador() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPrimeiroNome() { return primeiroNome; }
    public void setPrimeiroNome(String primeiroNome) { this.primeiroNome = primeiroNome; }
    public String getUltimoNome() { return ultimoNome; }
    public void setUltimoNome(String ultimoNome) { this.ultimoNome = ultimoNome; }
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = (nif == null || nif.isBlank()) ? null : nif; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public TipoUtilizador getTipoUtilizador() { return tipoUtilizador; }
    public void setTipoUtilizador(TipoUtilizador tipoUtilizador) { this.tipoUtilizador = tipoUtilizador; }
    public int getNrPontos() { return nrPontos; }
    public void setNrPontos(int nrPontos) { this.nrPontos = nrPontos; }
    public TipoPapel getRole() { return role; }
    public void setRole(TipoPapel role) { this.role = role; }
    public AuthMethod getAuthMethod() { return authMethod; }
    public void setAuthMethod(AuthMethod authMethod) { this.authMethod = authMethod; }
}
