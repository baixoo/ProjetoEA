package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.SequenciaParagem;

import java.util.List;

@Repository
public interface SequenciaParagemRepository extends JpaRepository<SequenciaParagem, Long> {
    List<SequenciaParagem> findByTrajetoIdOrderByOrdemAsc(Long trajetoId);
}
