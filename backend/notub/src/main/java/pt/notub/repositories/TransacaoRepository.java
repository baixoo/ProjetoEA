package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.models.EstadoPagamento;
import pt.notub.models.Transacao;

import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByTituloId(Long tituloId);
    List<Transacao> findByEstadoPagamento(EstadoPagamento estado);
}
