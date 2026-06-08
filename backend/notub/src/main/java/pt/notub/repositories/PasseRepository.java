package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.models.Passe;

import java.util.Optional;

@Repository
public interface PasseRepository extends JpaRepository<Passe, Long> {
    Optional<Passe> findByUtilizadorId(Long utilizadorId);
}
