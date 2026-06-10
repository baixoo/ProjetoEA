package pt.notub.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pt.notub.auth.dto.AuthResponse;
import pt.notub.auth.dto.LoginRequest;
import pt.notub.auth.dto.PedidoEsqueceuPassword;
import pt.notub.auth.dto.PedidoRedefinirPassword;
import pt.notub.auth.dto.RegisterRequest;
import pt.notub.auth.entity.TokenRecuperacaoSenha;
import pt.notub.auth.repository.TokenRecuperacaoSenhaRepository;
import pt.notub.common.notification.PublicadorEventosEmail;
import pt.notub.common.security.JwtUtils;
import pt.notub.common.security.UserDetailsImpl;
import pt.notub.common.util.NifValidator;
import pt.notub.user.dto.UserDTO;
import pt.notub.user.entity.AuthMethod;
import pt.notub.user.entity.TipoPapel;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.mapper.UserMapper;
import pt.notub.user.repository.UtilizadorRepository;
import pt.notub.user.service.UtilizadorService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UtilizadorRepository utilizadorRepository;
    private final TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final PublicadorEventosEmail publicadorEventosEmail;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    public AuthService(AuthenticationManager authenticationManager,
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

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return buildAuthResponse(authentication);
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        validateRegisterRequest(registerRequest);

        Utilizador utilizador = new Utilizador();
        utilizador.setEmail(registerRequest.getEmail());
        utilizador.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        utilizador.setPrimeiroNome(registerRequest.getPrimeiroNome());
        utilizador.setUltimoNome(registerRequest.getUltimoNome());
        String nif = registerRequest.getNif();
        utilizador.setNif(nif != null && nif.isEmpty() ? null : nif);
        if (registerRequest.getDataNascimento() != null && !registerRequest.getDataNascimento().isEmpty()) {
            LocalDate dataNascimento = LocalDate.parse(registerRequest.getDataNascimento());
            utilizador.setDataNascimento(dataNascimento);
            utilizador.setTipoUtilizador(UtilizadorService.calcularTipoUtilizador(dataNascimento));
        }
        utilizador.setRole(TipoPapel.UTILIZADOR);
        utilizador.setAuthMethod(AuthMethod.CREDENTIALS);

        Utilizador saved = utilizadorRepository.save(utilizador);
        publicadorEventosEmail.publicarUtilizadorCriado(
                saved.getId(), saved.getEmail(), saved.getPrimeiroNome(), saved.getUltimoNome());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registerRequest.getEmail(), registerRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return buildAuthResponse(authentication);
    }

    public UserDTO getCurrentUser(Utilizador utilizador) {
        return UserMapper.toDTO(utilizador);
    }

    public void sendPasswordRecovery(PedidoEsqueceuPassword pedido) {
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
    }

    public void resetPassword(PedidoRedefinirPassword pedido) {
        TokenRecuperacaoSenha token = tokenRecuperacaoSenhaRepository
                .findValidToken(pedido.getToken())
                .orElseThrow(() -> new RuntimeException("Token invalido ou expirado"));
        if (token.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token de recuperacao expirado");
        }
        Utilizador utilizador = token.getUtilizador();
        utilizador.setPassword(passwordEncoder.encode(pedido.getNovaPassword()));
        utilizadorRepository.save(utilizador);
        token.setUtilizado(true);
        tokenRecuperacaoSenhaRepository.save(token);
    }

    private AuthResponse buildAuthResponse(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateJwtToken(authentication);
        return new AuthResponse(jwt, userDetails.getId(), userDetails.getUsername());
    }

    private void validateRegisterRequest(RegisterRequest registerRequest) {
        if (registerRequest.getEmail() == null || registerRequest.getEmail().isBlank()) {
            throw new IllegalArgumentException("Erro: Email e obrigatorio!");
        }
        if (utilizadorRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Erro: Email ja em uso!");
        }
        if (registerRequest.getPrimeiroNome() == null || !registerRequest.getPrimeiroNome().matches("^[\\p{L}]+$")) {
            throw new IllegalArgumentException("Erro: Primeiro nome invalido! Deve conter apenas letras.");
        }
        if (registerRequest.getUltimoNome() == null || !registerRequest.getUltimoNome().matches("^[\\p{L}\\s]+$")) {
            throw new IllegalArgumentException("Erro: Ultimo nome invalido! Deve conter apenas letras e espacos.");
        }
        String nif = registerRequest.getNif();
        if (nif != null && !nif.isEmpty()) {
            if (!NifValidator.isValid(nif)) {
                throw new IllegalArgumentException("Erro: NIF invalido!");
            }
            if (utilizadorRepository.existsByNif(nif)) {
                throw new IllegalArgumentException("Erro: NIF ja em uso!");
            }
        }
        if (registerRequest.getDataNascimento() != null && !registerRequest.getDataNascimento().isEmpty()) {
            LocalDate dataNascimento = LocalDate.parse(registerRequest.getDataNascimento());
            if (dataNascimento.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Erro: A data de nascimento nao pode ser futura!");
            }
            if (dataNascimento.isBefore(LocalDate.of(1900, 1, 1))) {
                throw new IllegalArgumentException("Erro: A data de nascimento deve ser posterior a 1900-01-01!");
            }
        }
    }
}
