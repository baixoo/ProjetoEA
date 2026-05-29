package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.notub.models.PontosDePassagem;
import java.util.List;

public interface PontosDePassagemRepository extends JpaRepository<PontosDePassagem, Long> {
    List<PontosDePassagem> findByTrajetoIdOrderByOrdemAsc(Long trajetoId);
    List<PontosDePassagem> findByParagemId(Long paragemId);
}
