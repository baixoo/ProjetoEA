package pt.notub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.notub.models.*;
import pt.notub.repositories.UtilizadorRepository;

@Configuration
public class DataInitializer {
/// Apenas encontra-se aqui criar admin pq preciso de encriptar password
/// com SQL normal acho que nao conseguiria
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

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
                admin.setAuthMethod(AuthMethod.CREDENTIALS);
                admin.setTipoUtilizador(TipoUtilizador.ADULTO);
                try {
                    utilizadorRepository.save(admin);
                    logger.info("Admin criado: {}", adminEmail);
                } catch (DataIntegrityViolationException e) {
                    logger.info("Admin já existe (criado por outra réplica): {}", adminEmail);
                }
            }

            String motoristaEmail = "motorista@notub.pt";
            if (utilizadorRepository.findByEmail(motoristaEmail).isEmpty()) {
                Utilizador motorista = new Utilizador();
                motorista.setPrimeiroNome("Motorista");
                motorista.setUltimoNome("NoTUB");
                motorista.setEmail(motoristaEmail);
                motorista.setPassword(passwordEncoder.encode("motorista123"));
                motorista.setNif("000000001");
                motorista.setRole(TipoPapel.MOTORISTA);
                motorista.setAuthMethod(AuthMethod.CREDENTIALS);
                motorista.setTipoUtilizador(TipoUtilizador.ADULTO);
                try {
                    utilizadorRepository.save(motorista);
                    logger.info("Motorista criado: {}", motoristaEmail);
                } catch (DataIntegrityViolationException e) {
                    logger.info("Motorista já existe (criado por outra réplica): {}", motoristaEmail);
                }
            }
        };
    }
}
