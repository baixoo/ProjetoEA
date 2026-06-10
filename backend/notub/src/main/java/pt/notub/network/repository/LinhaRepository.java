package pt.notub.network.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.notub.network.entity.Linha;

public interface LinhaRepository extends JpaRepository<Linha, Long> {
}
