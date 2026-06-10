package pt.notub.points.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.notub.points.entity.HistoricoPontos;
import java.util.List;

public interface HistoricoPontosRepository extends JpaRepository<HistoricoPontos, Long> {
    List<HistoricoPontos> findByUtilizadorIdOrderByDataHoraDesc(Long utilizadorId);
}
