package pt.notub.common.security;

import pt.notub.user.entity.TipoPapel;

public record AuthenticatedUserContext(Long id, String email, TipoPapel role) {
}
