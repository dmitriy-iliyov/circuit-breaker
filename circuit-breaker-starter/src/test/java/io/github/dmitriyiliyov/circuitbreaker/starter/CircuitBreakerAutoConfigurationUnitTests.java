package io.github.dmitriyiliyov.circuitbreaker.starter;


import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.DefaultStrategiesProvider;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.StrategiesProvider;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.StrategyProvider;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.TimeBasedOpenStrategyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerAutoConfigurationUnitTests {

    private CircuitBreakerAutoConfiguration autoConfiguration;

    @BeforeEach
    void setUp() {
        autoConfiguration = new CircuitBreakerAutoConfiguration();
    }

    @Test
    @DisplayName("UT should create StrategiesProvider with default providers when the list is empty")
    void shouldCreateStrategiesProviderWithDefaultProviders() {
        StrategiesProvider strategiesProvider = autoConfiguration.strategiesProvider(Collections.emptyList());
        assertThat(strategiesProvider).isInstanceOf(DefaultStrategiesProvider.class);
        DefaultStrategiesProvider defaultProvider = (DefaultStrategiesProvider) strategiesProvider;
        assertThat(defaultProvider.getProviders()).hasSize(5);
    }

    @Test
    @DisplayName("UT should create StrategiesProvider with custom providers when the list is not empty")
    void shouldCreateStrategiesProviderWithCustomProviders() {
        StrategyProvider customProvider = new TimeBasedOpenStrategyProvider();
        List<StrategyProvider> customProviders = List.of(customProvider);

        StrategiesProvider strategiesProvider = autoConfiguration.strategiesProvider(customProviders);

        assertThat(strategiesProvider).isInstanceOf(DefaultStrategiesProvider.class);
        DefaultStrategiesProvider defaultProvider = (DefaultStrategiesProvider) strategiesProvider;
        assertThat(defaultProvider.getProviders()).isEqualTo(customProviders);
        assertThat(defaultProvider.getProviders().size()).isEqualTo(1);
    }
}
