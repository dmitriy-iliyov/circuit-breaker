package io.github.dmitriyiliyov.circuitbreaker.core;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Default CircuitBreaker implementation.
 */
public class DefaultCircuitBreaker implements CircuitBreaker, ConfigurableCircuitBreaker {

    private final Set<Class<? extends Throwable>> observableExceptions;
    private final Set<Class<? extends Throwable>> ignorableExceptions;
    private AtomicReference<CircuitState> state;

    /**
     * Creates a new DefaultCircuitBreaker instance.
     * <p>
     * <b>Note:</b> This constructor is intended to be used only by {@link DefaultCircuitBreakerFactory}
     * for initializing the circuit breaker. The state must be set later via {@link #setState(CircuitState)}
     * before the circuit breaker can be used.
     *
     * @param observableExceptions set of exceptions to observe
     * @param ignorableExceptions  set of exceptions to ignore
     */
    DefaultCircuitBreaker(Set<Class<? extends Throwable>> observableExceptions,
                          Set<Class<? extends Throwable>> ignorableExceptions) {
        this.observableExceptions = Set.copyOf(observableExceptions);
        this.ignorableExceptions = Set.copyOf(ignorableExceptions);
    }

    @Override
    public void execute(CheckedRunnable process) throws Throwable {
        state.get().execute(process);
    }

    @Override
    public <T> T execute(CheckedSupplier<T> process) throws Throwable {
        return state.get().execute(process);
    }

    @Override
    public Set<Class<? extends Throwable>> getObservableExceptions() {
        return observableExceptions;
    }

    @Override
    public Set<Class<? extends Throwable>> getIgnorableExceptions() {
        return ignorableExceptions;
    }

    @Override
    public Function<Throwable, Boolean> getChecker() {
        return throwable -> {
            for (Class<? extends Throwable> ignorable : ignorableExceptions) {
                if (ignorable.isInstance(throwable)) {
                    return false;
                }
            }
            for (Class<? extends Throwable> observable : observableExceptions) {
                if (observable.isInstance(throwable)) {
                    return true;
                }
            }
            return false;
        };
    }

    @Override
    public boolean trySetState(CircuitState previousState, CircuitState nextState) {
        return state.compareAndSet(previousState, nextState);
    }

    @Override
    public void setState(CircuitState state) {
        Objects.requireNonNull(state, "state cannot be null");
        if (this.state == null) {
            this.state = new AtomicReference<>(state);
        } else {
            throw new IllegalStateException("cannot modify state with this method");
        }
    }

    @Override
    public CircuitState getState() {
        return state.get();
    }
}
