package pt.notub.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.ticket.entity.Passe;

import java.util.Optional;

@Repository
public interface PasseRepository extends JpaRepository<Passe, Long> {
    Optional<Passe> findByUtilizadorId(Long utilizadorId);
}
