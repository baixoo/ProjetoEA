package pt.notub.network.mapper;

import pt.notub.network.dto.ServicoDTO;
import pt.notub.network.entity.Servico;

public final class ServicoMapper {
    private ServicoMapper() {}

    public static ServicoDTO toDTO(Servico servico) {
        if (servico == null) return null;
        ServicoDTO dto = new ServicoDTO();
        dto.setId(servico.getId());
        dto.setNome(servico.getNome());
        return dto;
    }
}
