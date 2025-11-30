package io.github.dmitriyiliyov.circuitbreaker;

import io.github.dmitriyiliyov.circuitbreaker.strategies.ObserveStrategy;

import java.util.function.Supplier;

public class CloseState implements CircuitState {

    private final CircuitBreaker circuitBreaker;
    private final CircuitState nextState;
    private final ObserveStrategy strategy;

    public CloseState(CircuitBreaker circuitBreaker, CircuitState nextState, ObserveStrategy strategy) {
        this.circuitBreaker = circuitBreaker;
        this.nextState = nextState;
        this.strategy = strategy;
    }

    @Override
    public void process(Runnable process) {
        strategy.observe(
                process,
                (exception) -> circuitBreaker.getObservableExceptions().contains(exception.getClass()),
                () -> circuitBreaker.setState(nextState)
        );
    }

    @Override
    public <T> T process(Supplier<T> process) {
        return strategy.observe(
                process,
                (exception) -> circuitBreaker.getObservableExceptions().contains(exception.getClass()),
                () -> circuitBreaker.setState(nextState)
        );
    }

    @Override
    public void reset() {
        strategy.reset();
    }
}
