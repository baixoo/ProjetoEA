package pt.notub.network.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.notub.network.entity.PontosDePassagem;
import java.util.List;

public interface PontosDePassagemRepository extends JpaRepository<PontosDePassagem, Long> {
    List<PontosDePassagem> findByTrajetoIdOrderByOrdemAsc(Long trajetoId);

    List<PontosDePassagem> findByParagemId(Long paragemId);
}
