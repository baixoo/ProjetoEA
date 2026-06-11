package pt.notub.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.notub.trip.entity.EstadoViagem;
import pt.notub.trip.entity.ViagemUtilizador;

import java.util.List;

@Repository
public interface ViagemUtilizadorRepository extends JpaRepository<ViagemUtilizador, Long> {
    List<ViagemUtilizador> findByTituloId(Long tituloId);
    List<ViagemUtilizador> findByEstado(EstadoViagem estado);
    long countByEstado(EstadoViagem estado);
    List<ViagemUtilizador> findByViagemVeiculoId(Long viagemVeiculoId);

    @Query("SELECT v FROM ViagemUtilizador v " +
       "JOIN FETCH v.viagemVeiculo vv " +
       "JOIN FETCH vv.veiculo vec " +
       "JOIN FETCH vv.trajeto traj " +
       "JOIN FETCH traj.linha lin " +
       "JOIN FETCH v.titulo t " +
       "WHERE t.utilizador.id = :utilizadorId")
    List<ViagemUtilizador> findByUtilizadorId(@Param("utilizadorId") Long utilizadorId);
}
