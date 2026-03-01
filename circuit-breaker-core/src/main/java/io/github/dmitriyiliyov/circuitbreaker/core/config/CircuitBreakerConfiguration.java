package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.util.Set;
import java.util.function.Consumer;

public final class CircuitBreakerConfiguration {

    private final Set<Class<? extends Throwable>> observableExceptions;
    private final Set<Class<? extends Throwable>> ignorableExceptions;
    private final Boolean lockFree;
    private final CloseStateConfiguration closeState;
    private final HalfOpenStateConfiguration halfOpenState;
    private final OpenStateConfiguration openState;

    private CircuitBreakerConfiguration(Set<Class<? extends Throwable>> observableExceptions,
                                        Set<Class<? extends Throwable>> ignorableExceptions,
                                        Boolean lockFree,
                                        CloseStateConfiguration closeState,
                                        HalfOpenStateConfiguration halfOpenState,
                                        OpenStateConfiguration openState) {
        if (observableExceptions == null || observableExceptions.isEmpty()) {
            throw new IllegalArgumentException("observableExceptions cannot be null or empty");
        }
        this.observableExceptions = observableExceptions;
        if (ignorableExceptions == null || ignorableExceptions.isEmpty()) {
            throw new IllegalArgumentException("ignorableExceptions cannot be null or empty");
        }
        this.ignorableExceptions = ignorableExceptions;
        this.lockFree = lockFree == null || lockFree;
        if (closeState == null) {
            throw new IllegalArgumentException("closeState cannot be null");
        }
        this.closeState = closeState;
        this.halfOpenState = halfOpenState;
        if (openState == null) {
            throw new IllegalArgumentException("openState cannot be null");
        }
        this.openState = openState;
    }

    public Set<Class<? extends Throwable>> getObservableExceptions() {
        return observableExceptions;
    }

    public Set<Class<? extends Throwable>> getIgnorableExceptions() {
        return ignorableExceptions;
    }

    public Boolean getLockFree() {
        return lockFree;
    }

    public CloseStateConfiguration getCloseState() {
        return closeState;
    }

    public HalfOpenStateConfiguration getHalfOpenState() {
        return halfOpenState;
    }

    public OpenStateConfiguration getOpenState() {
        return openState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Set<Class<? extends Throwable>> observableExceptions;
        private Set<Class<? extends Throwable>> ignorableExceptions;
        private Boolean lockFree;
        private CloseStateConfiguration closeState;
        private HalfOpenStateConfiguration halfOpenState;
        private OpenStateConfiguration openState;

        public Builder observableExceptions(Set<Class<? extends Throwable>> observableExceptions) {
            this.observableExceptions = observableExceptions;
            return this;
        }

        public Builder ignorableExceptions(Set<Class<? extends Throwable>> ignorableExceptions) {
            this.ignorableExceptions = ignorableExceptions;
            return this;
        }

        public Builder lockFree(Boolean lockFree) {
            this.lockFree = lockFree;
            return this;
        }

        public Builder closeState(Consumer<CloseStateConfiguration.Builder> builderConsumer) {
            CloseStateConfiguration.Builder builder = CloseStateConfiguration.builder();
            builderConsumer.accept(builder);
            this.closeState = builder.build();
            return this;
        }

        public Builder halfOpenState(Consumer<HalfOpenStateConfiguration.Builder> builderConsumer) {
            HalfOpenStateConfiguration.Builder builder = HalfOpenStateConfiguration.builder();
            builderConsumer.accept(builder);
            this.halfOpenState = builder.build();
            return this;
        }

        public Builder openState(Consumer<OpenStateConfiguration.Builder> builderConsumer) {
            OpenStateConfiguration.Builder builder = OpenStateConfiguration.builder();
            builderConsumer.accept(builder);
            this.openState = builder.build();
            return this;
        }

        public CircuitBreakerConfiguration build() {
            return new CircuitBreakerConfiguration(
                    observableExceptions,
                    ignorableExceptions,
                    lockFree,
                    closeState,
                    halfOpenState,
                    openState
            );
        }
    }
}
