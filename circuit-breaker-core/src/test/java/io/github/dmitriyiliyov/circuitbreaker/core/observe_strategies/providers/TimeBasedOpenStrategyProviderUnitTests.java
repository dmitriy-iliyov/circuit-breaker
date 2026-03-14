package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.TimeBasedOpenStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeBasedOpenStrategyProviderUnitTests {

    private final TimeBasedOpenStrategyProvider provider = new TimeBasedOpenStrategyProvider();
    private final CircuitBreakerConfiguration config = mock(CircuitBreakerConfiguration.class);

    @Test
    @DisplayName("getStateType: should return OPEN")
    void getStateType_shouldReturnOpen() {
        assertThat(provider.getStateType()).isEqualTo(CircuitStateType.OPEN);
    }

    @Test
    @DisplayName("getStrategy: should return TimeBasedOpenStrategy for valid config")
    void getStrategy_shouldReturnCorrectStrategy() {
        when(config.getWaitDurationInOpenState()).thenReturn(Duration.ofSeconds(10));

        Object strategy = provider.getStrategy(config);

        assertThat(strategy).isInstanceOf(TimeBasedOpenStrategy.class);
    }

    @Test
    @DisplayName("getStrategy: should throw exception for unsupported config")
    void getStrategy_shouldThrowException_forUnsupportedConfig() {
        when(config.getWaitDurationInOpenState()).thenReturn(null);

        assertThatThrownBy(() -> provider.getStrategy(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("don't supports");
    }

    @Test
    @DisplayName("supports: should throw exception for null configuration")
    void supports_shouldThrowException_forNullConfiguration() {
        assertThatThrownBy(() -> provider.supports(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("configuration cannot be null");
    }

    @Test
    @DisplayName("supports: should return false if wait duration is null")
    void supports_shouldReturnFalse_ifWaitDurationIsNull() {
        when(config.getWaitDurationInOpenState()).thenReturn(null);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("supports: should return true for valid configuration")
    void supports_shouldReturnTrue_forValidConfiguration() {
        when(config.getWaitDurationInOpenState()).thenReturn(Duration.ofSeconds(10));

        assertThat(provider.supports(config)).isTrue();
    }
}
