package pt.notub.payment;

public record PagamentoConfirmadoEvent(
    Long transacaoId,
    Long utilizadorId,
    String tipoProduto,
    Integer quantidade,
    String modalidade,
    String zonaIds
) {}
