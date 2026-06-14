package pt.notub.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.notub.trip.entity.ViagemVeiculo;

import java.util.List;

@Repository
public interface ViagemVeiculoRepository extends JpaRepository<ViagemVeiculo, Long> {
    List<ViagemVeiculo> findByVeiculoId(Long veiculoId);
    List<ViagemVeiculo> findByTrajetoId(Long trajetoId);

    @Query("SELECT vv FROM ViagemVeiculo vv WHERE vv.startTime IS NOT NULL AND vv.finishTime IS NULL")
    List<ViagemVeiculo> findActiveViagens();

    @Query("SELECT vv FROM ViagemVeiculo vv WHERE vv.veiculo.id = :veiculoId AND vv.startTime IS NOT NULL AND vv.finishTime IS NULL")
    List<ViagemVeiculo> findActiveByVeiculoId(@Param("veiculoId") Long veiculoId);

    @Query("SELECT vv FROM ViagemVeiculo vv WHERE vv.startTime >= :start AND vv.startTime < :end")
    List<ViagemVeiculo> findViagensIniciadasHoje(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
