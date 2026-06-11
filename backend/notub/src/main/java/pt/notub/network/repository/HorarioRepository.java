package pt.notub.network.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.notub.network.entity.Horario;

import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
    List<Horario> findByPontoPassagemIdOrderByHoraAsc(Long pontoPassagemId);

    @Query("""
            SELECT h
            FROM Horario h
            JOIN FETCH h.pontoPassagem p
            JOIN FETCH p.paragem
            JOIN FETCH p.trajeto t
            JOIN FETCH h.servico s
            WHERE t.id = :trajetoId
              AND UPPER(s.nome) = UPPER(:servicoNome)
            ORDER BY h.gtfsTripId, p.ordem, h.hora
            """)
    List<Horario> findByTrajetoAndServico(@Param("trajetoId") Long trajetoId,
                                          @Param("servicoNome") String servicoNome);

    @Query("""
            SELECT h
            FROM Horario h
            JOIN FETCH h.pontoPassagem p
            JOIN FETCH p.paragem
            JOIN FETCH p.trajeto t
            JOIN FETCH h.servico s
            WHERE t.id = :trajetoId
              AND UPPER(s.nome) IN :servicoNomes
            ORDER BY h.gtfsTripId, p.ordem, h.hora
            """)
    List<Horario> findByTrajetoAndServicos(@Param("trajetoId") Long trajetoId,
                                           @Param("servicoNomes") List<String> servicoNomes);

    @Query("""
            SELECT h
            FROM Horario h
            JOIN FETCH h.pontoPassagem p
            JOIN FETCH p.paragem
            JOIN FETCH p.trajeto t
            JOIN FETCH h.servico s
            WHERE p.paragem.id = :paragemId
              AND UPPER(s.nome) = UPPER(:servicoNome)
            ORDER BY h.hora, h.gtfsTripId
            """)
    List<Horario> findByParagemAndServico(@Param("paragemId") Long paragemId,
                                          @Param("servicoNome") String servicoNome);

    @Query("""
            SELECT h
            FROM Horario h
            JOIN FETCH h.pontoPassagem p
            JOIN FETCH p.trajeto t
            JOIN FETCH t.linha
            JOIN FETCH h.servico s
            WHERE t.linha.id = :linhaId
              AND UPPER(s.nome) = UPPER(:servicoNome)
            ORDER BY t.id, p.ordem, h.gtfsTripId, h.hora
            """)
    List<Horario> findByLinhaAndServico(@Param("linhaId") Long linhaId,
                                         @Param("servicoNome") String servicoNome);

    @Query("""
            SELECT h
            FROM Horario h
            JOIN FETCH h.pontoPassagem p
            JOIN FETCH p.paragem pg
            JOIN FETCH p.trajeto t
            JOIN FETCH h.servico s
            WHERE UPPER(s.nome) = UPPER(:servicoNome)
            ORDER BY t.id, p.ordem, h.gtfsTripId, h.hora
            """)
    List<Horario> findByServicoNome(@Param("servicoNome") String servicoNome);

    @Query("""
            SELECT h
            FROM Horario h
            JOIN FETCH h.pontoPassagem p
            JOIN FETCH p.paragem pg
            JOIN FETCH p.trajeto t
            JOIN FETCH h.servico s
            WHERE t.id = :trajetoId
              AND pg.id = :paragemId
              AND UPPER(s.nome) = UPPER(:servicoNome)
            ORDER BY h.hora ASC
            """)
    List<Horario> findByTrajetoParagemAndServico(@Param("trajetoId") Long trajetoId,
                                                 @Param("paragemId") Long paragemId,
                                                 @Param("servicoNome") String servicoNome);

    java.util.Optional<Horario> findByPontoPassagemIdAndGtfsTripId(Long pontoPassagemId, String gtfsTripId);
}
