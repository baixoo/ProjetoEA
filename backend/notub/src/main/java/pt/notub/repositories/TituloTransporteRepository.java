package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.models.TituloTransporte;

@Repository
public interface TituloTransporteRepository extends JpaRepository<TituloTransporte, Long> {
}
