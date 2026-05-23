package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.models.Paragem;

import java.util.Optional;

@Repository
public interface ParagemRepository extends JpaRepository<Paragem, Long> {
    Optional<Paragem> findByNome(String nome);
}
