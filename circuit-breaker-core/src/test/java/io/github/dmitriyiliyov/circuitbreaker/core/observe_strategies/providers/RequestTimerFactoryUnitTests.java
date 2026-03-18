package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.DefaultRequestTimer;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.NoopRequestTimer;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RequestTimerFactoryUnitTests {

    private final CircuitBreakerConfiguration config = mock(CircuitBreakerConfiguration.class);

    @BeforeEach
    void setUp() {
        reset(config);
    }

    @Test
    @DisplayName("UT: of(configuration) should return DefaultRequestTimer when timer is enabled")
    void of_shouldReturnDefaultRequestTimer_whenEnabled() {
        when(config.isRequestTimerEnable()).thenReturn(true);
        when(config.getMaxRequestExecutionDuration()).thenReturn(Duration.ofSeconds(1));

        RequestTimer timer = RequestTimerFactory.of(config);

        assertThat(timer).isInstanceOf(DefaultRequestTimer.class);
        verify(config).isRequestTimerEnable();
        verify(config).getMaxRequestExecutionDuration();
        verifyNoMoreInteractions(config);
    }

    @Test
    @DisplayName("UT: of(configuration) should return NoopRequestTimer when timer is disabled")
    void of_shouldReturnNoopRequestTimer_whenDisabled() {
        when(config.isRequestTimerEnable()).thenReturn(false);

        RequestTimer timer = RequestTimerFactory.of(config);

        assertThat(timer).isInstanceOf(NoopRequestTimer.class);
        verify(config).isRequestTimerEnable();
        verifyNoMoreInteractions(config);
    }
}
