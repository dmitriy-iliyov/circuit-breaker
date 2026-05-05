package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CloseStateConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.LockFreeSlidingWindowCloseStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LockFreeSlidingWindowCloseStrategyProviderUnitTests {

    private final LockFreeSlidingWindowCloseStrategyProvider provider = new LockFreeSlidingWindowCloseStrategyProvider();
    private final CircuitBreakerConfiguration config = mock(CircuitBreakerConfiguration.class);
    private final CloseStateConfiguration closeStateConfig = mock(CloseStateConfiguration.class);

    @Test
    @DisplayName("UT getStateType() should return CLOSE")
    void getStateType_shouldReturnClose() {
        assertThat(provider.getStateType()).isEqualTo(CircuitStateType.CLOSE);
    }

    @Test
    @DisplayName("UT getStrategy() should return LockFreeSlidingWindowCloseStrategy for valid config")
    void getStrategy_shouldReturnCorrectStrategy() {
        when(config.getCloseStateConfiguration()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(true);
        when(closeStateConfig.getWindowSize()).thenReturn(10);
        when(closeStateConfig.getExceptionCountThreshold()).thenReturn(5);
        when(closeStateConfig.getInitialDelay()).thenReturn(Duration.ofSeconds(1));

        Object strategy = provider.getStrategy(config);

        assertThat(strategy).isInstanceOf(LockFreeSlidingWindowCloseStrategy.class);
    }

    @Test
    @DisplayName("UT getStrategy() should throw exception for unsupported config")
    void getStrategy_shouldThrowException_forUnsupportedConfig() {
        when(config.getCloseStateConfiguration()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(false);

        assertThatThrownBy(() -> provider.getStrategy(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("don't supports");
    }

    @Test
    @DisplayName("UT supports() should throw exception for null configuration")
    void supports_shouldThrowException_forNullConfiguration() {
        assertThatThrownBy(() -> provider.supports(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("configuration cannot be null");
    }

    @Test
    @DisplayName("UT supports() should throw exception for null close state configuration")
    void supports_shouldThrowException_forNullCloseStateConfiguration() {
        when(config.getCloseStateConfiguration()).thenReturn(null);
        assertThatThrownBy(() -> provider.supports(config))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("close state configuration cannot be null");
    }

    @Test
    @DisplayName("UT supports() should return false if lock-free mode is disabled")
    void supports_shouldReturnFalse_ifLockFreeIsDisabled() {
        when(config.getCloseStateConfiguration()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(false);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("UT supports() should return false if window size is null")
    void supports_shouldReturnFalse_ifWindowSizeIsNull() {
        when(config.getCloseStateConfiguration()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(true);
        when(closeStateConfig.getWindowSize()).thenReturn(null);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("UT supports() should return false if exception count threshold is null")
    void supports_shouldReturnFalse_ifExceptionCountThresholdIsNull() {
        when(config.getCloseStateConfiguration()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(true);
        when(closeStateConfig.getWindowSize()).thenReturn(10);
        when(closeStateConfig.getExceptionCountThreshold()).thenReturn(null);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("UT supports() should return false if initial delay is null")
    void supports_shouldReturnFalse_ifInitialDelayIsNull() {
        when(config.getCloseStateConfiguration()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(true);
        when(closeStateConfig.getWindowSize()).thenReturn(10);
        when(closeStateConfig.getExceptionCountThreshold()).thenReturn(5);
        when(closeStateConfig.getInitialDelay()).thenReturn(null);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("UT supports() should return true for valid configuration")
    void supports_shouldReturnTrue_forValidConfiguration() {
        when(config.getCloseStateConfiguration()).thenReturn(closeStateConfig);
        when(config.getLockFree()).thenReturn(true);
        when(closeStateConfig.getWindowSize()).thenReturn(10);
        when(closeStateConfig.getExceptionCountThreshold()).thenReturn(5);
        when(closeStateConfig.getInitialDelay()).thenReturn(Duration.ofSeconds(1));

        assertThat(provider.supports(config)).isTrue();
    }
}
