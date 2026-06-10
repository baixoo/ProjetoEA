package pt.notub.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pt.notub.user.entity.AuthMethod;
import pt.notub.user.entity.TipoUtilizador;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;
import pt.notub.common.notification.PublicadorEventosEmail;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtUtils jwtUtils;
    private final UtilizadorRepository utilizadorRepository;
    private final PublicadorEventosEmail publicadorEventosEmail;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(JwtUtils jwtUtils, UtilizadorRepository utilizadorRepository,
                                              PublicadorEventosEmail publicadorEventosEmail,
                                              @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL}") String frontendUrl) {
        this.jwtUtils = jwtUtils;
        this.utilizadorRepository = utilizadorRepository;
        this.publicadorEventosEmail = publicadorEventosEmail;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        logger.info("OAuth2 authentication success - processing login");
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = oauthToken.getPrincipal();

            String email = oAuth2User.getAttribute("email");
            String firstName = oAuth2User.getAttribute("given_name");
            String lastName = oAuth2User.getAttribute("family_name");
            logger.info("Google user attributes - email: {}, firstName: {}, lastName: {}", email, firstName, lastName);

            boolean isNewUser = utilizadorRepository.findByEmail(email).isEmpty();
            logger.info("User {} is {}", email, isNewUser ? "NEW - will create account" : "EXISTING");

            Utilizador utilizador;
            if (isNewUser) {
                utilizador = new Utilizador();
                utilizador.setEmail(email);
                utilizador.setPrimeiroNome(firstName);
                utilizador.setUltimoNome(lastName);
                utilizador.setAuthMethod(AuthMethod.GOOGLE);
                utilizador.setTipoUtilizador(TipoUtilizador.ADULTO);
                utilizador = utilizadorRepository.save(utilizador);
                logger.info("Created new user with id: {}", utilizador.getId());
                publicadorEventosEmail.publicarUtilizadorCriado(
                        utilizador.getId(), utilizador.getEmail(),
                        utilizador.getPrimeiroNome(), utilizador.getUltimoNome());
            } else {
                utilizador = utilizadorRepository.findByEmail(email).get();
                logger.info("Found existing user with id: {}", utilizador.getId());
            }

            String token = jwtUtils.generateTokenFromUsername(email);
            logger.info("JWT token generated for {} (length: {})", email, token.length());

            String redirectUrl = frontendUrl + "/oauth2/redirect?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            logger.info("Redirecting to frontend: {}/oauth2/redirect?token=...", frontendUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } catch (Exception e) {
            logger.error("OAuth2 success handler failed unexpectedly", e);
            String redirectUrl = frontendUrl + "/signin?oauth_error=true";
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }
}
