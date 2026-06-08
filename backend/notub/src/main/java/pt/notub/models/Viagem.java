package pt.notub.models;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
public class Viagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trajeto_id")
    private Trajeto trajeto;

    @Column(name = "service_id")
    private String serviceId;

    @Column(name = "hora_partida")
    private LocalTime horaPartida;

    @Column(name = "gtfs_trip_id")
    private String gtfsTripId;

    public Viagem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Trajeto getTrajeto() { return trajeto; }
    public void setTrajeto(Trajeto trajeto) { this.trajeto = trajeto; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public LocalTime getHoraPartida() { return horaPartida; }
    public void setHoraPartida(LocalTime horaPartida) { this.horaPartida = horaPartida; }
    public String getGtfsTripId() { return gtfsTripId; }
    public void setGtfsTripId(String gtfsTripId) { this.gtfsTripId = gtfsTripId; }
}
