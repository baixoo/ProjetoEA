package pt.notub.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.UserMapper;
import pt.notub.auth.dto.LoginRequest;
import pt.notub.auth.dto.PedidoEsqueceuPassword;
import pt.notub.auth.dto.PedidoRedefinirPassword;
import pt.notub.auth.dto.RegisterRequest;
import pt.notub.models.AuthMethod;
import pt.notub.models.TipoPapel;
import pt.notub.models.TokenRecuperacaoSenha;
import pt.notub.models.Utilizador;
import pt.notub.repositories.TokenRecuperacaoSenhaRepository;
import pt.notub.repositories.UtilizadorRepository;
import pt.notub.security.JwtUtils;
import pt.notub.security.UserDetailsImpl;
import pt.notub.notification.PublicadorEventosEmail;
import pt.notub.user.dto.UserDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UtilizadorRepository utilizadorRepository;
    private final TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final PublicadorEventosEmail publicadorEventosEmail;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    public AuthController(AuthenticationManager authenticationManager,
                          UtilizadorRepository utilizadorRepository,
                          TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils,
                          PublicadorEventosEmail publicadorEventosEmail) {
        this.authenticationManager = authenticationManager;
        this.utilizadorRepository = utilizadorRepository;
        this.tokenRecuperacaoSenhaRepository = tokenRecuperacaoSenhaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.publicadorEventosEmail = publicadorEventosEmail;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", userDetails.getId());
        response.put("email", userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        if (utilizadorRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Erro: Email ja em uso!");
        }
        if (registerRequest.getNif() != null && !registerRequest.getNif().isEmpty()
                && utilizadorRepository.existsByNif(registerRequest.getNif())) {
            return ResponseEntity.badRequest().body("Erro: NIF ja em uso!");
        }
        Utilizador utilizador = new Utilizador();
        utilizador.setEmail(registerRequest.getEmail());
        utilizador.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        utilizador.setPrimeiroNome(registerRequest.getPrimeiroNome());
        utilizador.setUltimoNome(registerRequest.getUltimoNome());
        utilizador.setNif(registerRequest.getNif());
        if (registerRequest.getDataNascimento() != null && !registerRequest.getDataNascimento().isEmpty()) {
            utilizador.setDataNascimento(LocalDate.parse(registerRequest.getDataNascimento()));
        }
        utilizador.setRole(TipoPapel.UTILIZADOR);
        utilizador.setAuthMethod(AuthMethod.CREDENTIALS);
        utilizadorRepository.save(utilizador);
        publicadorEventosEmail.publicarUtilizadorCriado(
                utilizador.getId(), utilizador.getEmail(),
                utilizador.getPrimeiroNome(), utilizador.getUltimoNome());
        return ResponseEntity.ok("Utilizador registado com sucesso!");
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return ResponseEntity.status(401).build();
        }
        Utilizador utilizador = utilizadorRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
        return ResponseEntity.ok(UserMapper.toDTO(utilizador));
    }

    @PostMapping("/esqueceu-password")
    public ResponseEntity<?> esqueceuPassword(@RequestBody PedidoEsqueceuPassword pedido) {
        utilizadorRepository.findByEmail(pedido.getEmail()).ifPresent(utilizador -> {
            TokenRecuperacaoSenha token = new TokenRecuperacaoSenha();
            token.setUtilizador(utilizador);
            token.setToken(UUID.randomUUID().toString());
            token.setDataExpiracao(LocalDateTime.now().plusMinutes(30));
            token.setUtilizado(false);
            tokenRecuperacaoSenhaRepository.save(token);
            String urlRecuperacao = frontendUrl + "/reset-password?token=" + token.getToken();
            publicadorEventosEmail.publicarRecuperacaoPassword(
                    utilizador.getId(), utilizador.getEmail(), utilizador.getPrimeiroNome(),
                    token.getToken(), urlRecuperacao);
        });
        return ResponseEntity.ok("Se o email existir, foi enviado um link de recuperacao.");
    }

    @PostMapping("/redefinir-password")
    public ResponseEntity<?> redefinirPassword(@RequestBody PedidoRedefinirPassword pedido) {
        TokenRecuperacaoSenha token = tokenRecuperacaoSenhaRepository
                .findValidToken(pedido.getToken())
                .orElseThrow(() -> new RuntimeException("Token invalido ou expirado"));
        if (token.getDataExpiracao().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token de recuperacao expirado");
        }
        Utilizador utilizador = token.getUtilizador();
        utilizador.setPassword(passwordEncoder.encode(pedido.getNovaPassword()));
        utilizadorRepository.save(utilizador);
        token.setUtilizado(true);
        tokenRecuperacaoSenhaRepository.save(token);
        return ResponseEntity.ok("Palavra-passe redefinida com sucesso");
    }
}
