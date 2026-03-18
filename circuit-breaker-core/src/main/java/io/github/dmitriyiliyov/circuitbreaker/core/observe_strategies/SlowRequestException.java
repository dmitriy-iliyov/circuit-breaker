package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

public class SlowRequestException extends RuntimeException {
    public SlowRequestException(String message) {
        super(message);
    }
}
