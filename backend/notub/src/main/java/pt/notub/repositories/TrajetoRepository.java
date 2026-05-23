package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.notub.models.Trajeto;
import java.util.List;

public interface TrajetoRepository extends JpaRepository<Trajeto, Long> {
    List<Trajeto> findByLinhaId(Long linhaId);
}
