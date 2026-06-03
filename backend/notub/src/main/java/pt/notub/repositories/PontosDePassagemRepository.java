package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.notub.models.PontosDePassagem;
import java.util.List;

public interface PontosDePassagemRepository extends JpaRepository<PontosDePassagem, Long> {
    @Query(value = "SELECT * FROM pontos_de_passagem WHERE trajeto_id = :trajetoId ORDER BY ordem ASC", nativeQuery = true)
    List<PontosDePassagem> findByTrajetoIdOrderByOrdemAsc(@Param("trajetoId") Long trajetoId);

    List<PontosDePassagem> findByParagemId(Long paragemId);
}
