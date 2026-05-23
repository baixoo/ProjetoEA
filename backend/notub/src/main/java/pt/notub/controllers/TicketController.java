package pt.notub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.dto.request.BuyPasseRequest;
import pt.notub.dto.request.BuyTicketRequest;
import pt.notub.models.Utilizador;
import pt.notub.security.AuthenticatedUser;
import pt.notub.services.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping({"/buy", "/comprar"})
    public ResponseEntity<?> buyTickets(@AuthenticatedUser Utilizador utilizador, @RequestBody BuyTicketRequest request) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ticketService.buyTickets(utilizador.getEmail(), request.getQuantidade(), request.getZonaIds()));
    }

    @PostMapping({"/passe/buy", "/passe/comprar"})
    public ResponseEntity<?> buyPasse(@AuthenticatedUser Utilizador utilizador, @RequestBody BuyPasseRequest request) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ticketService.buyPasse(utilizador.getEmail(), request.getModalidade(), request.getZonaIds()));
    }

    @GetMapping({"/my-tickets", "/meus-bilhetes"})
    public ResponseEntity<?> getMyTickets(@AuthenticatedUser Utilizador utilizador) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ticketService.getUserTickets(utilizador.getEmail()));
    }

    @GetMapping({"/my-passe", "/meu-passe"})
    public ResponseEntity<?> getMyPasse(@AuthenticatedUser Utilizador utilizador) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ticketService.getUserPasse(utilizador.getEmail()));
    }
}
