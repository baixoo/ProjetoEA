package pt.notub.driver.dto;

public record StartViagemRequest(Long veiculoId, Long trajetoId, String serviceId, String gtfsTripId) {
}
