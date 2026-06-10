package pt.notub.payment.event;

public record PagamentoRejeitadoEvent(
    Long transacaoId,
    Long utilizadorId
) {}
