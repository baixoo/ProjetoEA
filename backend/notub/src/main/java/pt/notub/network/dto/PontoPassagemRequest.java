package pt.notub.network.dto;

import java.time.LocalTime;

public record PontoPassagemRequest(
        Integer ordem,
        LocalTime horaChegada,
        Integer tempoDesdeInicio,
        Long paragemId
) {
}
