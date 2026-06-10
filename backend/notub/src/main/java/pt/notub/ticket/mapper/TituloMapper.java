package pt.notub.ticket.mapper;

import pt.notub.ticket.dto.TituloTransporteDTO;
import pt.notub.ticket.entity.Bilhete;
import pt.notub.ticket.entity.Passe;
import pt.notub.ticket.entity.TituloTransporte;

public final class TituloMapper {

    private TituloMapper() {}

    public static TituloTransporteDTO toDTO(TituloTransporte titulo) {
        if (titulo == null) return null;
        if (titulo instanceof Bilhete bilhete) {
            return BilheteMapper.toDTO(bilhete);
        }
        if (titulo instanceof Passe passe) {
            return PasseMapper.toDTO(passe);
        }
        throw new IllegalArgumentException("Tipo de titulo desconhecido: " + titulo.getClass());
    }
}
