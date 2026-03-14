package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenStateStrategy;

/**
 * A container for the strategies that define the behavior of each circuit breaker state.
 *
 * @param closeStateStrategy    The strategy for the {@link CloseState}.
 * @param halfOpenStateStrategy The strategy for the {@link HalfOpenState}.
 * @param openStateStrategy     The strategy for the {@link OpenState}.
 */
public record Strategies(
        CloseStateStrategy closeStateStrategy,
        HalfOpenStateStrategy halfOpenStateStrategy,
        OpenStateStrategy openStateStrategy
) { }
