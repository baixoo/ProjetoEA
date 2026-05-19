package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.models.Carreira;

@Repository
public interface CarreiraRepository extends JpaRepository<Carreira, Long> {
}
