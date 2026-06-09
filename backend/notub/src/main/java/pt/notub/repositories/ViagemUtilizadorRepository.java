package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.notub.models.EstadoViagem;
import pt.notub.models.ViagemUtilizador;

import java.util.List;

@Repository
public interface ViagemUtilizadorRepository extends JpaRepository<ViagemUtilizador, Long> {
    List<ViagemUtilizador> findByTituloId(Long tituloId);
    List<ViagemUtilizador> findByEstado(EstadoViagem estado);
    List<ViagemUtilizador> findByViagemVeiculoId(Long viagemVeiculoId);

    @Query("SELECT v FROM ViagemUtilizador v " +
       "JOIN FETCH v.viagemVeiculo vv " +     
       "JOIN FETCH vv.veiculo vec " +          
       "JOIN FETCH vv.trajeto traj " +        
       "JOIN FETCH traj.linha lin " +         
       "JOIN v.titulo t " +
       "LEFT JOIN pt.notub.models.Bilhete b ON b.id = t.id " +
       "LEFT JOIN pt.notub.models.Passe p ON p.id = t.id " +
       "WHERE b.utilizador.id = :utilizadorId OR p.utilizador.id = :utilizadorId")
List<ViagemUtilizador> findByUtilizadorId(@Param("utilizadorId") Long utilizadorId);
}
