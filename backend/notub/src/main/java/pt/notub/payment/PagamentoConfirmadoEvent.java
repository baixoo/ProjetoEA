package pt.notub.payment;

public record PagamentoConfirmadoEvent(
    Long transacaoId,
    Long utilizadorId,
    String tipoProduto,
    Integer quantidade,
    String modalidade,
    Long zonaId
) {}
