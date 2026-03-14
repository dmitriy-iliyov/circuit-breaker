package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.StrategiesProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DefaultCircuitBreakerFactoryUnitTests {

    private final CircuitBreakerRegistry registry = mock(CircuitBreakerRegistry.class);
    private final StrategiesProvider strategiesProvider = mock(StrategiesProvider.class);

    private final CloseStateStrategy closeStateStrategy = mock(CloseStateStrategy.class);
    private final HalfOpenStateStrategy halfOpenStateStrategy = mock(HalfOpenStateStrategy.class);
    private final OpenStateStrategy openStateStrategy = mock(OpenStateStrategy.class);

    private final Strategies strategiesWithHalfOpen = new Strategies(
            closeStateStrategy, halfOpenStateStrategy, openStateStrategy
    );
    private final Strategies strategiesWithoutHalfOpen = new Strategies(
            closeStateStrategy, null, openStateStrategy
    );

    private final DefaultCircuitBreakerFactory factory = new DefaultCircuitBreakerFactory(registry, strategiesProvider);

    private CircuitBreakerConfiguration configWithHalfOpen() {
        return CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(b -> b.observeTime(Duration.ofSeconds(1)).exceptionRateThreshold(0.5).initialDelay(Duration.ZERO))
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .maxExceptionCountInHalfOpenState(5)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();
    }

    private CircuitBreakerConfiguration configWithoutHalfOpen() {
        return CircuitBreakerConfiguration.builder()
                .name("test")
                .observableExceptions(Set.of(RuntimeException.class))
                .closeState(b -> b.observeTime(Duration.ofSeconds(1)).exceptionRateThreshold(0.5).initialDelay(Duration.ZERO))
                .halfOpenStateEnabled(false)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();
    }

    @Test
    @DisplayName("create: should return non-null CircuitBreaker")
    public void of_shouldReturnNonNullCircuitBreaker() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        when(strategiesProvider.getStrategies(config)).thenReturn(strategiesWithoutHalfOpen);

        CircuitBreaker result = factory.of(config);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("create: should register circuit breaker in registry with config name")
    public void of_shouldRegisterCircuitBreakerInRegistry() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        when(strategiesProvider.getStrategies(config)).thenReturn(strategiesWithoutHalfOpen);

        CircuitBreaker result = factory.of(config);

        verify(registry, times(1)).register(config, result);
    }

    @Test
    @DisplayName("create: should call getStrategies with configuration")
    public void of_shouldCallGetStrategiesWithConfiguration() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        when(strategiesProvider.getStrategies(config)).thenReturn(strategiesWithoutHalfOpen);

        factory.of(config);

        verify(strategiesProvider, times(1)).getStrategies(config);
    }

    @Test
    @DisplayName("create: should use init when halfOpenState is enabled")
    public void of_shouldUseInitWhenHalfOpenStateEnabled() {
        CircuitBreakerConfiguration config = configWithHalfOpen();
        when(strategiesProvider.getStrategies(config)).thenReturn(strategiesWithHalfOpen);

        CircuitBreaker result = factory.of(config);

        assertThat(result).isNotNull();
        assertThat(result.getState()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("create: should use initWithoutHalfOpenState when halfOpenState is disabled")
    public void of_shouldUseInitWithoutHalfOpenStateWhenDisabled() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        when(strategiesProvider.getStrategies(config)).thenReturn(strategiesWithoutHalfOpen);

        CircuitBreaker result = factory.of(config);

        assertThat(result).isNotNull();
        assertThat(result.getState()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("createFromExists: should return non-null CircuitBreaker")
    public void ofExists_shouldReturnNonNullCircuitBreaker() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        when(registry.getConfiguration("test")).thenReturn(config);
        when(strategiesProvider.getStrategies(any(CircuitBreakerConfiguration.class))).thenReturn(strategiesWithoutHalfOpen);

        CircuitBreaker result = factory.ofExists("test", "test-copy");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("createFromExists: should register new circuit breaker under new name")
    public void ofExists_shouldRegisterUnderNewName() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        when(registry.getConfiguration("test")).thenReturn(config);
        when(strategiesProvider.getStrategies(any(CircuitBreakerConfiguration.class))).thenReturn(strategiesWithoutHalfOpen);

        CircuitBreaker result = factory.ofExists("test", "test-copy");

        ArgumentCaptor<CircuitBreakerConfiguration> configCaptor = ArgumentCaptor.forClass(CircuitBreakerConfiguration.class);
        verify(registry, times(1)).register(configCaptor.capture(), eq(result));
        assertThat(configCaptor.getValue().getName()).isEqualTo("test-copy");
    }

    @Test
    @DisplayName("createFromExists: should fetch configuration from registry by reference name")
    public void ofRegistry() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        when(registry.getConfiguration("test")).thenReturn(config);
        when(strategiesProvider.getStrategies(any(CircuitBreakerConfiguration.class))).thenReturn(strategiesWithoutHalfOpen);

        factory.ofExists("test", "test-copy");

        verify(registry, times(1)).getConfiguration("test");
    }

    @Test
    @DisplayName("createFromExists: should not register under reference name")
    public void ofExists_shouldNotRegisterUnderReferenceName() {
        CircuitBreakerConfiguration config = configWithoutHalfOpen();
        when(registry.getConfiguration("test")).thenReturn(config);
        when(strategiesProvider.getStrategies(any(CircuitBreakerConfiguration.class))).thenReturn(strategiesWithoutHalfOpen);

        factory.ofExists("test", "test-copy");

        verify(registry, never()).register(eq(config), any());
    }
}
