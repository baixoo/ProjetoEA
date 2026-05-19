package pt.notub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pt.notub.dto.request.LoginRequest;
import pt.notub.dto.request.RegisterRequest;
import pt.notub.models.Utilizador;
import pt.notub.repositories.UtilizadorRepository;
import pt.notub.security.JwtUtils;
import pt.notub.security.UserDetailsImpl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager,
                          UtilizadorRepository utilizadorRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.utilizadorRepository = utilizadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
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
            return ResponseEntity.badRequest().body("Error: Email already in use!");
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

        utilizadorRepository.save(utilizador);

        return ResponseEntity.ok("User registered successfully!");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Utilizador utilizador = utilizadorRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(utilizador);
    }
}
