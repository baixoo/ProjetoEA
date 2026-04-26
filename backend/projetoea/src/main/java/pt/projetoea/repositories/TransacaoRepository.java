package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.EstadoPagamento;
import pt.projetoea.models.Transacao;

import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByTituloId(Long tituloId);
    List<Transacao> findByEstadoPagamento(EstadoPagamento estado);
}
