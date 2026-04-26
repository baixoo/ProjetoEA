package pt.projetoea.services;

import org.springframework.stereotype.Service;
import pt.projetoea.models.*;
import pt.projetoea.repositories.BilheteRepository;
import pt.projetoea.repositories.PasseRepository;
import pt.projetoea.repositories.UtilizadorRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    private final BilheteRepository bilheteRepository;
    private final PasseRepository passeRepository;
    private final UtilizadorRepository utilizadorRepository;

    public TicketService(BilheteRepository bilheteRepository, PasseRepository passeRepository,
                         UtilizadorRepository utilizadorRepository) {
        this.bilheteRepository = bilheteRepository;
        this.passeRepository = passeRepository;
        this.utilizadorRepository = utilizadorRepository;
    }

    public List<Bilhete> buyTickets(String email, int quantidade) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Bilhete> bilhetes = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            Bilhete bilhete = new Bilhete();
            bilhete.setUtilizador(utilizador);
            bilhete.setUsado(false);
            bilhetes.add(bilheteRepository.save(bilhete));
        }
        return bilhetes;
    }

    public Passe buyPasse(String email, ModalidadePasse modalidade) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user already has a passe
        passeRepository.findByUtilizadorId(utilizador.getId()).ifPresent(passe -> {
            throw new RuntimeException("User already has an active passe");
        });

        Passe passe = new Passe();
        passe.setUtilizador(utilizador);
        passe.setModalidade(modalidade);
        passe.setInicio(LocalDateTime.now());

        // Calculate end date based on modalidade
        switch (modalidade) {
            case H24 -> passe.setFim(LocalDateTime.now().plusHours(24));
            case H48 -> passe.setFim(LocalDateTime.now().plusHours(48));
            case H72 -> passe.setFim(LocalDateTime.now().plusHours(72));
            case SEMANAL -> passe.setFim(LocalDateTime.now().plusWeeks(1));
            case MENSAL -> passe.setFim(LocalDateTime.now().plusMonths(1));
            case ANUAL -> passe.setFim(LocalDateTime.now().plusYears(1));
        }

        return passeRepository.save(passe);
    }

    public List<Bilhete> getUserTickets(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return bilheteRepository.findByUtilizadorId(utilizador.getId());
    }

    public Passe getUserPasse(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return passeRepository.findByUtilizadorId(utilizador.getId()).orElse(null);
    }
}
