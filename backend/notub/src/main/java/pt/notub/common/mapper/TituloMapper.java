package pt.notub.common.mapper;

import pt.notub.ticket.dto.TituloTransporteDTO;
import pt.notub.models.TituloTransporte;
import pt.notub.models.Bilhete;
import pt.notub.models.Passe;

public final class TituloMapper { 

    private TituloMapper() {}

    public static TituloTransporteDTO toDTO(TituloTransporte titulo) {
        if (titulo instanceof Bilhete) {
            return BilheteMapper.toDTO((Bilhete) titulo);
        } else if (titulo instanceof Passe) {
            return PasseMapper.toDTO((Passe) titulo);
        }
        throw new IllegalArgumentException("Tipo de título desconhecido: " + titulo.getClass());
    }
}