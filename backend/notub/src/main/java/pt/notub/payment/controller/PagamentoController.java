package pt.notub.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.payment.dto.CheckoutRequest;
import pt.notub.payment.dto.CheckoutResponse;
import pt.notub.payment.dto.PagamentoStatusResponse;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;
import pt.notub.payment.service.PagamentoService;

@RestController
@RequestMapping({"/api/pagamento", "/api/payment"})
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticatedUser AuthenticatedUserContext utilizador,
                                                     @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagamentoService.iniciarCheckout(utilizador.email(), request));
    }

    @GetMapping({"/estado", "/status"})
    public ResponseEntity<PagamentoStatusResponse> verificarEstado(
            @AuthenticatedUser AuthenticatedUserContext utilizador,
            @RequestParam("t") String token) {
        return ResponseEntity.ok(pagamentoService.verificarEstado(token, utilizador.id()));
    }
}
