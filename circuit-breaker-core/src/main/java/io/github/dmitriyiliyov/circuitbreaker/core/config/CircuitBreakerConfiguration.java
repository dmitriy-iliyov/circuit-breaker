package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class CircuitBreakerConfiguration {

    private final String name;
    private final Set<Class<? extends Throwable>> observableExceptions;
    private final Set<Class<? extends Throwable>> ignorableExceptions;
    private final Boolean lockFree;
    private final CloseStateConfiguration closeState;
    private final Duration waitDurationInOpenState;
    private final boolean isHalfOpenStateEnabled;
    private final int maxRequestInHalfOpenState;
    private final int maxExceptionCountInHalfOpenState;

    private CircuitBreakerConfiguration(String name,
                                        Set<Class<? extends Throwable>> observableExceptions,
                                        Set<Class<? extends Throwable>> ignorableExceptions,
                                        ExceptionPriority exceptionPriority,
                                        Boolean lockFree,
                                        CloseStateConfiguration closeState,
                                        Boolean isHalfOpenStateEnabled,
                                        Integer maxRequestInHalfOpenState,
                                        Integer maxExceptionCountInHalfOpenState,
                                        Double maxExceptionRateInHalfOpenState,
                                        Duration waitDurationInOpenState) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null, blank or empty");
        }
        this.name = name;

        checkObservableAndIgnorableExceptions(observableExceptions, ignorableExceptions, exceptionPriority);
        Set<Class<? extends Throwable>> mutableObservable = new HashSet<>(observableExceptions);
        Set<Class<? extends Throwable>> mutableIgnorable = new HashSet<>(
                ignorableExceptions == null ? Collections.emptySet() : ignorableExceptions
        );
        prepareObservableAndIgnorableExceptions(mutableObservable, mutableIgnorable, exceptionPriority);
        this.observableExceptions = Set.copyOf(mutableObservable);
        this.ignorableExceptions = Set.copyOf(mutableIgnorable);

        this.lockFree = lockFree == null || lockFree;

        if (closeState == null) {
            throw new IllegalArgumentException("closeState cannot be null");
        }
        this.closeState = closeState;

        if (waitDurationInOpenState == null) {
            throw new IllegalArgumentException("waitDurationInOpenState cannot be null");
        }
        this.waitDurationInOpenState = waitDurationInOpenState;

        this.isHalfOpenStateEnabled = isHalfOpenStateEnabled != null && isHalfOpenStateEnabled;
        if (this.isHalfOpenStateEnabled && (maxRequestInHalfOpenState == null || maxRequestInHalfOpenState == 0)) {
            throw new IllegalArgumentException("maxRequestInHalfOpenState cannot be null or == 0 when isHalfOpenStateEnabled == true");
        } else if (!this.isHalfOpenStateEnabled) {
            this.maxRequestInHalfOpenState = 0;
            this.maxExceptionCountInHalfOpenState = 0;
        } else {
            if (maxRequestInHalfOpenState < 0) {
                throw new IllegalArgumentException("maxRequestInHalfOpenState cannot be < 0");
            }
            this.maxRequestInHalfOpenState = maxRequestInHalfOpenState;

            boolean hasCount = maxExceptionCountInHalfOpenState != null && maxExceptionCountInHalfOpenState > 0;
            boolean hasRate = maxExceptionRateInHalfOpenState != null && maxExceptionRateInHalfOpenState > 0;
            if (!hasCount && !hasRate) {
                throw new IllegalArgumentException("either maxExceptionCountInHalfOpenState or maxExceptionRateInHalfOpenState must be non null and > 0");
            }
            this.maxExceptionCountInHalfOpenState = hasCount
                    ? maxExceptionCountInHalfOpenState
                    : (int) Math.ceil(maxRequestInHalfOpenState * maxExceptionRateInHalfOpenState);
        }
    }

    /**
     * Validates exception sets based on the provided priority.
     * <p>
     * Observable exceptions must always be non-null and non-empty.
     * Ignorable exceptions must be non-null and non-empty only if the priority is {@link ExceptionPriority#IGNORABLE}.
     *
     * @param observableExceptions set of exceptions to observe
     * @param ignorableExceptions  set of exceptions to ignore
     * @param priority             priority for resolving conflicts between observable and ignorable exceptions
     * @throws IllegalArgumentException if validation fails
     */
    private void checkObservableAndIgnorableExceptions(Set<Class<? extends Throwable>> observableExceptions,
                                                       Set<Class<? extends Throwable>> ignorableExceptions,
                                                       ExceptionPriority priority) {
        Objects.requireNonNull(observableExceptions, "observableExceptions cannot be null");
        if (observableExceptions.isEmpty()) {
            throw new IllegalArgumentException("observableExceptions cannot be empty");
        }
        boolean isIgnorableExceptionsNullOrEmpty = ignorableExceptions == null || ignorableExceptions.isEmpty();
        if (ExceptionPriority.IGNORABLE.equals(priority) && isIgnorableExceptionsNullOrEmpty) {
            throw new IllegalArgumentException("ignorableExceptions cannot be null or empty when priority 'IGNORABLE'");
        }
        if (priority == null && !isIgnorableExceptionsNullOrEmpty) {
            throw new IllegalArgumentException("required exceptionPriority when ignorableExceptions not null or not empty");
        }
    }

    /**
     * Resolves conflicts between observable and ignorable sets based on priority.
     * Removes shared exceptions from the lower-priority set.
     *
     * @param observable mutable set of observable exceptions
     * @param ignorable  mutable set of ignorable exceptions
     * @param priority   determines which set keeps the shared exceptions
     */
    private void prepareObservableAndIgnorableExceptions(Set<Class<? extends Throwable>> observable,
                                                         Set<Class<? extends Throwable>> ignorable,
                                                         ExceptionPriority priority) {
        if (priority == null) {
            return;
        }
        if (ExceptionPriority.IGNORABLE.equals(priority)) {
            ignorable.forEach(observable::remove);
        } else if (ExceptionPriority.OBSERVABLE.equals(priority)) {
            observable.forEach(ignorable::remove);
        } else {
            throw new IllegalStateException("unexpected ExceptionPriority '%s'".formatted(priority));
        }
    }

    public String getName() {
        return name;
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

    public boolean isHalfOpenStateEnabled() {
        return isHalfOpenStateEnabled;
    }

    public int getMaxRequestInHalfOpenState() {
        return maxRequestInHalfOpenState;
    }

    public int getMaxExceptionCountInHalfOpenState() {
        return maxExceptionCountInHalfOpenState;
    }

    public Duration getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    @Override
    public String toString() {
        return "CircuitBreakerConfiguration{" +
                "name='" + name + '\'' +
                ", observableExceptions=" + observableExceptions +
                ", ignorableExceptions=" + ignorableExceptions +
                ", lockFree=" + lockFree +
                ", closeState=" + closeState +
                ", waitDurationInOpenState=" + waitDurationInOpenState +
                ", isHalfOpenStateEnabled=" + isHalfOpenStateEnabled +
                ", maxRequestInHalfOpenState=" + maxRequestInHalfOpenState +
                ", maxExceptionCountInHalfOpenState=" + maxExceptionCountInHalfOpenState +
                '}';
    }

    public Builder toBuilder() {
        return new Builder()
                .name(name)
                .observableExceptions(observableExceptions)
                .ignorableExceptions(ignorableExceptions)
                .lockFree(lockFree)
                .closeState(closeState)
                .waitDurationInOpenState(waitDurationInOpenState)
                .halfOpenStateEnabled(isHalfOpenStateEnabled)
                .maxRequestInHalfOpenState(maxRequestInHalfOpenState)
                .maxExceptionCountInHalfOpenState(maxExceptionCountInHalfOpenState);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;
        private Set<Class<? extends Throwable>> observableExceptions;
        private Set<Class<? extends Throwable>> ignorableExceptions;
        private ExceptionPriority exceptionPriority;
        private Boolean lockFree;
        private CloseStateConfiguration closeState;
        private Duration waitDurationInOpenState;
        private Boolean halfOpenStateEnabled;
        private Integer maxRequestInHalfOpenState;
        private Integer maxExceptionCountInHalfOpenState;
        private Double maxExceptionRateInHalfOpenState;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder observableExceptions(Set<Class<? extends Throwable>> observableExceptions) {
            this.observableExceptions = observableExceptions;
            return this;
        }

        public Builder ignorableExceptions(Set<Class<? extends Throwable>> ignorableExceptions) {
            this.ignorableExceptions = ignorableExceptions;
            return this;
        }

        public Builder exceptionPriority(ExceptionPriority priority) {
            this.exceptionPriority = priority;
            return this;
        }

        public Builder lockFree(Boolean lockFree) {
            this.lockFree = lockFree;
            return this;
        }

        Builder closeState(CloseStateConfiguration closeState) {
            this.closeState = closeState;
            return this;
        }

        public Builder closeState(Consumer<CloseStateConfiguration.Builder> builderConsumer) {
            CloseStateConfiguration.Builder builder = CloseStateConfiguration.builder();
            builderConsumer.accept(builder);
            this.closeState = builder.build();
            return this;
        }

        public Builder waitDurationInOpenState(Duration waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
            return this;
        }

        public Builder halfOpenStateEnabled(Boolean halfOpenStateEnabled) {
            this.halfOpenStateEnabled = halfOpenStateEnabled;
            return this;
        }

        public Builder maxRequestInHalfOpenState(Integer maxRequestInHalfOpenState) {
            this.maxRequestInHalfOpenState = maxRequestInHalfOpenState;
            return this;
        }

        public Builder maxExceptionCountInHalfOpenState(Integer maxExceptionCountInHalfOpenState) {
            this.maxExceptionCountInHalfOpenState = maxExceptionCountInHalfOpenState;
            return this;
        }

        public Builder maxExceptionRateInHalfOpenState(Double maxExceptionRateInHalfOpenState) {
            this.maxExceptionRateInHalfOpenState = maxExceptionRateInHalfOpenState;
            return this;
        }

        public CircuitBreakerConfiguration build() {
            return new CircuitBreakerConfiguration(
                    name,
                    observableExceptions,
                    ignorableExceptions,
                    exceptionPriority,
                    lockFree,
                    closeState,
                    halfOpenStateEnabled,
                    maxRequestInHalfOpenState,
                    maxExceptionCountInHalfOpenState,
                    maxExceptionRateInHalfOpenState,
                    waitDurationInOpenState
            );
        }
    }
}
