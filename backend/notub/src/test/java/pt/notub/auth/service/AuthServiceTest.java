package pt.notub.auth.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
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
import pt.notub.user.entity.AuthMethod;
import pt.notub.user.entity.TipoPapel;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UtilizadorRepository utilizadorRepository;

    @Mock
    private TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PublicadorEventosEmail publicadorEventosEmail;

    @Mock
    private Authentication authentication;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                authenticationManager,
                utilizadorRepository,
                tokenRecuperacaoSenhaRepository,
                passwordEncoder,
                jwtUtils,
                publicadorEventosEmail);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://frontend");
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void login_returnsJwtResponse() {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(7L);
        utilizador.setEmail("user@example.com");
        utilizador.setPassword("encoded");
        utilizador.setRole(TipoPapel.UTILIZADOR);

        UserDetailsImpl principal = UserDetailsImpl.build(utilizador);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");

        AuthResponse response = service.login(new LoginRequest() {{
            setEmail("user@example.com");
            setPassword("secret");
        }});

        assertEquals("jwt-token", response.token());
        assertEquals(7L, response.id());
        assertEquals("user@example.com", response.email());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void register_validRequest_persistsAndReturnsJwtResponse() {
        when(utilizadorRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(utilizadorRepository.existsByNif("123456789")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(authentication.getPrincipal()).thenReturn(buildPrincipal(11L, "new@example.com"));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");
        when(utilizadorRepository.save(any())).thenAnswer(invocation -> {
            Utilizador saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("secret");
        request.setPrimeiroNome("Maria");
        request.setUltimoNome("Silva");
        request.setNif("123456789");
        request.setDataNascimento("2000-01-01");

        AuthResponse response = service.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals(11L, response.id());
        assertEquals("new@example.com", response.email());

        ArgumentCaptor<Utilizador> captor = ArgumentCaptor.forClass(Utilizador.class);
        verify(utilizadorRepository).save(captor.capture());
        assertEquals("encoded-secret", captor.getValue().getPassword());
        assertEquals(TipoPapel.UTILIZADOR, captor.getValue().getRole());
        assertEquals(AuthMethod.CREDENTIALS, captor.getValue().getAuthMethod());
        verify(publicadorEventosEmail).publicarUtilizadorCriado(11L, "new@example.com", "Maria", "Silva");
    }

    @Test
    void register_invalidFirstName_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("secret");
        request.setPrimeiroNome("Maria123");
        request.setUltimoNome("Silva");

        assertThrows(IllegalArgumentException.class, () -> service.register(request));
    }

    @Test
    void sendPasswordRecovery_existingUserCreatesToken() {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(22L);
        utilizador.setEmail("forgot@example.com");
        utilizador.setPrimeiroNome("Ana");

        when(utilizadorRepository.findByEmail("forgot@example.com")).thenReturn(Optional.of(utilizador));
        when(tokenRecuperacaoSenhaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendPasswordRecovery(new PedidoEsqueceuPassword() {{
            setEmail("forgot@example.com");
        }});

        ArgumentCaptor<TokenRecuperacaoSenha> captor = ArgumentCaptor.forClass(TokenRecuperacaoSenha.class);
        verify(tokenRecuperacaoSenhaRepository).save(captor.capture());
        assertEquals(utilizador, captor.getValue().getUtilizador());
        assertNotNull(captor.getValue().getToken());
        assertFalse(captor.getValue().isUtilizado());
        assertTrue(captor.getValue().getDataExpiracao().isAfter(LocalDateTime.now()));
        verify(publicadorEventosEmail).publicarRecuperacaoPassword(
                eq(22L),
                eq("forgot@example.com"),
                eq("Ana"),
                eq(captor.getValue().getToken()),
                contains("/reset-password?token="));
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndMarksTokenUsed() {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(33L);
        utilizador.setEmail("reset@example.com");

        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha();
        token.setId(44L);
        token.setToken("token-123");
        token.setUtilizador(utilizador);
        token.setDataExpiracao(LocalDateTime.now().plusMinutes(10));
        token.setUtilizado(false);

        when(tokenRecuperacaoSenhaRepository.findValidToken("token-123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(utilizadorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRecuperacaoSenhaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.resetPassword(new PedidoRedefinirPassword() {{
            setToken("token-123");
            setNovaPassword("new-password");
        }});

        assertEquals("encoded-new-password", utilizador.getPassword());
        assertTrue(token.isUtilizado());
        verify(utilizadorRepository).save(utilizador);
        verify(tokenRecuperacaoSenhaRepository).save(token);
    }

    private UserDetailsImpl buildPrincipal(Long id, String email) {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(id);
        utilizador.setEmail(email);
        utilizador.setPassword("encoded");
        utilizador.setRole(TipoPapel.UTILIZADOR);
        return UserDetailsImpl.build(utilizador);
    }
}
