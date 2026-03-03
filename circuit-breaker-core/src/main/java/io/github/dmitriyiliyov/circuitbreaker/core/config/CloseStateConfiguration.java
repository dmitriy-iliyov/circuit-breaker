package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.time.Duration;
import java.util.Objects;

public final class CloseStateConfiguration {

    private final WindowType windowType;
    private final Duration observeTime;
    private final Integer windowSize;
    private final Double exceptionRateThreshold;
    private final Integer exceptionCountThreshold;
    private final Duration waitBeforeStartTime;

    private CloseStateConfiguration(WindowType windowType, Duration observeTime, Integer windowSize,
                                    Double exceptionRateThreshold, Integer exceptionCountThreshold,
                                    Duration waitBeforeStartTime) {
        this.windowType = Objects.requireNonNull(windowType, "windowType cannot be null");
        if (observeTime == null && windowSize == null) {
            throw new IllegalArgumentException("either observeTime or windowSize must be provided");
        }
        if (observeTime != null && windowSize != null) {
            throw new IllegalArgumentException("both observeTime and windowSize cannot be provided simultaneously");
        }
        this.observeTime = observeTime;
        this.windowSize = windowSize;
        if (exceptionRateThreshold == null && exceptionCountThreshold == null) {
            throw new IllegalArgumentException("either exceptionRateThreshold or exceptionCountThreshold must be provided");
        }
        if (exceptionRateThreshold != null && exceptionCountThreshold != null) {
            throw new IllegalArgumentException("both exceptionRateThreshold and exceptionCountThreshold cannot be provided simultaneously");
        }
        this.exceptionRateThreshold = exceptionRateThreshold;
        this.exceptionCountThreshold = exceptionCountThreshold;
        if (waitBeforeStartTime == null && WindowType.FIXED.equals(windowType)) {
            throw new IllegalArgumentException("waitBeforeStartTime cannot be null");
        }
        this.waitBeforeStartTime = waitBeforeStartTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public WindowType getWindowType() {
        return windowType;
    }

    public Duration getObserveTime() {
        return observeTime;
    }

    public Integer getWindowSize() {
        return windowSize;
    }

    public Double getExceptionRateThreshold() {
        return exceptionRateThreshold;
    }

    public Integer getExceptionCountThreshold() {
        return exceptionCountThreshold;
    }

    public Duration getWaitBeforeStartTime() {
        return waitBeforeStartTime;
    }

    public static class Builder {

        private WindowType windowType;
        private Duration observeTime;
        private Integer windowSize;
        private Double exceptionRateThreshold;
        private Integer exceptionCountThreshold;
        private Duration waitBeforeStartTime;

        public Builder windowMoveType(WindowType windowType) {
            this.windowType = windowType;
            return this;
        }

        public Builder observeTime(Duration observeTime) {
            this.observeTime = observeTime;
            return this;
        }

        public Builder windowSize(Integer windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        public Builder exceptionRateThreshold(Double exceptionRateThreshold) {
            this.exceptionRateThreshold = exceptionRateThreshold;
            return this;
        }

        public Builder exceptionCountThreshold(Integer exceptionCountThreshold) {
            this.exceptionCountThreshold = exceptionCountThreshold;
            return this;
        }

        public Builder observeStartTime(Duration observeStartTime) {
            this.waitBeforeStartTime = observeStartTime;
            return this;
        }

        public CloseStateConfiguration build() {
            return new CloseStateConfiguration(
                    windowType,
                    observeTime,
                    windowSize,
                    exceptionRateThreshold,
                    exceptionCountThreshold,
                    waitBeforeStartTime
            );
        }
    }
}
