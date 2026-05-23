package pt.notub.email.comando;

import org.springframework.stereotype.Component;
import pt.notub.email.service.ServicoEnvioEmail;

import java.util.Map;

@Component
public class ComandoUtilizadorCriado implements ComandoEvento {

    private static final String TIPO = "UTILIZADOR_CRIADO";

    private final ServicoEnvioEmail servicoEnvioEmail;

    public ComandoUtilizadorCriado(ServicoEnvioEmail servicoEnvioEmail) {
        this.servicoEnvioEmail = servicoEnvioEmail;
    }

    @Override
    public String tipoEvento() {
        return TIPO;
    }

    @Override
    public void executar(Map<String, Object> dados) {
        String email = (String) dados.get("email");
        String primeiroNome = (String) dados.get("primeiroNome");
        String ultimoNome = (String) dados.get("ultimoNome");
        servicoEnvioEmail.enviarBoasVindas(email, primeiroNome, ultimoNome);
    }
}
