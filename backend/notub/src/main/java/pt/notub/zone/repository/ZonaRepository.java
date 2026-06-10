package pt.notub.zone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.zone.entity.Zona;

import java.util.Optional;

@Repository
public interface ZonaRepository extends JpaRepository<Zona, Long> {
    Optional<Zona> findByNome(String nome);
}
