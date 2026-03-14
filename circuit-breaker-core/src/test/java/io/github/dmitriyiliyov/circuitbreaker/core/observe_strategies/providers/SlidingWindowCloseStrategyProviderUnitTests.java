package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CloseStateConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.SlidingWindowCloseStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlidingWindowCloseStrategyProviderUnitTests {

    private final SlidingWindowCloseStrategyProvider provider = new SlidingWindowCloseStrategyProvider();
    private final CircuitBreakerConfiguration config = mock(CircuitBreakerConfiguration.class);
    private final CloseStateConfiguration closeStateConfig = mock(CloseStateConfiguration.class);

    @Test
    @DisplayName("getStateType: should return CLOSE")
    void getStateType_shouldReturnClose() {
        assertThat(provider.getStateType()).isEqualTo(CircuitStateType.CLOSE);
    }

    @Test
    @DisplayName("getStrategy: should return SlidingWindowCloseStrategy for valid config")
    void getStrategy_shouldReturnCorrectStrategy() {
        when(config.getCloseState()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(false);
        when(closeStateConfig.getWindowSize()).thenReturn(10);
        when(closeStateConfig.getExceptionCountThreshold()).thenReturn(5);
        when(closeStateConfig.getInitialDelay()).thenReturn(Duration.ofSeconds(1));

        Object strategy = provider.getStrategy(config);

        assertThat(strategy).isInstanceOf(SlidingWindowCloseStrategy.class);
    }

    @Test
    @DisplayName("getStrategy: should throw exception for unsupported config")
    void getStrategy_shouldThrowException_forUnsupportedConfig() {
        when(config.getCloseState()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(true); // Invalid config

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
    @DisplayName("supports: should throw exception for null close state configuration")
    void supports_shouldThrowException_forNullCloseStateConfiguration() {
        when(config.getCloseState()).thenReturn(null);
        assertThatThrownBy(() -> provider.supports(config))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("close state configuration cannot be null");
    }

    @Test
    @DisplayName("supports: should return false if lock-free mode is enabled")
    void supports_shouldReturnFalse_ifLockFreeIsEnabled() {
        when(config.getCloseState()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(true);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("supports: should return false if window size is null")
    void supports_shouldReturnFalse_ifWindowSizeIsNull() {
        when(config.getCloseState()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(false);
        when(closeStateConfig.getWindowSize()).thenReturn(null);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("supports: should return false if exception count threshold is null")
    void supports_shouldReturnFalse_ifExceptionCountThresholdIsNull() {
        when(config.getCloseState()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(false);
        when(closeStateConfig.getWindowSize()).thenReturn(10);
        when(closeStateConfig.getExceptionCountThreshold()).thenReturn(null);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("supports: should return false if initial delay is null")
    void supports_shouldReturnFalse_ifInitialDelayIsNull() {
        when(config.getCloseState()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(false);
        when(closeStateConfig.getWindowSize()).thenReturn(10);
        when(closeStateConfig.getExceptionCountThreshold()).thenReturn(5);
        when(closeStateConfig.getInitialDelay()).thenReturn(null);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("supports: should return true for valid configuration")
    void supports_shouldReturnTrue_forValidConfiguration() {
        when(config.getCloseState()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(false);
        when(closeStateConfig.getWindowSize()).thenReturn(10);
        when(closeStateConfig.getExceptionCountThreshold()).thenReturn(5);
        when(closeStateConfig.getInitialDelay()).thenReturn(Duration.ofSeconds(1));

        assertThat(provider.supports(config)).isTrue();
    }
}
