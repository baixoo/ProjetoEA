package pt.notub.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class StatelessOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger logger = LoggerFactory.getLogger(StatelessOAuth2AuthorizationRequestRepository.class);
    private static final String STATE_PARAM = "state";
    private static final long STATE_TTL_SECONDS = 300;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${projetoea.app.jwtSecret}")
    private String jwtSecret;

    private final ClientRegistrationRepository clientRegistrationRepository;

    public StatelessOAuth2AuthorizationRequestRepository(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String stateParam = request.getParameter(STATE_PARAM);
        if (stateParam == null || stateParam.isBlank()) {
            return null;
        }
        try {
            JsonNode claims = verifyAndDecode(stateParam);
            if (claims == null) {
                return null;
            }
            String registrationId = claims.get("rid").asText();
            String codeVerifier = claims.get("cv").asText();

            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);
            if (clientRegistration == null) {
                logger.error("Unknown registration_id: {}", registrationId);
                return null;
            }
            String redirectUri = claims.has("ruri") ? claims.get("ruri").asText() : clientRegistration.getRedirectUri();

            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                    .clientId(clientRegistration.getClientId())
                    .redirectUri(redirectUri)
                    .scopes(clientRegistration.getScopes())
                    .state(stateParam)
                    .attributes(attrs -> {
                        attrs.put("registration_id", registrationId);
                        attrs.put("code_verifier", codeVerifier);
                    })
                    .build();
        } catch (Exception e) {
            logger.error("Failed to load authorization request from state", e);
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request, HttpServletResponse response) {
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                   HttpServletResponse response) {
        return loadAuthorizationRequest(request);
    }

    public String encodeState(String registrationId, String codeVerifier, String redirectUri) {
        try {
            ObjectNode claims = mapper.createObjectNode();
            claims.put("rid", registrationId);
            claims.put("cv", codeVerifier);
            claims.put("ruri", redirectUri);
            claims.put("iat", System.currentTimeMillis() / 1000);
            claims.put("exp", (System.currentTimeMillis() / 1000) + STATE_TTL_SECONDS);
            return signJwt(claims);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode state", e);
        }
    }

    private String signJwt(ObjectNode claims) throws Exception {
        byte[] secretBytes = Base64.getDecoder().decode(jwtSecret);
        String headerB64 = b64urlEncode("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payloadB64 = b64urlEncode(mapper.writeValueAsBytes(claims));
        String signingInput = headerB64 + "." + payloadB64;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
        String signature = b64urlEncode(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + signature;
    }

    private JsonNode verifyAndDecode(String jwt) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(jwtSecret);
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) return null;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            String expected = b64urlEncode(mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8)));
            if (!expected.equals(parts[2])) {
                logger.warn("State JWT signature invalid");
                return null;
            }
            JsonNode claims = mapper.readTree(b64urlDecode(parts[1]));
            if (claims.has("exp") && claims.get("exp").asLong() < System.currentTimeMillis() / 1000) {
                logger.warn("State JWT expired");
                return null;
            }
            return claims;
        } catch (Exception e) {
            logger.error("State JWT decode failed", e);
            return null;
        }
    }

    private String b64urlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] b64urlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}
