package pt.notub.driver.dto;

import java.time.LocalTime;

public record ViagemScheduleDTO(
    LocalTime horaPartida,
    String gtfsTripId,
    String serviceId
) {}
