package pt.notub.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pt.notub.models.Utilizador;
import pt.notub.repositories.UtilizadorRepository;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UtilizadorRepository utilizadorRepository;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(JwtUtils jwtUtils, UtilizadorRepository utilizadorRepository) {
        this.jwtUtils = jwtUtils;
        this.utilizadorRepository = utilizadorRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        // Create or find user
        Utilizador utilizador = utilizadorRepository.findByEmail(email).orElseGet(() -> {
            Utilizador newUser = new Utilizador();
            newUser.setEmail(email);
            newUser.setPrimeiroNome(firstName);
            newUser.setUltimoNome(lastName);
            return utilizadorRepository.save(newUser);
        });

        // Generate JWT
        String token = jwtUtils.generateTokenFromUsername(email);

        // Redirect to frontend with token
        String redirectUrl = frontendUrl + "/oauth2/redirect?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
