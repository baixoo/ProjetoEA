package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.notub.models.Trajeto;
import java.util.List;

public interface TrajetoRepository extends JpaRepository<Trajeto, Long> {
    List<Trajeto> findByLinhaId(Long linhaId);

    @Query("SELECT DISTINCT t FROM Trajeto t JOIN t.pontosDePassagem p WHERE p.paragem.id = :paragemId")
    List<Trajeto> findByParagemId(@Param("paragemId") Long paragemId);
}
