package pt.notub.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class StatelessOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final Logger logger = LoggerFactory.getLogger(StatelessOAuth2AuthorizationRequestResolver.class);
    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final StatelessOAuth2AuthorizationRequestRepository stateRepository;

    public StatelessOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                                        StatelessOAuth2AuthorizationRequestRepository stateRepository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
                "/oauth2/authorization");
        this.stateRepository = stateRepository;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest resolved = delegate.resolve(request);
        return resolved != null ? encodeState(resolved) : null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest resolved = delegate.resolve(request, clientRegistrationId);
        return resolved != null ? encodeState(resolved) : null;
    }

    private OAuth2AuthorizationRequest encodeState(OAuth2AuthorizationRequest original) {
        String registrationId = (String) original.getAttributes().get("registration_id");
        String codeVerifier = (String) original.getAttributes().get("code_verifier");

        if (codeVerifier == null) {
            logger.warn("No code_verifier found — PKCE may not be active for registration: {}", registrationId);
            return original;
        }

        String jwtState = stateRepository.encodeState(registrationId, codeVerifier, original.getRedirectUri());

        String originalUri = original.getAuthorizationRequestUri();
        String originalStateEncoded = URLEncoder.encode(original.getState(), StandardCharsets.UTF_8);
        String jwtStateEncoded = URLEncoder.encode(jwtState, StandardCharsets.UTF_8);
        String newUri = originalUri.replace("state=" + originalStateEncoded, "state=" + jwtStateEncoded);

        return OAuth2AuthorizationRequest.from(original)
                .state(jwtState)
                .authorizationRequestUri(newUri)
                .build();
    }
}
