package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.models.Paragem;

@Repository
public interface ParagemRepository extends JpaRepository<Paragem, Long> {
}
