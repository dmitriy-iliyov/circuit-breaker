package io.github.dmitriyiliyov.circuitbreaker.example;

public class SpecificBusinessException extends RuntimeException {
    public SpecificBusinessException(String message) {
        super(message);
    }
}
