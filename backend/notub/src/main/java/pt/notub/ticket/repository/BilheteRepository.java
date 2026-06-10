package pt.notub.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.ticket.entity.Bilhete;

import java.util.List;

@Repository
public interface BilheteRepository extends JpaRepository<Bilhete, Long> {
    List<Bilhete> findByUtilizadorId(Long utilizadorId);
}
