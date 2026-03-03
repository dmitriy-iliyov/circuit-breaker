package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseObserveStrategy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeFixedRequestWindowErrorRateStrategy implements CloseObserveStrategy {

    private final int windowSize;
    private final int exceptionCountThreshold;
    private final long observeStartMillis;
    private final AtomicReference<StrategyState> state;

    public LockFreeFixedRequestWindowErrorRateStrategy(int windowSize, double exceptionRateThreshold, Duration waitBeforeStartTime) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        this.windowSize = windowSize;
        if (exceptionRateThreshold < 0) {
            throw new IllegalArgumentException("exceptionRateThreshold must be >= 0");
        }
        this.exceptionCountThreshold = (int) Math.ceil(exceptionRateThreshold * windowSize);
        Objects.requireNonNull(waitBeforeStartTime, "waitBeforeStartTime cannot be null");
        this.observeStartMillis = System.currentTimeMillis() + waitBeforeStartTime.toMillis();
        this.state = new AtomicReference<>(StrategyState.of());
    }

    @Override
    public void onRequest() {
        if (observeStartMillis > System.currentTimeMillis()) {
            return;
        }
        while (true) {
            StrategyState currentState = state.get();
            int currentRequests = currentState.requests().incrementAndGet();
            if (currentRequests > windowSize) {
                if (state.compareAndSet(currentState, StrategyState.of(1, 0))) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    @Override
    public void onException() {
        if (observeStartMillis > System.currentTimeMillis()) {
            return;
        }
        while (true) {
            StrategyState currentState = state.get();
            int currentRequests = currentState.requests().incrementAndGet();
            if (currentRequests > windowSize) {
                if (state.compareAndSet(currentState, StrategyState.of(1, 1))) {
                    return;
                }
            } else {
                currentState.exceptions().incrementAndGet();
                return;
            }
        }
    }

    @Override
    public boolean shouldTrip() {
        int exceptionCount = state.get().exceptions().get();
        return exceptionCount > 0 && exceptionCount >= exceptionCountThreshold ;
    }

    @Override
    public void reset() {
        state.set(StrategyState.of());
    }
}
