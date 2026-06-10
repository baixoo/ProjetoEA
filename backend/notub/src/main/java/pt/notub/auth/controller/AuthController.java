package pt.notub.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.auth.dto.AuthResponse;
import pt.notub.auth.dto.LoginRequest;
import pt.notub.auth.dto.PedidoEsqueceuPassword;
import pt.notub.auth.dto.PedidoRedefinirPassword;
import pt.notub.auth.dto.RegisterRequest;
import pt.notub.auth.service.AuthService;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.user.dto.UserDTO;
import pt.notub.user.entity.Utilizador;

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
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            return ResponseEntity.ok(authService.register(registerRequest));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(authService.getCurrentUser(utilizador));
    }

    @PostMapping({"/esqueceu-password", "/forgot-password"})
    public ResponseEntity<?> esqueceuPassword(@RequestBody PedidoEsqueceuPassword pedido) {
        authService.sendPasswordRecovery(pedido);
        return ResponseEntity.ok("Se o email existir, foi enviado um link de recuperacao.");
    }

    @PostMapping({"/redefinir-password", "/reset-password"})
    public ResponseEntity<?> redefinirPassword(@RequestBody PedidoRedefinirPassword pedido) {
        try {
            authService.resetPassword(pedido);
            return ResponseEntity.ok("Palavra-passe redefinida com sucesso");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
