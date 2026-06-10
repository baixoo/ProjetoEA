package pt.notub.admin.dto;

public record AdminStatsResponse(
        long utilizadores,
        long bilhetes,
        long passes,
        long veiculos,
        long transacoes,
        long viagens
) {
}
