package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.Paragem;

@Repository
public interface ParagemRepository extends JpaRepository<Paragem, Long> {
}
