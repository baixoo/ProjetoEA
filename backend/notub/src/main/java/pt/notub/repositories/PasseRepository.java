package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.notub.models.Passe;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasseRepository extends JpaRepository<Passe, Long> {

    List<Passe> findByUtilizadorIdOrderByInicioAsc(Long utilizadorId);

    // Passe ativo no momento atual (inicio <= now <= fim)
    @Query("SELECT p FROM Passe p WHERE p.utilizador.id = :uid AND p.inicio <= :now AND p.fim >= :now")
    Optional<Passe> findPasseAtivo(@Param("uid") Long utilizadorId, @Param("now") LocalDateTime now);

    // Verifica se existe sobreposição de intervalo para um utilizador
    // Um novo passe [novoInicio, novoFim] conflitua com um existente [p.inicio, p.fim]
    // quando novoInicio < p.fim AND novoFim > p.inicio
    @Query("SELECT COUNT(p) > 0 FROM Passe p WHERE p.utilizador.id = :uid AND p.inicio < :novoFim AND p.fim > :novoInicio")
    boolean existeConflito(@Param("uid") Long utilizadorId,
                           @Param("novoInicio") LocalDateTime novoInicio,
                           @Param("novoFim") LocalDateTime novoFim);
}
