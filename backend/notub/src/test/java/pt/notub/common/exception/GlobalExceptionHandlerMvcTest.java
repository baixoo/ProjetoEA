package pt.notub.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorFixtureController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void badRequest_usesGlobalErrorShape() throws Exception {
        mockMvc.perform(get("/errors/bad-request").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensagem").value("Pedido invalido"));
    }

    @Test
    void notFound_usesGlobalErrorShape() throws Exception {
        mockMvc.perform(get("/errors/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void conflict_usesGlobalErrorShape() throws Exception {
        mockMvc.perform(get("/errors/conflict").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void unauthorized_usesGlobalErrorShape() throws Exception {
        mockMvc.perform(get("/errors/unauthorized").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Authentication Required"));
    }

    @Test
    void forbidden_usesGlobalErrorShape() throws Exception {
        mockMvc.perform(get("/errors/forbidden").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Access Denied"));
    }

    @RestController
    static class ErrorFixtureController {
        @GetMapping("/errors/bad-request")
        void badRequest() {
            throw new PedidoInvalidoException("Pedido invalido");
        }

        @GetMapping("/errors/not-found")
        void notFound() {
            throw new RecursoNaoEncontradoException("Nao encontrado");
        }

        @GetMapping("/errors/conflict")
        void conflict() {
            throw new ConflitoException("Conflito");
        }

        @GetMapping("/errors/unauthorized")
        void unauthorized() {
            throw new AutenticacaoRequeridaException();
        }

        @GetMapping("/errors/forbidden")
        void forbidden() {
            throw new AcessoNegadoException();
        }
    }
}
