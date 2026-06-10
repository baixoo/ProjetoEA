package pt.notub.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.notub.auth.entity.TokenRecuperacaoSenha;
import java.util.Optional;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {

    @Query("SELECT t FROM TokenRecuperacaoSenha t WHERE t.token = :token AND t.utilizado = false")
    Optional<TokenRecuperacaoSenha> findValidToken(@Param("token") String token);
}
