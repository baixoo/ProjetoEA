package pt.notub.email.comando;

import java.util.Map;

public interface ComandoEvento {
    String routingKey();
    void executar(Map<String, Object> dados);
}
