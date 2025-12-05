package io.github.dmitriyiliyov.circuitbreaker;

import io.github.dmitriyiliyov.circuitbreaker.strategies.open.OpenObserveStrategy;

import java.util.function.Supplier;

public class OpenState implements CircuitState {

    private final CircuitBreaker circuitBreaker;
    private final CircuitState nextState;
    private final OpenObserveStrategy strategy;

    public OpenState(CircuitBreaker circuitBreaker, CircuitState nextState, OpenObserveStrategy strategy) {
        this.circuitBreaker = circuitBreaker;
        this.nextState = nextState;
        this.strategy = strategy;
    }

    @Override
    public void process(Runnable process) {
        strategy.observe(
                process,
                () -> circuitBreaker.setState(nextState)
        );
    }

    @Override
    public <T> T process(Supplier<T> process) {
        return strategy.observe(
                process,
                () -> circuitBreaker.setState(nextState)
        );
    }

    @Override
    public void reset() {
        strategy.reset();
    }
}
