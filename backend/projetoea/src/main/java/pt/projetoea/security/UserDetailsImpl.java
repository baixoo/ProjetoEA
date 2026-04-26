package pt.projetoea.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pt.projetoea.models.Utilizador;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    private Long id;
    private String email;
    private String password;

    public UserDetailsImpl(Long id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }

    public static UserDetailsImpl build(Utilizador utilizador) {
        return new UserDetailsImpl(
                utilizador.getId(),
                utilizador.getEmail(),
                utilizador.getPassword()
        );
    }

    public Long getId() { return id; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
