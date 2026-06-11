package pt.notub.vehicle.dto;

import pt.notub.vehicle.entity.Point;

public record VeiculoRequest(
        String matricula,
        Integer nLugares,
        Integer lotacaoAtual,
        Integer tempoAtraso,
        Point localizacaoAtual,
        Long linhaId
) {
}
