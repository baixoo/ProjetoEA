package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.notub.models.HistoricoPontos;
import java.util.List;

public interface HistoricoPontosRepository extends JpaRepository<HistoricoPontos, Long> {
    List<HistoricoPontos> findByUtilizadorIdOrderByDataHoraDesc(Long utilizadorId);
}
