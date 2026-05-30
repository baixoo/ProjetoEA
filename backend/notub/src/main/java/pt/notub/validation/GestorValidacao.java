package pt.notub.validation;

import org.springframework.stereotype.Service;
import pt.notub.exception.RecursoNaoEncontradoException;
import pt.notub.models.Bilhete;
import pt.notub.models.TituloTransporte;
import pt.notub.repositories.BilheteRepository;
import pt.notub.repositories.TituloTransporteRepository;

/**
 * Validation manager that uses the Strategy pattern.
 * Delegates validation to the configured EstrategiaValidacao implementation.
 * Defined in the PIM as GestorValidacao.
 */
@Service
public class GestorValidacao {

    private final EstrategiaValidacao estrategia;
    private final TituloTransporteRepository tituloTransporteRepository;
    private final BilheteRepository bilheteRepository;

    public GestorValidacao(EstrategiaValidacao estrategia,
                           TituloTransporteRepository tituloTransporteRepository,
                           BilheteRepository bilheteRepository) {
        this.estrategia = estrategia;
        this.tituloTransporteRepository = tituloTransporteRepository;
        this.bilheteRepository = bilheteRepository;
    }

    /**
     * Validates a transport title by its ID.
     * @param tituloId the ID of the transport title
     * @return true if valid
     */
    public boolean validarTitulo(Long tituloId) {
        TituloTransporte titulo = tituloTransporteRepository.findById(tituloId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Titulo nao encontrado"));
        return estrategia.validar(titulo);
    }

    /**
     * Validates and marks a bilhete as used if valid.
     * @param tituloId the ID of the bilhete
     * @return true if it was valid and has been marked as used
     */
    public boolean validarEUsarBilhete(Long tituloId) {
        TituloTransporte titulo = tituloTransporteRepository.findById(tituloId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Titulo nao encontrado"));

        if (!estrategia.validar(titulo)) {
            return false;
        }

        if (titulo instanceof Bilhete) {
            Bilhete bilhete = (Bilhete) titulo;
            bilhete.setUsado(true);
            bilheteRepository.save(bilhete);
        }

        return true;
    }
}
