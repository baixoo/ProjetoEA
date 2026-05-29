package pt.notub.common.mapper;

import pt.notub.models.Utilizador;
import pt.notub.user.dto.UserDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    public static UserDTO toDTO(Utilizador u) {
        if (u == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(u.getId());
        dto.setPrimeiroNome(u.getPrimeiroNome());
        dto.setUltimoNome(u.getUltimoNome());
        dto.setNif(u.getNif());
        dto.setEmail(u.getEmail());
        dto.setDataNascimento(u.getDataNascimento());
        dto.setTipoUtilizador(u.getTipoUtilizador());
        dto.setNrPontos(u.getNrPontos());
        dto.setRole(u.getRole());
        dto.setAuthMethod(u.getAuthMethod());
        return dto;
    }

    public static List<UserDTO> toDTOList(List<Utilizador> users) {
        if (users == null) return null;
        return users.stream().map(UserMapper::toDTO).collect(Collectors.toList());
    }
}
