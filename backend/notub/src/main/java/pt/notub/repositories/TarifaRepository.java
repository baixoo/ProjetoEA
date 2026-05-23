package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.notub.models.ModalidadePasse;
import pt.notub.models.Tarifa;
import pt.notub.models.TipoUtilizador;
import java.util.List;
import java.util.Optional;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    Optional<Tarifa> findByTipoUtilizadorAndModalidadeAndNrZonas(TipoUtilizador tipoUtilizador, ModalidadePasse modalidade, int nrZonas);
    Optional<Tarifa> findByTipoUtilizadorIsNullAndModalidadeAndNrZonas(ModalidadePasse modalidade, int nrZonas);
    List<Tarifa> findByTipoUtilizador(TipoUtilizador tipoUtilizador);
    List<Tarifa> findByModalidade(ModalidadePasse modalidade);
}
