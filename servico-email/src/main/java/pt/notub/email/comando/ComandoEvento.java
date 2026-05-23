package pt.notub.email.comando;

import java.util.Map;

public interface ComandoEvento {
    String tipoEvento();
    void executar(Map<String, Object> dados);
}
