package pt.notub.auth.dto;

public record AuthResponse(String token, Long id, String email) {
}
