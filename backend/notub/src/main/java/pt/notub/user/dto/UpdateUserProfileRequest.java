package pt.notub.user.dto;

public record UpdateUserProfileRequest(
        String primeiroNome,
        String ultimoNome,
        String nif,
        String dataNascimento
) {
}
