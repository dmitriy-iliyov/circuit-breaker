package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.LockFreeCountBasedHalfOpenStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LockFreeCountBasedHalfOpenStrategyProviderUnitTests {

    private final LockFreeCountBasedHalfOpenStrategyProvider provider = new LockFreeCountBasedHalfOpenStrategyProvider();
    private final CircuitBreakerConfiguration config = mock(CircuitBreakerConfiguration.class);

    @Test
    @DisplayName("getStateType: should return HALF_OPEN")
    void getStateType_shouldReturnHalfOpen() {
        assertThat(provider.getStateType()).isEqualTo(CircuitStateType.HALF_OPEN);
    }

    @Test
    @DisplayName("getStrategy: should return LockFreeCountBasedHalfOpenStrategy for valid config")
    void getStrategy_shouldReturnCorrectStrategy() {
        when(config.isHalfOpenStateEnabled()).thenReturn(true);
        when(config.getLockFree()).thenReturn(true);
        when(config.getMaxRequestInHalfOpenState()).thenReturn(10);
        when(config.getMaxExceptionCountInHalfOpenState()).thenReturn(5);

        Object strategy = provider.getStrategy(config);

        assertThat(strategy).isInstanceOf(LockFreeCountBasedHalfOpenStrategy.class);
    }

    @Test
    @DisplayName("getStrategy: should throw exception for unsupported config")
    void getStrategy_shouldThrowException_forUnsupportedConfig() {
        when(config.isHalfOpenStateEnabled()).thenReturn(false);

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
    @DisplayName("supports: should return false if half-open state is disabled")
    void supports_shouldReturnFalse_ifHalfOpenStateIsDisabled() {
        when(config.isHalfOpenStateEnabled()).thenReturn(false);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("supports: should return false if lock-free mode is disabled")
    void supports_shouldReturnFalse_ifLockFreeIsDisabled() {
        when(config.isHalfOpenStateEnabled()).thenReturn(true);
        when(config.getLockFree()).thenReturn(false);

        assertThat(provider.supports(config)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({ "0", "-1" })
    @DisplayName("supports: should return false if max requests is not positive")
    void supports_shouldReturnFalse_ifMaxRequestsIsNotPositive(int maxRequests) {
        when(config.isHalfOpenStateEnabled()).thenReturn(true);
        when(config.getLockFree()).thenReturn(true);
        when(config.getMaxRequestInHalfOpenState()).thenReturn(maxRequests);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("supports: should return false if max exceptions is negative")
    void supports_shouldReturnFalse_ifMaxExceptionsIsNegative() {
        when(config.isHalfOpenStateEnabled()).thenReturn(true);
        when(config.getLockFree()).thenReturn(true);
        when(config.getMaxRequestInHalfOpenState()).thenReturn(10);
        when(config.getMaxExceptionCountInHalfOpenState()).thenReturn(-1);

        assertThat(provider.supports(config)).isFalse();
    }

    @Test
    @DisplayName("supports: should return true for valid configuration")
    void supports_shouldReturnTrue_forValidConfiguration() {
        when(config.isHalfOpenStateEnabled()).thenReturn(true);
        when(config.getLockFree()).thenReturn(true);
        when(config.getMaxRequestInHalfOpenState()).thenReturn(10);
        when(config.getMaxExceptionCountInHalfOpenState()).thenReturn(5);

        assertThat(provider.supports(config)).isTrue();
    }
}
