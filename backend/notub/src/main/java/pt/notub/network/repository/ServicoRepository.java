package pt.notub.network.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.notub.network.entity.Servico;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    Optional<Servico> findByNomeIgnoreCase(String nome);

    List<Servico> findAllByOrderByNomeAsc();
}
