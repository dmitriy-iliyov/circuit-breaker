package io.github.dmitriyiliyov.circuitbreaker.core.config;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.SlowRequestException;

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
    private final ExceptionPriority exceptionPriority;
    private final Boolean lockFree;
    private final CloseStateConfiguration closeStateConfiguration;
    private final Duration waitDurationInOpenState;
    private final HalfOpenStateConfiguration halfOpenStateConfiguration;
    private final boolean isRequestTimerEnable;
    private final Duration maxRequestExecutionDuration;

    private CircuitBreakerConfiguration(String name,
                                        Set<Class<? extends Throwable>> observableExceptions,
                                        Set<Class<? extends Throwable>> ignorableExceptions,
                                        ExceptionPriority exceptionPriority,
                                        Boolean lockFree,
                                        CloseStateConfiguration closeStateConfiguration,
                                        HalfOpenStateConfiguration halfOpenStateConfiguration,
                                        Duration waitDurationInOpenState,
                                        Duration maxRequestExecutionDuration) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null, blank or empty");
        }
        this.name = name;

        this.maxRequestExecutionDuration = maxRequestExecutionDuration;
        if (this.maxRequestExecutionDuration == null) {
            this.isRequestTimerEnable = false;
        } else {
            this.isRequestTimerEnable = true;
        }

        // configuring set of observable and ignorable exceptions
        this.exceptionPriority = exceptionPriority;
        checkObservableAndIgnorableExceptions(observableExceptions, ignorableExceptions, exceptionPriority);
        Set<Class<? extends Throwable>> mutableObservable = new HashSet<>(observableExceptions);
        Set<Class<? extends Throwable>> mutableIgnorable = new HashSet<>(
                ignorableExceptions == null ? Collections.emptySet() : ignorableExceptions
        );

        prepareObservableAndIgnorableExceptions(mutableObservable, mutableIgnorable, exceptionPriority);

        if (this.isRequestTimerEnable && !mutableIgnorable.contains(SlowRequestException.class)) {
            mutableObservable.add(SlowRequestException.class);
        }

        this.observableExceptions = Set.copyOf(mutableObservable);
        this.ignorableExceptions = Set.copyOf(mutableIgnorable);

        // configuring states
        this.lockFree = lockFree == null || lockFree;

        if (closeStateConfiguration == null) {
            throw new IllegalArgumentException("closeStateConfiguration cannot be null");
        }
        this.closeStateConfiguration = closeStateConfiguration;

        if (waitDurationInOpenState == null) {
            throw new IllegalArgumentException("waitDurationInOpenState cannot be null");
        }
        this.waitDurationInOpenState = waitDurationInOpenState;

        if (halfOpenStateConfiguration == null) {
            this.halfOpenStateConfiguration = HalfOpenStateConfiguration.builder()
                    .halfOpenStateEnabled(false)
                    .build();
        } else {
            this.halfOpenStateConfiguration = halfOpenStateConfiguration;
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

    public CloseStateConfiguration getCloseStateConfiguration() {
        return closeStateConfiguration;
    }

    public Duration getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    public HalfOpenStateConfiguration getHalfOpenStateConfiguration() {
        return halfOpenStateConfiguration;
    }

    public boolean isRequestTimerEnable() {
        return isRequestTimerEnable;
    }

    public Duration getMaxRequestExecutionDuration() {
        return maxRequestExecutionDuration;
    }

    @Override
    public String toString() {
        return "CircuitBreakerConfiguration{" +
                "name='" + name + '\'' +
                ", observableExceptions=" + observableExceptions +
                ", ignorableExceptions=" + ignorableExceptions +
                ", exceptionPriority=" + exceptionPriority +
                ", lockFree=" + lockFree +
                ", closeStateConfiguration=" + closeStateConfiguration +
                ", waitDurationInOpenState=" + waitDurationInOpenState +
                ", halfOpenStateConfiguration=" + halfOpenStateConfiguration +
                ", isRequestTimerEnable=" + isRequestTimerEnable +
                ", maxRequestExecutionDuration=" + maxRequestExecutionDuration +
                '}';
    }

    public Builder toBuilder() {
        return new Builder(
                name,
                observableExceptions,
                ignorableExceptions,
                exceptionPriority,
                lockFree,
                closeStateConfiguration,
                waitDurationInOpenState,
                halfOpenStateConfiguration,
                maxRequestExecutionDuration
        );
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
        private HalfOpenStateConfiguration halfOpenState;
        private Duration maxRequestExecutionDuration;

        private Builder() {}

        private Builder(String name, Set<Class<? extends Throwable>> observableExceptions, Set<Class<? extends Throwable>> ignorableExceptions, ExceptionPriority exceptionPriority, Boolean lockFree, CloseStateConfiguration closeState, Duration waitDurationInOpenState, HalfOpenStateConfiguration halfOpenState, Duration maxRequestExecutionDuration) {
            this.name = name;
            this.observableExceptions = observableExceptions;
            this.ignorableExceptions = ignorableExceptions;
            this.exceptionPriority = exceptionPriority;
            this.lockFree = lockFree;
            this.closeState = closeState;
            this.waitDurationInOpenState = waitDurationInOpenState;
            this.halfOpenState = halfOpenState;
            this.maxRequestExecutionDuration = maxRequestExecutionDuration;
        }

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            return this;
        }

        public Builder observableExceptions(Set<Class<? extends Throwable>> observableExceptions) {
            this.observableExceptions = Objects.requireNonNull(observableExceptions, "observableExceptions cannot be null");
            return this;
        }

        public Builder ignorableExceptions(Set<Class<? extends Throwable>> ignorableExceptions) {
            this.ignorableExceptions = Objects.requireNonNull(ignorableExceptions, "ignorableExceptions cannot be null");
            return this;
        }

        public Builder exceptionPriority(ExceptionPriority priority) {
            this.exceptionPriority = Objects.requireNonNull(priority, "priority cannot be null");
            return this;
        }

        public Builder lockFree(Boolean lockFree) {
            this.lockFree = Objects.requireNonNull(lockFree, "lockFree cannot be null");
            return this;
        }

        public Builder closeState(Consumer<CloseStateConfiguration.Builder> builderConsumer) {
            Objects.requireNonNull(builderConsumer, "builderConsumer cannot be null");
            CloseStateConfiguration.Builder builder = CloseStateConfiguration.builder();
            builderConsumer.accept(builder);
            this.closeState = builder.build();
            return this;
        }

        public Builder waitDurationInOpenState(Duration waitDurationInOpenState) {
            this.waitDurationInOpenState = Objects.requireNonNull(waitDurationInOpenState, "waitDurationInOpenState cannot be null");
            return this;
        }

        public Builder halfOpenState(Consumer<HalfOpenStateConfiguration.Builder> builderConsumer) {
            Objects.requireNonNull(builderConsumer, "builderConsumer cannot be null");
            HalfOpenStateConfiguration.Builder builder = HalfOpenStateConfiguration.builder();
            builderConsumer.accept(builder);
            this.halfOpenState = builder.build();
            return this;
        }

        public Builder maxRequestExecutionDuration(Duration maxRequestExecutionDuration) {
            this.maxRequestExecutionDuration = Objects.requireNonNull(maxRequestExecutionDuration, "maxRequestExecutionDuration cannot be null");
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
                    halfOpenState,
                    waitDurationInOpenState,
                    maxRequestExecutionDuration
            );
        }
    }
}
