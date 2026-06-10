package pt.notub.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.notub.common.exception.GlobalExceptionHandler;
import pt.notub.network.dto.LinhaDTO;
import pt.notub.network.dto.LinhaRequest;
import pt.notub.network.service.TransportNetworkService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminRedeControllerMvcTest {

    @Mock
    private TransportNetworkService transportNetworkService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRedeController(transportNetworkService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createLinha_returnsCreated() throws Exception {
        LinhaDTO dto = new LinhaDTO();
        dto.setNome("Linha 1");
        dto.setIdentificadorServico("UTEIS");
        when(transportNetworkService.createLinha(any(LinhaRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/admin/network/linhas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinhaRequest("Linha 1", "UTEIS"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Linha 1"))
                .andExpect(jsonPath("$.identificadorServico").value("UTEIS"));

        verify(transportNetworkService).createLinha(any(LinhaRequest.class));
    }

    @Test
    void updateLinha_returnsOk() throws Exception {
        LinhaDTO dto = new LinhaDTO();
        dto.setNome("Linha 2");
        dto.setIdentificadorServico("SAB");
        when(transportNetworkService.updateLinha(eq(7L), any(LinhaRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/admin/network/linhas/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinhaRequest("Linha 2", "SAB"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Linha 2"))
                .andExpect(jsonPath("$.identificadorServico").value("SAB"));
    }

    @Test
    void deleteLinha_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/network/linhas/9"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(transportNetworkService).deleteLinha(9L);
    }
}
