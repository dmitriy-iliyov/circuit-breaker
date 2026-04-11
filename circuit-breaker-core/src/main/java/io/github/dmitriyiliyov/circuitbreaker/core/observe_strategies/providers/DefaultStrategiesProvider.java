package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers;

import io.github.dmitriyiliyov.circuitbreaker.core.Strategies;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenStateStrategy;

import java.util.*;
import java.util.stream.Collectors;

public final class DefaultStrategiesProvider implements StrategiesProvider {

    private final Map<CircuitStateType, List<StrategyProvider>> providers;

    public DefaultStrategiesProvider(List<StrategyProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(
                                StrategyProvider::getStateType,
                                () -> new EnumMap<>(CircuitStateType.class),
                                Collectors.toList()
                        ),
                        Collections::unmodifiableMap)
                );
    }

    @Override
    public Strategies getStrategies(CircuitBreakerConfiguration configuration) {
        CloseStateStrategy closeStateStrategy = (CloseStateStrategy) findStrategyProvider(CircuitStateType.CLOSE, configuration)
                .getStrategy(configuration);
        HalfOpenStateStrategy halfOpenStateStrategy = null;
        if (configuration.isHalfOpenStateEnabled()) {
            halfOpenStateStrategy = (HalfOpenStateStrategy) findStrategyProvider(CircuitStateType.HALF_OPEN, configuration)
                    .getStrategy(configuration);
        }
        OpenStateStrategy openStateStrategy = (OpenStateStrategy) findStrategyProvider(CircuitStateType.OPEN, configuration)
                .getStrategy(configuration);
        return new Strategies(
                closeStateStrategy,
                halfOpenStateStrategy,
                openStateStrategy
        );
    }

    private StrategyProvider findStrategyProvider(CircuitStateType stateType, CircuitBreakerConfiguration configuration) {
        for (StrategyProvider provider : providers.get(stateType)) {
            if (provider.supports(configuration)) {
                return provider;
            }
        }
        throw new IllegalStateException("strategy provider not found for '%s' state".formatted(stateType.name()));
    }

    public List<StrategyProvider> getProviders() {
        return providers.values().stream().flatMap(Collection::stream).toList();
    }
}
