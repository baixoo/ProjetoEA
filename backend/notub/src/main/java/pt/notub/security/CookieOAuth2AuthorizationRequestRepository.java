package pt.notub.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;

@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_MAX_AGE = 300;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = findCookie(request);
        if (cookie == null) return null;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
            return objectMapper.readValue(bytes, OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeCookie(response);
            return;
        }
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(authorizationRequest);
            String encoded = Base64.getUrlEncoder().encodeToString(bytes);
            String headerValue = String.format(
                    "%s=%s; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=%d",
                    COOKIE_NAME, encoded, COOKIE_MAX_AGE);
            response.addHeader("Set-Cookie", headerValue);
        } catch (Exception e) {
            removeCookie(response);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest req = loadAuthorizationRequest(request);
        removeCookie(response);
        return req;
    }

    private Cookie findCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    private void removeCookie(HttpServletResponse response) {
        String headerValue = String.format(
                "%s=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0",
                COOKIE_NAME);
        response.addHeader("Set-Cookie", headerValue);
    }
}
