package pt.notub.ticket.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.points.service.ServicoPontos;
import pt.notub.tariff.entity.ModalidadePasse;
import pt.notub.ticket.dto.BilheteDTO;
import pt.notub.ticket.dto.PasseDTO;
import pt.notub.ticket.entity.Bilhete;
import pt.notub.ticket.entity.Passe;
import pt.notub.ticket.entity.TipoTituloTransporte;
import pt.notub.ticket.mapper.BilheteMapper;
import pt.notub.ticket.mapper.PasseMapper;
import pt.notub.ticket.repository.BilheteRepository;
import pt.notub.ticket.repository.PasseRepository;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;
import pt.notub.zone.entity.Zona;
import pt.notub.zone.repository.ZonaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    private static final int MAX_ZONE_NUM = 3;

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

    public List<BilheteDTO> buyTickets(String email, int quantidade, Long zonaId) {
        if (quantidade <= 0) {
            throw new PedidoInvalidoException("Quantidade deve ser maior que zero");
        }
        Utilizador utilizador = findUtilizadorByEmail(email);
        Zona zona = findZona(zonaId);
        List<Bilhete> bilhetes = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            bilhetes.add(criarBilhete(utilizador, zona));
        }
        return BilheteMapper.toDTOList(bilhetes);
    }

    public PasseDTO buyPasse(String email, ModalidadePasse modalidade, Long zonaId) {
        Utilizador utilizador = findUtilizadorByEmail(email);
        Zona zona = findZona(zonaId);
        return PasseMapper.toDTO(criarPasse(utilizador, modalidade, zona));
    }

    public List<BilheteDTO> getUserTickets(String email) {
        Utilizador utilizador = findUtilizadorByEmail(email);
        return BilheteMapper.toDTOList(bilheteRepository.findByUtilizadorId(utilizador.getId()));
    }

    public PasseDTO getUserPasse(String email) {
        Utilizador utilizador = findUtilizadorByEmail(email);
        Passe passe = passeRepository.findByUtilizadorId(utilizador.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Passe nao encontrado"));
        return PasseMapper.toDTO(passe);
    }

    private Bilhete criarBilhete(Utilizador utilizador, Zona zona) {
        Bilhete bilhete = new Bilhete();
        bilhete.setUtilizador(utilizador);
        bilhete.setTipo(TipoTituloTransporte.BILHETE);
        bilhete.setUsado(false);
        bilhete.setZona(zona);
        return bilheteRepository.save(bilhete);
    }

    private Passe criarPasse(Utilizador utilizador, ModalidadePasse modalidade, Zona zona) {
        passeRepository.findByUtilizadorId(utilizador.getId()).ifPresent(passe -> {
            if (passe.getFim() != null && passe.getFim().isAfter(LocalDateTime.now())) {
                throw new PedidoInvalidoException("Utilizador ja tem um passe ativo ate " + passe.getFim());
            }
        });
        Passe passe = new Passe();
        passe.setUtilizador(utilizador);
        passe.setTipo(TipoTituloTransporte.PASSE);
        passe.setModalidade(modalidade);
        passe.setInicio(LocalDateTime.now());
        passe.setFim(calcularFimPasse(modalidade));
        passe.setZona(zona);
        return passeRepository.save(passe);
    }

    private Utilizador findUtilizadorByEmail(String email) {
        return utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
    }

    private Zona findZona(Long zonaId) {
        if (zonaId == null) {
            throw new PedidoInvalidoException("Zona invalida");
        }
        Zona zona = zonaRepository.findById(zonaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        int num = zona.getNum();
        if (num < 1 || num > MAX_ZONE_NUM) {
            throw new PedidoInvalidoException("Zona invalida");
        }
        return zona;
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
