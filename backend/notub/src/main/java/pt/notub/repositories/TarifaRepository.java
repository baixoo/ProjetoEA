package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.models.ModalidadePasse;
import pt.notub.models.Tarifa;
import pt.notub.models.TipoPerfil;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    Optional<Tarifa> findByPerfilAndModalidade(TipoPerfil perfil, ModalidadePasse modalidade);
    List<Tarifa> findByPerfil(TipoPerfil perfil);
    List<Tarifa> findByModalidade(ModalidadePasse modalidade);
}
