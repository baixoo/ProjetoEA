package pt.notub.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthSecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authenticationEntryPoint_writesApiErrorResponseJson() throws Exception {
        AuthEntryPointJwt handler = new AuthEntryPointJwt(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, new AuthenticationException("missing token") {});

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, body.get("status").asInt());
        assertEquals("Unauthorized", body.get("error").asText());
        assertEquals("Autenticacao requerida", body.get("mensagem").asText());
    }

    @Test
    void accessDeniedHandler_writesApiErrorResponseJson() throws Exception {
        AuthAccessDeniedHandler handler = new AuthAccessDeniedHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("forbidden"));

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, body.get("status").asInt());
        assertEquals("Forbidden", body.get("error").asText());
        assertEquals("forbidden", body.get("mensagem").asText());
    }
}
