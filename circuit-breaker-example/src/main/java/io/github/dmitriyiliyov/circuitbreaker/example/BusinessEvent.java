package io.github.dmitriyiliyov.circuitbreaker.example;

import java.util.UUID;

public record BusinessEvent(
        UUID id
) {
    public static BusinessEvent of() {
        return new BusinessEvent(UUID.randomUUID());
    }
}
