package pt.notub.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.dto.request.CheckoutRequest;
import pt.notub.dto.response.CheckoutResponse;
import pt.notub.dto.response.PagamentoStatusResponse;
import pt.notub.models.Utilizador;
import pt.notub.security.AuthenticatedUser;
import pt.notub.services.PagamentoService;

import java.util.Map;

@RestController
@RequestMapping("/api/pagamento")
public class PagamentoController {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoController.class);

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@AuthenticatedUser Utilizador utilizador, @RequestBody CheckoutRequest request) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        try {
            CheckoutResponse response = pagamentoService.iniciarCheckout(utilizador.getEmail(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro no checkout: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{id}/estado")
    public ResponseEntity<?> verificarEstado(@AuthenticatedUser Utilizador utilizador, @PathVariable Long id) {
        if (utilizador == null) return ResponseEntity.status(401).build();
        try {
            PagamentoStatusResponse response = pagamentoService.verificarEstado(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
