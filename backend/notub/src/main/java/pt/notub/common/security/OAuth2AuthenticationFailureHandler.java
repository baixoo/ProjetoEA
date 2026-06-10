package pt.notub.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        logger.error("OAuth2 authentication failed - exception: {}", exception.getMessage(), exception);
        logger.error("OAuth2 failure details - request URI: {}, remote addr: {}", request.getRequestURI(), request.getRemoteAddr());
        if (exception.getCause() != null) {
            logger.error("OAuth2 root cause: {}", exception.getCause().getMessage(), exception.getCause());
        }
        String redirectUrl = frontendUrl + "/signin?oauth_error=true";
        logger.info("Redirecting to signin with error: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
