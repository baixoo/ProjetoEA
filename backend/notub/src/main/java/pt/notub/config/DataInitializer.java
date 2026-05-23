package pt.notub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.notub.models.TipoPapel;
import pt.notub.models.Utilizador;
import pt.notub.repositories.UtilizadorRepository;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Order(1)
    CommandLineRunner initAdmin(UtilizadorRepository utilizadorRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@notub.pt";
            if (utilizadorRepository.findByEmail(adminEmail).isEmpty()) {
                Utilizador admin = new Utilizador();
                admin.setPrimeiroNome("Admin");
                admin.setUltimoNome("NoTUB");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setNif("000000000");
                admin.setRole(TipoPapel.ADMINISTRADOR);
                utilizadorRepository.save(admin);
                logger.info("Admin criado: {}", adminEmail);
            }
        };
    }
}
