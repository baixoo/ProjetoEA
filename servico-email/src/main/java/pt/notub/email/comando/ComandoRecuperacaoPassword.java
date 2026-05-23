package pt.notub.email.comando;

import org.springframework.stereotype.Component;
import pt.notub.email.service.ServicoEnvioEmail;

import java.util.Map;

@Component
public class ComandoRecuperacaoPassword implements ComandoEvento {

    private static final String TIPO = "RECUPERACAO_PASSWORD_PEDIDA";

    private final ServicoEnvioEmail servicoEnvioEmail;

    public ComandoRecuperacaoPassword(ServicoEnvioEmail servicoEnvioEmail) {
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
        String urlRecuperacao = (String) dados.get("urlRecuperacao");
        servicoEnvioEmail.enviarRecuperacaoPassword(email, primeiroNome, urlRecuperacao);
    }
}
