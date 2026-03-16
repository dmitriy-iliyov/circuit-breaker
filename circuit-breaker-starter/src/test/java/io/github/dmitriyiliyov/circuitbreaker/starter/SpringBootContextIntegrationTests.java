package io.github.dmitriyiliyov.circuitbreaker.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CircuitBreakerAutoConfiguration.class)
public class SpringBootContextIntegrationTests {

    @Test
    @DisplayName("IT load context")
    public void loadContext() {}
}
