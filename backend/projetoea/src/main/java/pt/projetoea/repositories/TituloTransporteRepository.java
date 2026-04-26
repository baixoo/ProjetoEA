package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.TituloTransporte;

@Repository
public interface TituloTransporteRepository extends JpaRepository<TituloTransporte, Long> {
}
