package pt.notub.network.dto;

import pt.notub.vehicle.entity.Point;

public record ParagemRequest(String nome, Point localizacao, Long zonaId) {
}
