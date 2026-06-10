package pt.notub.validation.strategy;

import pt.notub.ticket.entity.TituloTransporte;

/**
 * Strategy interface for transport title validation.
 * Part of the Strategy pattern defined in the PIM.
 */
public interface EstrategiaValidacao {

    /**
     * Validates a transport title.
     * @param titulo the transport title to validate
     * @return true if the title is valid, false otherwise
     */
    boolean validar(TituloTransporte titulo);
}
