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
import pt.notub.ticket.TicketService;
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
    public ResponseEntity<List<BilheteDTO>> buyTickets(@AuthenticatedUser Utilizador utilizador, @RequestBody BuyTicketRequest request) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(BilheteMapper.toDTOList(ticketService.buyTickets(utilizador.getEmail(), request.getQuantidade(), request.getZonaIds())));
    }

    @PostMapping({"/passe/buy", "/passe/comprar"})
    public ResponseEntity<PasseDTO> buyPasse(@AuthenticatedUser Utilizador utilizador, @RequestBody BuyPasseRequest request) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(PasseMapper.toDTO(ticketService.buyPasse(utilizador.getEmail(), request.getModalidade(), request.getZonaIds())));
    }

    @GetMapping({"/my-tickets", "/meus-bilhetes"})
    public ResponseEntity<List<BilheteDTO>> getMyTickets(@AuthenticatedUser Utilizador utilizador) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(BilheteMapper.toDTOList(ticketService.getUserTickets(utilizador.getEmail())));
    }

    @GetMapping({"/my-passe", "/meu-passe"})
    public ResponseEntity<PasseDTO> getMyPasse(@AuthenticatedUser Utilizador utilizador) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        Passe passe = ticketService.getUserPasse(utilizador.getEmail());
        if (passe == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(PasseMapper.toDTO(passe));
    }
}
