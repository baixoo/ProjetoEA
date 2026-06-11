package pt.notub.driver.dto;

import java.util.List;

public record DriverTrajetoDTO(
        Long id,
        String linha,
        String direcao,
        String primeiraParagem,
        String ultimaParagem,
        List<DriverParagemInfo> paragens
) {
    public record DriverParagemInfo(
            Long pontoPassagemId,
            Integer ordem,
            Long paragemId,
            String nome,
            Integer zona
    ) {}
}
