package pt.notub.ticket;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.BilheteMapper;
import pt.notub.common.mapper.PasseMapper;
import pt.notub.ticket.dto.BuyPasseRequest;
import pt.notub.ticket.dto.BuyTicketRequest;
import pt.notub.models.Passe;
import pt.notub.models.Utilizador;
import pt.notub.security.AuthenticatedUser;
import pt.notub.ticket.dto.BilheteDTO;
import pt.notub.ticket.dto.PasseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping({"/buy", "/comprar"})
    public ResponseEntity<List<BilheteDTO>> buyTickets(
            @AuthenticatedUser Utilizador utilizador,
            @RequestBody BuyTicketRequest request) {
        return ResponseEntity.ok(
                BilheteMapper.toDTOList(
                        ticketService.buyTickets(utilizador.getEmail(),
                                request.getQuantidade(), request.getZonaId())));
    }

    @PostMapping({"/passe/buy", "/passe/comprar"})
    public ResponseEntity<PasseDTO> buyPasse(
            @AuthenticatedUser Utilizador utilizador,
            @RequestBody BuyPasseRequest request) {
        return ResponseEntity.ok(
                PasseMapper.toDTO(
                        ticketService.buyPasse(utilizador.getEmail(),
                                request.getModalidade(), request.getZonaId(),
                                request.getMesInicio(), request.getAnoInicio())));
    }

    @GetMapping({"/my-tickets", "/meus-bilhetes"})
    public ResponseEntity<List<BilheteDTO>> getMyTickets(
            @AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(
                BilheteMapper.toDTOList(ticketService.getUserTickets(utilizador.getEmail())));
    }

    /** Todos os passes do utilizador, ordenados por início. */
    @GetMapping({"/my-passes", "/meus-passes"})
    public ResponseEntity<List<PasseDTO>> getMyPasses(
            @AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(
                PasseMapper.toDTOList(ticketService.getUserPasses(utilizador.getEmail())));
    }

    /** Passe atualmente ativo (204 se não existir). */
    @GetMapping({"/my-passe/ativo", "/meu-passe/ativo"})
    public ResponseEntity<PasseDTO> getPasseAtivo(
            @AuthenticatedUser Utilizador utilizador) {
        Passe passe = ticketService.getPasseAtivo(utilizador.getEmail());
        if (passe == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(PasseMapper.toDTO(passe));
    }
}
