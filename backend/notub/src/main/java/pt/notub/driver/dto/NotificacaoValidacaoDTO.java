package pt.notub.driver.dto;

import java.time.LocalDateTime;

public class NotificacaoValidacaoDTO {
    private boolean valido;
    private String nomePassageiro;
    private String tituloTipo;
    private Long veiculoId;
    private LocalDateTime timestamp;

    public NotificacaoValidacaoDTO() {}

    public NotificacaoValidacaoDTO(boolean valido, String nomePassageiro, String tituloTipo, Long veiculoId, LocalDateTime timestamp) {
        this.valido = valido;
        this.nomePassageiro = nomePassageiro;
        this.tituloTipo = tituloTipo;
        this.veiculoId = veiculoId;
        this.timestamp = timestamp;
    }

    public boolean isValido() { return valido; }
    public void setValido(boolean valido) { this.valido = valido; }
    public String getNomePassageiro() { return nomePassageiro; }
    public void setNomePassageiro(String nomePassageiro) { this.nomePassageiro = nomePassageiro; }
    public String getTituloTipo() { return tituloTipo; }
    public void setTituloTipo(String tituloTipo) { this.tituloTipo = tituloTipo; }
    public Long getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Long veiculoId) { this.veiculoId = veiculoId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
