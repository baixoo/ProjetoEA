package pt.notub.services;

import org.springframework.stereotype.Service;
import pt.notub.models.*;
import pt.notub.repositories.*;

import java.time.LocalDateTime;
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

    public Bilhete criarBilhete(Utilizador utilizador, List<Zona> zonas) {
        Bilhete bilhete = new Bilhete();
        bilhete.setUtilizador(utilizador);
        bilhete.setUsado(false);
        bilhete.setZonas(zonas);
        return bilheteRepository.save(bilhete);
    }

    public Passe criarPasse(Utilizador utilizador, ModalidadePasse modalidade, List<Zona> zonas) {
        passeRepository.findByUtilizadorId(utilizador.getId()).ifPresent(passe -> {
            if (passe.getFim() != null && passe.getFim().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Utilizador ja tem um passe ativo ate " + passe.getFim());
            }
        });
        Passe passe = new Passe();
        passe.setUtilizador(utilizador);
        passe.setModalidade(modalidade);
        passe.setInicio(LocalDateTime.now());
        passe.setFim(calcularFimPasse(modalidade));
        passe.setZonas(zonas);
        return passeRepository.save(passe);
    }

    public List<Bilhete> buyTickets(String email, int quantidade, List<Long> zonaIds) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
        List<Zona> zonas = zonaRepository.findAllById(zonaIds);
        List<Bilhete> bilhetes = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            bilhetes.add(criarBilhete(utilizador, zonas));
        }
        servicoPontos.atribuirPontosCompra(utilizador.getId());
        return bilhetes;
    }

    public Passe buyPasse(String email, ModalidadePasse modalidade, List<Long> zonaIds) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
        List<Zona> zonas = zonaRepository.findAllById(zonaIds);
        Passe passe = criarPasse(utilizador, modalidade, zonas);
        servicoPontos.atribuirPontosCompra(utilizador.getId());
        return passe;
    }

    public List<Bilhete> getUserTickets(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
        return bilheteRepository.findByUtilizadorId(utilizador.getId());
    }

    public Passe getUserPasse(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
        return passeRepository.findByUtilizadorId(utilizador.getId()).orElse(null);
    }

    private LocalDateTime calcularFimPasse(ModalidadePasse modalidade) {
        return switch (modalidade) {
            case H24 -> LocalDateTime.now().plusHours(24);
            case H48 -> LocalDateTime.now().plusHours(48);
            case H72 -> LocalDateTime.now().plusHours(72);
            case SEMANAL -> LocalDateTime.now().plusWeeks(1);
            case MENSAL -> LocalDateTime.now().plusMonths(1);
            case ANUAL -> LocalDateTime.now().plusYears(1);
        };
    }
}
