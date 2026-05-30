package pt.notub.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.payment.dto.CheckoutRequest;
import pt.notub.payment.dto.CheckoutResponse;
import pt.notub.payment.dto.PagamentoStatusResponse;
import pt.notub.models.Utilizador;
import pt.notub.security.AuthenticatedUser;
import pt.notub.payment.PagamentoService;

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
        try {
            CheckoutResponse response = pagamentoService.iniciarCheckout(utilizador.getEmail(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro no checkout: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/estado")
    public ResponseEntity<?> verificarEstado(
            @AuthenticatedUser Utilizador utilizador,
            @RequestParam("t") String token) {
        try {
            PagamentoStatusResponse response = pagamentoService.verificarEstado(token, utilizador.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
