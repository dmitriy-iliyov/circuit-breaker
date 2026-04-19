package io.github.dmitriyiliyov.circuitbreaker.core;

public class GradualHalfOpenRefuseException extends RuntimeException {
    public GradualHalfOpenRefuseException(String message) {
        super(message);
    }

    public GradualHalfOpenRefuseException() {
        super("Gradual half open state randomly refuse this request");
    }
}
