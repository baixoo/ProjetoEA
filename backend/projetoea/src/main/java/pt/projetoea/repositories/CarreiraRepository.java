package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.Carreira;

@Repository
public interface CarreiraRepository extends JpaRepository<Carreira, Long> {
}
