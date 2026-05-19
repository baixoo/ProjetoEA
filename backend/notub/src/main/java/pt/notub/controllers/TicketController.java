package pt.notub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pt.notub.dto.request.BuyPasseRequest;
import pt.notub.dto.request.BuyTicketRequest;
import pt.notub.security.UserDetailsImpl;
import pt.notub.services.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buyTickets(@RequestBody BuyTicketRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
        return ResponseEntity.ok(ticketService.buyTickets(email, request.getQuantidade()));
    }

    @PostMapping("/passe/buy")
    public ResponseEntity<?> buyPasse(@RequestBody BuyPasseRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
        return ResponseEntity.ok(ticketService.buyPasse(email, request.getModalidade()));
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<?> getMyTickets() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
        return ResponseEntity.ok(ticketService.getUserTickets(email));
    }

    @GetMapping("/my-passe")
    public ResponseEntity<?> getMyPasse() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
        return ResponseEntity.ok(ticketService.getUserPasse(email));
    }
}
