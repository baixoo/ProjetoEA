package pt.notub.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.ticket.entity.TituloTransporte;

@Repository
public interface TituloTransporteRepository extends JpaRepository<TituloTransporte, Long> {
}
