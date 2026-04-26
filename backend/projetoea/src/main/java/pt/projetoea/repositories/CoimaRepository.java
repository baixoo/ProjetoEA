package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.Coima;

@Repository
public interface CoimaRepository extends JpaRepository<Coima, Long> {
}
