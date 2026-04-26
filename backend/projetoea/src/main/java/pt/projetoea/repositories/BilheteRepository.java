package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.Bilhete;

import java.util.List;

@Repository
public interface BilheteRepository extends JpaRepository<Bilhete, Long> {
    List<Bilhete> findByUtilizadorId(Long utilizadorId);
}
