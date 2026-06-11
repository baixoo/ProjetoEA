package pt.notub.driver.dto;

public record AvancarViagemResponseDTO(
    Long pontoAtualId,
    String pontoAtualNome,
    Long pontoSeguinteId,
    String pontoSeguinteNome,
    boolean isFinal,
    int tempoAtraso
) {}
