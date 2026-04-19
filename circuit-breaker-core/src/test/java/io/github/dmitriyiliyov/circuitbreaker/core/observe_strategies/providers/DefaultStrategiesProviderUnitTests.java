package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.Strategies;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.config.HalfOpenType;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenStateStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DefaultStrategiesProviderUnitTests {

    private final CloseStateStrategy closeStrategy = mock(CloseStateStrategy.class);
    private final HalfOpenStateStrategy halfOpenStrategy = mock(HalfOpenStateStrategy.class);
    private final OpenStateStrategy openStrategy = mock(OpenStateStrategy.class);

    private final StrategyProvider closeProvider = mock(StrategyProvider.class);
    private final StrategyProvider halfOpenProvider = mock(StrategyProvider.class);
    private final StrategyProvider openProvider = mock(StrategyProvider.class);

    private DefaultStrategiesProvider providerWithHalfOpen() {
        when(closeProvider.getStateType()).thenReturn(CircuitStateType.CLOSE);
        when(halfOpenProvider.getStateType()).thenReturn(CircuitStateType.HALF_OPEN);
        when(openProvider.getStateType()).thenReturn(CircuitStateType.OPEN);
        return new DefaultStrategiesProvider(List.of(closeProvider, openProvider, halfOpenProvider));
    }

    private DefaultStrategiesProvider providerWithoutHalfOpen() {
        when(closeProvider.getStateType()).thenReturn(CircuitStateType.CLOSE);
        when(openProvider.getStateType()).thenReturn(CircuitStateType.OPEN);
        return new DefaultStrategiesProvider(List.of(closeProvider, openProvider));
    }

    private CircuitBreakerConfiguration configWithHalfOpen() {
        return CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(b -> b.observeTime(Duration.ofSeconds(1)).exceptionRateThreshold(0.5).initialDelay(Duration.ZERO))
                .halfOpenState(halfOpenState -> halfOpenState
                        .halfOpenStateEnabled(true)
                        .type(HalfOpenType.NORMAL)
                        .maxRequestInHalfOpenState(10)
                        .maxExceptionCountInHalfOpenState(5)
                )
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();
    }

    private CircuitBreakerConfiguration configWithoutHalfOpen() {
        return CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(b -> b.observeTime(Duration.ofSeconds(1)).exceptionRateThreshold(0.5).initialDelay(Duration.ZERO))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();
    }

    @Test
    @DisplayName("getStrategies: should return strategies with all three when halfOpen is enabled")
    void getStrategies_shouldReturnAllThreeStrategiesWhenHalfOpenEnabled() {
        CircuitBreakerConfiguration config = configWithHalfOpen();
        DefaultStrategiesProvider provider = providerWithHalfOpen();

        when(closeProvider.supports(config)).thenReturn(true);
        when(closeProvider.getStrategy(config)).thenReturn(closeStrategy);
        when(halfOpenProvider.supports(config)).thenReturn(true);
        when(halfOpenProvider.getStrategy(config)).thenReturn(halfOpenStrategy);
        when(openProvider.supports(config)).thenReturn(true);
        when(openProvider.getStrategy(config)).thenReturn(openStrategy);

        Strategies strategies = provider.getStrategies(config);

        assertThat(strategies.closeStateStrategy()).isSameAs(closeStrategy);
        assertThat(strategies.halfOpenStateStrategy()).isSameAs(halfOpenStrategy);
        assertThat(strategies.openStateStrategy()).isSameAs(openStrategy);
    }

    @Test
    @DisplayName("getStrategies: halfOpenStateStrategy should be null when halfOpen is disabled")
    void getStrategies_halfOpenStrategyShouldBeNullWhenDisabled() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        DefaultStrategiesProvider provider = providerWithoutHalfOpen();

        when(closeProvider.supports(config)).thenReturn(true);
        when(closeProvider.getStrategy(config)).thenReturn(closeStrategy);
        when(openProvider.supports(config)).thenReturn(true);
        when(openProvider.getStrategy(config)).thenReturn(openStrategy);

        Strategies strategies = provider.getStrategies(config);

        assertThat(strategies.closeStateStrategy()).isSameAs(closeStrategy);
        assertThat(strategies.halfOpenStateStrategy()).isNull();
        assertThat(strategies.openStateStrategy()).isSameAs(openStrategy);
    }

    @Test
    @DisplayName("getStrategies: should use first supporting provider when multiple present")
    void getStrategies_shouldUseFirstSupportingProvider() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();

        StrategyProvider secondCloseProvider = mock(StrategyProvider.class);
        when(secondCloseProvider.getStateType()).thenReturn(CircuitStateType.CLOSE);

        when(closeProvider.getStateType()).thenReturn(CircuitStateType.CLOSE);
        when(openProvider.getStateType()).thenReturn(CircuitStateType.OPEN);

        DefaultStrategiesProvider provider = new DefaultStrategiesProvider(List.of(closeProvider, secondCloseProvider, openProvider));

        when(closeProvider.supports(config)).thenReturn(true);
        when(closeProvider.getStrategy(config)).thenReturn(closeStrategy);
        when(openProvider.supports(config)).thenReturn(true);
        when(openProvider.getStrategy(config)).thenReturn(openStrategy);

        provider.getStrategies(config);

        verify(closeProvider, times(1)).getStrategy(config);
        verify(secondCloseProvider, never()).getStrategy(any());
    }

    @Test
    @DisplayName("getStrategies: should skip non-supporting provider and use next")
    void getStrategies_shouldSkipNonSupportingProvider() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();

        StrategyProvider fallbackCloseProvider = mock(StrategyProvider.class);
        when(fallbackCloseProvider.getStateType()).thenReturn(CircuitStateType.CLOSE);

        CloseStateStrategy fallbackStrategy = mock(CloseStateStrategy.class);

        when(closeProvider.getStateType()).thenReturn(CircuitStateType.CLOSE);
        when(openProvider.getStateType()).thenReturn(CircuitStateType.OPEN);

        DefaultStrategiesProvider provider = new DefaultStrategiesProvider(List.of(closeProvider, fallbackCloseProvider, openProvider));

        when(closeProvider.supports(config)).thenReturn(false);
        when(fallbackCloseProvider.supports(config)).thenReturn(true);
        when(fallbackCloseProvider.getStrategy(config)).thenReturn(fallbackStrategy);
        when(openProvider.supports(config)).thenReturn(true);
        when(openProvider.getStrategy(config)).thenReturn(openStrategy);

        Strategies strategies = provider.getStrategies(config);

        assertThat(strategies.closeStateStrategy()).isSameAs(fallbackStrategy);
    }

    @Test
    @DisplayName("findStrategyProvider: should throw when no supporting provider found")
    void findStrategyProvider_shouldThrowWhenNoSupportingProviderFound() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        DefaultStrategiesProvider provider = providerWithoutHalfOpen();

        when(closeProvider.supports(config)).thenReturn(false);
        when(openProvider.supports(config)).thenReturn(true);
        when(openProvider.getStrategy(config)).thenReturn(openStrategy);

        assertThatThrownBy(() -> provider.getStrategies(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSE");
    }

    @Test
    @DisplayName("findStrategyProvider: should throw when state type has no providers at all")
    void findStrategyProvider_shouldThrowWhenStateTypeHasNoProviders() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();

        when(openProvider.getStateType()).thenReturn(CircuitStateType.OPEN);

        DefaultStrategiesProvider provider = new DefaultStrategiesProvider(List.of(openProvider));

        when(openProvider.supports(config)).thenReturn(true);
        when(openProvider.getStrategy(config)).thenReturn(openStrategy);

        assertThatThrownBy(() -> provider.getStrategies(config))
                .isInstanceOf(NullPointerException.class);
    }
}