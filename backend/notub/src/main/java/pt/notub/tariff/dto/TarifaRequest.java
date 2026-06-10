package pt.notub.tariff.dto;

public record TarifaRequest(
        Float valor,
        String tipoUtilizador,
        String modalidade,
        Integer nrZonas
) {
}
