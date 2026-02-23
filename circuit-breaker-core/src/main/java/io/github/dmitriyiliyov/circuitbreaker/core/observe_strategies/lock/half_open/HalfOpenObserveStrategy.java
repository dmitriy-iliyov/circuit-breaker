package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open;

public interface HalfOpenObserveStrategy {
    void onRequest();
    void onException();
    HalfOpenTransition getTransition();
    void reset();
}
