package pt.notub.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.notub.trip.entity.Viagem;
import java.time.LocalTime;
import java.util.List;

public interface ViagemRepository extends JpaRepository<Viagem, Long> {
    List<Viagem> findByTrajetoId(Long trajetoId);

    @Query("SELECT v FROM Viagem v WHERE v.trajeto.id = :trajetoId AND v.serviceId = :serviceId AND v.horaPartida >= :afterTime ORDER BY v.horaPartida ASC")
    List<Viagem> findNextDepartures(@Param("trajetoId") Long trajetoId, @Param("serviceId") String serviceId, @Param("afterTime") LocalTime afterTime);

    @Query("SELECT v FROM Viagem v WHERE v.trajeto.id = :trajetoId AND v.serviceId = :serviceId ORDER BY v.horaPartida ASC")
    List<Viagem> findByTrajetoAndService(@Param("trajetoId") Long trajetoId, @Param("serviceId") String serviceId);

    @Query("SELECT DISTINCT v.serviceId FROM Viagem v WHERE v.trajeto.id = :trajetoId")
    List<String> findDistinctServiceIds(@Param("trajetoId") Long trajetoId);
}
