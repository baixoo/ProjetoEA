package pt.notub.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.core.MethodParameter;
import pt.notub.common.exception.GlobalExceptionHandler;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;
import pt.notub.payment.dto.CheckoutRequest;
import pt.notub.payment.dto.CheckoutResponse;
import pt.notub.payment.dto.PagamentoStatusResponse;
import pt.notub.payment.service.PagamentoService;
import pt.notub.user.entity.TipoPapel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PagamentoControllerMvcTest {

    @Mock
    private PagamentoService pagamentoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PagamentoController(pagamentoService))
                .setCustomArgumentResolvers(new FixedAuthenticatedUserResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void checkout_returnsCreated() throws Exception {
        when(pagamentoService.iniciarCheckout(eq("user@example.com"), any(CheckoutRequest.class)))
                .thenReturn(new CheckoutResponse(1L, "token-123", "http://redirect", "EM_CURSO"));

        mockMvc.perform(post("/api/pagamento/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoProduto":"BILHETE","quantidade":1,"zonaId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-123"))
                .andExpect(jsonPath("$.estado").value("EM_CURSO"));
    }

    @Test
    void verificarEstado_returnsOk() throws Exception {
        PagamentoStatusResponse response = new PagamentoStatusResponse();
        response.setTransacaoId(5L);
        response.setEstado("CONCLUIDO");
        response.setPagamentoExternoStatus("Success");
        response.setTituloCriado(true);

        when(pagamentoService.verificarEstado("tok-1", 42L)).thenReturn(response);

        mockMvc.perform(get("/api/pagamento/estado")
                        .param("t", "tok-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transacaoId").value(5L))
                .andExpect(jsonPath("$.estado").value("CONCLUIDO"))
                .andExpect(jsonPath("$.tituloCriado").value(true));
    }

    private static final class FixedAuthenticatedUserResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(@NonNull MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticatedUser.class)
                    && parameter.getParameterType().equals(AuthenticatedUserContext.class);
        }

        @Override
        public Object resolveArgument(@NonNull MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return new AuthenticatedUserContext(42L, "user@example.com", TipoPapel.UTILIZADOR);
        }
    }
}
