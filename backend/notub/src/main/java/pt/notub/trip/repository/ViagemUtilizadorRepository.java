package pt.notub.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
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

    List<ViagemUtilizador> findByUtilizadorId(Long utilizadorId);
}
