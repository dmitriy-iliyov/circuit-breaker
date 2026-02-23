package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

public interface OpenObserveStrategy {
    void onRequest();
    boolean shouldTrip();
    void reset();
}
