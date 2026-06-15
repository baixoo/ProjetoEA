package pt.notub.trip.dto;

public record IniciarViagemRequest(String tipoTitulo, Long quantidade, Long paragemEntradaId, Long viagemVeiculoId) {
}
