package pt.notub.validation;

import org.springframework.stereotype.Component;
import pt.notub.models.Bilhete;
import pt.notub.models.Passe;
import pt.notub.models.TituloTransporte;

import java.time.LocalDateTime;

/**
 * QR Code validation strategy.
 * Validates transport titles by checking if they are valid and not expired.
 * Implements the Strategy pattern defined in the PIM.
 */
@Component
public class ValidacaoQRCode implements EstrategiaValidacao {

    @Override
    public boolean validar(TituloTransporte titulo) {
        if (titulo == null) {
            return false;
        }

        if (titulo instanceof Bilhete) {
            Bilhete bilhete = (Bilhete) titulo;
            return !bilhete.isUsado();
        }

        if (titulo instanceof Passe) {
            Passe passe = (Passe) titulo;
            LocalDateTime now = LocalDateTime.now();
            return passe.getInicio() != null
                    && passe.getFim() != null
                    && now.isAfter(passe.getInicio())
                    && now.isBefore(passe.getFim());
        }

        return false;
    }
}
