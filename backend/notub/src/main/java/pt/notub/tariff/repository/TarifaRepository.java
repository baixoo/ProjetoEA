package pt.notub.tariff.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.notub.tariff.entity.ModalidadePasse;
import pt.notub.tariff.entity.Tarifa;
import pt.notub.user.entity.TipoUtilizador;
import java.util.List;
import java.util.Optional;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    @Query("SELECT t FROM Tarifa t WHERE " +
           "(:tipoUtilizador IS NULL OR t.tipoUtilizador = :tipoUtilizador) AND " +
           "((:modalidade IS NULL AND t.modalidade IS NULL) OR t.modalidade = :modalidade) AND " +
           "t.nrZonas = :nrZonas")
    Optional<Tarifa> findByCriteria(
            @Param("tipoUtilizador") TipoUtilizador tipoUtilizador,
            @Param("modalidade") ModalidadePasse modalidade,
            @Param("nrZonas") int nrZonas);

    List<Tarifa> findByTipoUtilizador(TipoUtilizador tipoUtilizador);
    List<Tarifa> findByModalidade(ModalidadePasse modalidade);
}
