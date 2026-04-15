package pt.projetoea.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for development/stateless APIs so POST requests don't get blocked
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Open up everything under /api/ so you don't get redirected to /login
                .requestMatchers("/api/**").permitAll()
                // Require authentication for anything else
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
