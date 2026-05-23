package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.notub.models.Linha;

public interface LinhaRepository extends JpaRepository<Linha, Long> {
}
