package pt.notub.driver.dto;

import java.time.LocalTime;

public record ViagemScheduleDTO(
    Long id,
    LocalTime horaPartida,
    String gtfsTripId,
    String serviceId
) {}
