package pt.notub.network.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.network.entity.Paragem;

import java.util.Optional;

@Repository
public interface ParagemRepository extends JpaRepository<Paragem, Long> {
    Optional<Paragem> findByNome(String nome);
}
