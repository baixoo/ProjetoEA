package pt.notub.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.notub.payment.entity.EstadoPagamento;
import pt.notub.payment.entity.Transacao;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByTituloId(Long tituloId);
    List<Transacao> findByEstadoPagamento(EstadoPagamento estado);
    List<Transacao> findByUtilizadorId(Long utilizadorId);
    Optional<Transacao> findByToken(String token);

    @Query("SELECT t FROM Transacao t WHERE t.utilizador.id = :userId AND t.estadoPagamento = :estado")
    Optional<Transacao> findActiveByUser(@Param("userId") Long userId, @Param("estado") EstadoPagamento estado);
}
