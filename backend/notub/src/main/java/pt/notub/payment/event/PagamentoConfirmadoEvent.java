package pt.notub.payment.event;

public record PagamentoConfirmadoEvent(
    Long transacaoId,
    Long utilizadorId,
    String tipoProduto,
    Integer quantidade,
    String modalidade,
    Long zonaId
) {}
