package pt.notub.common.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilizadorRepository utilizadorRepository;

    public UserDetailsServiceImpl(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return UserDetailsImpl.build(utilizador);
    }
}
