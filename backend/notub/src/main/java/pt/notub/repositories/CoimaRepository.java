package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.models.Coima;

@Repository
public interface CoimaRepository extends JpaRepository<Coima, Long> {
}
