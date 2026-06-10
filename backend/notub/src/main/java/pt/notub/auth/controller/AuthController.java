package pt.notub.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.auth.dto.AuthResponse;
import pt.notub.auth.dto.LoginRequest;
import pt.notub.auth.dto.PedidoEsqueceuPassword;
import pt.notub.auth.dto.PedidoRedefinirPassword;
import pt.notub.auth.dto.RegisterRequest;
import pt.notub.auth.service.AuthService;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;
import pt.notub.common.dto.MensagemResponse;
import pt.notub.user.dto.UserDTO;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticatedUser AuthenticatedUserContext utilizador) {
        return ResponseEntity.ok(authService.getCurrentUser(utilizador.email()));
    }

    @PostMapping({"/esqueceu-password", "/forgot-password"})
    public ResponseEntity<MensagemResponse> esqueceuPassword(@RequestBody PedidoEsqueceuPassword pedido) {
        return ResponseEntity.ok(authService.sendPasswordRecovery(pedido));
    }

    @PostMapping({"/redefinir-password", "/reset-password"})
    public ResponseEntity<MensagemResponse> redefinirPassword(@RequestBody PedidoRedefinirPassword pedido) {
        return ResponseEntity.ok(authService.resetPassword(pedido));
    }
}
