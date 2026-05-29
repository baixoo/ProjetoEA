package pt.notub.payment;

public record PagamentoRejeitadoEvent(
    Long transacaoId,
    Long utilizadorId
) {}
