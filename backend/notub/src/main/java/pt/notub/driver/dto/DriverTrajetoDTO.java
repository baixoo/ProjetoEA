package pt.notub.driver.dto;

public record DriverTrajetoDTO(
        Long id,
        String linha,
        String direcao,
        String primeiraParagem,
        String ultimaParagem
) {
}
