package pt.projetoea.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.projetoea.models.ModalidadePasse;
import pt.projetoea.models.Tarifa;
import pt.projetoea.models.TipoPerfil;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    Optional<Tarifa> findByPerfilAndModalidade(TipoPerfil perfil, ModalidadePasse modalidade);
    List<Tarifa> findByPerfil(TipoPerfil perfil);
    List<Tarifa> findByModalidade(ModalidadePasse modalidade);
}
