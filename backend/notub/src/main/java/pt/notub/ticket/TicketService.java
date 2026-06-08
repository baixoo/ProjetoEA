package pt.notub.ticket;

import org.springframework.stereotype.Service;
import pt.notub.exception.RecursoNaoEncontradoException;
import pt.notub.models.*;
import pt.notub.points.ServicoPontos;
import pt.notub.repositories.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    private final BilheteRepository bilheteRepository;
    private final PasseRepository passeRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ZonaRepository zonaRepository;
    private final ServicoPontos servicoPontos;

    public TicketService(BilheteRepository bilheteRepository, PasseRepository passeRepository,
                         UtilizadorRepository utilizadorRepository, ZonaRepository zonaRepository,
                         ServicoPontos servicoPontos) {
        this.bilheteRepository = bilheteRepository;
        this.passeRepository = passeRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.zonaRepository = zonaRepository;
        this.servicoPontos = servicoPontos;
    }

    public Bilhete criarBilhete(Utilizador utilizador, Zona zona) {
        Bilhete bilhete = new Bilhete();
        bilhete.setUtilizador(utilizador);
        bilhete.setUsado(false);
        bilhete.setZona(zona);
        return bilheteRepository.save(bilhete);
    }

    /**
     * Cria um passe com início no dia 1 do mês/ano indicado.
     * Para MENSAL: fim = último momento do mesmo mês.
     * Para ANUAL:  fim = último momento do mês 12 do ano de início.
     * Rejeita se o intervalo colidir com qualquer passe já existente do utilizador.
     */
    public Passe criarPasse(Utilizador utilizador, ModalidadePasse modalidade, Zona zona,
                            int mesInicio, int anoInicio) {
        LocalDateTime inicio = LocalDate.of(anoInicio, mesInicio, 1).atStartOfDay();
        LocalDateTime fim = calcularFimPasse(modalidade, YearMonth.of(anoInicio, mesInicio));

        if (passeRepository.existeConflito(utilizador.getId(), inicio, fim)) {
            throw new RuntimeException(
                "Já existe um passe para o período selecionado. Escolha um mês diferente.");
        }

        Passe passe = new Passe();
        passe.setUtilizador(utilizador);
        passe.setModalidade(modalidade);
        passe.setInicio(inicio);
        passe.setFim(fim);
        passe.setZona(zona);
        return passeRepository.save(passe);
    }

    public List<Bilhete> buyTickets(String email, int quantidade, Long zonaId) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        Zona zona = zonaRepository.findById(zonaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        List<Bilhete> bilhetes = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            bilhetes.add(criarBilhete(utilizador, zona));
        }
        return bilhetes;
    }

    public Passe buyPasse(String email, ModalidadePasse modalidade, Long zonaId,
                          int mesInicio, int anoInicio) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        Zona zona = zonaRepository.findById(zonaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        return criarPasse(utilizador, modalidade, zona, mesInicio, anoInicio);
    }

    public List<Bilhete> getUserTickets(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        return bilheteRepository.findByUtilizadorId(utilizador.getId());
    }

    /** Todos os passes do utilizador, ordenados por data de início. */
    public List<Passe> getUserPasses(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        return passeRepository.findByUtilizadorIdOrderByInicioAsc(utilizador.getId());
    }

    /** Passe ativo neste momento (null se não existir). */
    public Passe getPasseAtivo(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        return passeRepository.findPasseAtivo(utilizador.getId(), LocalDateTime.now()).orElse(null);
    }

    // ----- helpers -----

    private LocalDateTime calcularFimPasse(ModalidadePasse modalidade, YearMonth ym) {
        return switch (modalidade) {
            case MENSAL -> ym.atEndOfMonth().atTime(23, 59, 59);
            case ANUAL  -> YearMonth.of(ym.getYear(), 12).atEndOfMonth().atTime(23, 59, 59);
            // modalidades de curta duração não são usadas no fluxo de compra,
            // mas ficam mapeadas para não quebrar o switch
            default -> LocalDateTime.now().plusHours(24);
        };
    }
}
