package pt.notub.trip.repository;

import java.util.List;
import pt.notub.trip.entity.Monitorizacao;
import pt.notub.user.entity.Utilizador;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface MonitorizacaoRepository extends JpaRepository<Monitorizacao, Long> {
    
    List<Monitorizacao> findByViagemVeiculoId(Long viagemVeiculoId);
    List<Monitorizacao> findByUtilizadorId(Long userId);
 
    Optional<Monitorizacao> findByUtilizadorIdAndViagemVeiculoId(Long userId, Long viagemVeiculoId);

    @Query("SELECT m.utilizador FROM Monitorizacao m WHERE m.viagemVeiculo.id = :viagemVeiculoId")
    List<Utilizador> getSubscribedUsers(@Param("viagemVeiculoId") Long viagemVeiculoId);
}
