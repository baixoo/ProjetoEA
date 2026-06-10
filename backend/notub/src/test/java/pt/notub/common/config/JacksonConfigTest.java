package pt.notub.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pt.notub.common.security.AuthAccessDeniedHandler;
import pt.notub.common.security.AuthEntryPointJwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JacksonConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JacksonConfig.class, AuthEntryPointJwt.class, AuthAccessDeniedHandler.class);

    @Test
    void securityHandlersCanUseSharedObjectMapperBean() {
        contextRunner.run(context -> {
            assertEquals(1, context.getBeansOfType(ObjectMapper.class).size());
            assertNotNull(context.getBean(AuthEntryPointJwt.class));
            assertNotNull(context.getBean(AuthAccessDeniedHandler.class));
        });
    }
}
