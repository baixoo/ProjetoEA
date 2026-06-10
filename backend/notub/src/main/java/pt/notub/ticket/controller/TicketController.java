package pt.notub.ticket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.ticket.dto.BuyPasseRequest;
import pt.notub.ticket.dto.BuyTicketRequest;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;
import pt.notub.ticket.service.TicketService;
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
    public ResponseEntity<List<BilheteDTO>> buyTickets(@AuthenticatedUser AuthenticatedUserContext utilizador,
                                                       @RequestBody BuyTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.buyTickets(utilizador.email(), request.getQuantidade(), request.getZonaId()));
    }

    @PostMapping({"/passe/buy", "/passe/comprar"})
    public ResponseEntity<PasseDTO> buyPasse(@AuthenticatedUser AuthenticatedUserContext utilizador,
                                             @RequestBody BuyPasseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.buyPasse(utilizador.email(), request.getModalidade(), request.getZonaId()));
    }

    @GetMapping({"/my-tickets", "/meus-bilhetes"})
    public ResponseEntity<List<BilheteDTO>> getMyTickets(@AuthenticatedUser AuthenticatedUserContext utilizador) {
        return ResponseEntity.ok(ticketService.getUserTickets(utilizador.email()));
    }

    @GetMapping({"/my-passe", "/meu-passe"})
    public ResponseEntity<PasseDTO> getMyPasse(@AuthenticatedUser AuthenticatedUserContext utilizador) {
        return ResponseEntity.ok(ticketService.getUserPasse(utilizador.email()));
    }
}
