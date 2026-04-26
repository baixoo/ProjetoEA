package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.Trajeto;

import java.util.List;

@Repository
public interface TrajetoRepository extends JpaRepository<Trajeto, Long> {
    List<Trajeto> findByCarreiraId(Long carreiraId);
}
