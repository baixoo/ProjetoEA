package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.EstadoViagem;
import pt.projetoea.models.ViagemUtilizador;

import java.util.List;

@Repository
public interface ViagemUtilizadorRepository extends JpaRepository<ViagemUtilizador, Long> {
    List<ViagemUtilizador> findByTituloId(Long tituloId);
    List<ViagemUtilizador> findByEstado(EstadoViagem estado);
    List<ViagemUtilizador> findByViagemVeiculoId(Long viagemVeiculoId);
}
