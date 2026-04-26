package pt.projetoea.dto.request;

public class RegisterRequest {
    private String primeiroNome;
    private String ultimoNome;
    private String email;
    private String password;
    private String nif;
    private String dataNascimento;

    public String getPrimeiroNome() { return primeiroNome; }
    public void setPrimeiroNome(String primeiroNome) { this.primeiroNome = primeiroNome; }
    public String getUltimoNome() { return ultimoNome; }
    public void setUltimoNome(String ultimoNome) { this.ultimoNome = ultimoNome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(String dataNascimento) { this.dataNascimento = dataNascimento; }
}
