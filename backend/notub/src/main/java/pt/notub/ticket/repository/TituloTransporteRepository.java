package pt.notub.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pt.notub.ticket.entity.TipoTituloTransporte;
import pt.notub.ticket.entity.TituloTransporte;

import java.util.List;

@Repository
public interface TituloTransporteRepository extends JpaRepository<TituloTransporte, Long> {

    List<TituloTransporte> findByTipoAndUtilizadorId(TipoTituloTransporte tipo, Long utilizadorId);
}
