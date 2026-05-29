package pt.notub.user.dto;

import pt.notub.models.AuthMethod;
import pt.notub.models.TipoPapel;
import pt.notub.models.TipoUtilizador;

import java.time.LocalDate;

public class UserDTO {

    private Long id;
    private String primeiroNome;
    private String ultimoNome;
    private String nif;
    private String email;
    private LocalDate dataNascimento;
    private TipoUtilizador tipoUtilizador;
    private int nrPontos;
    private TipoPapel role;
    private AuthMethod authMethod;

    public UserDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPrimeiroNome() { return primeiroNome; }
    public void setPrimeiroNome(String primeiroNome) { this.primeiroNome = primeiroNome; }
    public String getUltimoNome() { return ultimoNome; }
    public void setUltimoNome(String ultimoNome) { this.ultimoNome = ultimoNome; }
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
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
