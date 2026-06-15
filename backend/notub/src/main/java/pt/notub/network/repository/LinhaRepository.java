package pt.notub.network.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.notub.network.dto.LinhaSummaryDTO;
import pt.notub.network.entity.Linha;

import java.util.List;

public interface LinhaRepository extends JpaRepository<Linha, Long> {

    @Query("select new pt.notub.network.dto.LinhaSummaryDTO(l.id, l.nome) from Linha l order by l.nome, l.id")
    List<LinhaSummaryDTO> findAllSummaries();
}
