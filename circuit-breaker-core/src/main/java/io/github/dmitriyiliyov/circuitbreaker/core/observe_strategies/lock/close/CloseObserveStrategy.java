package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

public interface CloseObserveStrategy {
    void onRequest();
    void onException();
    boolean shouldTrip();
    void reset();
}
