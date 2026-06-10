package pt.notub.transaction.dto;

public record CreateTransacaoRequest(Long tituloId, Long utilizadorId, String referenciaExterna) {
}
