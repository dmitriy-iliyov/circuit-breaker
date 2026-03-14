package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.time.Duration;

public final class CloseStateConfiguration {

    private final Duration observeTime;
    private final Integer windowSize;
    private final Double exceptionRateThreshold;
    private final Integer exceptionCountThreshold;
    private final Duration initialDelay;

    private CloseStateConfiguration(Duration observeTime,
                                    Integer windowSize,
                                    Double exceptionRateThreshold,
                                    Integer exceptionCountThreshold,
                                    Duration initialDelay) {
        if (observeTime == null && windowSize == null) {
            throw new IllegalArgumentException("either observeTime or windowSize must be provided");
        }
        if (observeTime != null && windowSize != null) {
            throw new IllegalArgumentException("both observeTime and windowSize cannot be provided simultaneously");
        }
        this.observeTime = observeTime;
        this.windowSize = windowSize;
        if (this.windowSize != null) {
            this.exceptionRateThreshold = exceptionRateThreshold;
            this.exceptionCountThreshold = isHasCount(exceptionRateThreshold, exceptionCountThreshold)
                    ? exceptionCountThreshold
                    : (int) Math.ceil(windowSize * exceptionRateThreshold);
        } else {
            this.exceptionRateThreshold = exceptionRateThreshold;
            this.exceptionCountThreshold = exceptionCountThreshold;
        }
        this.initialDelay = initialDelay == null ? Duration.ZERO : initialDelay;
    }

    private static boolean isHasCount(Double exceptionRateThreshold, Integer exceptionCountThreshold) {
        boolean hasCount = exceptionCountThreshold != null && exceptionCountThreshold > 0;
        boolean hasRate = exceptionRateThreshold != null && exceptionRateThreshold > 0;
        if (hasCount && hasRate) {
            throw new IllegalArgumentException("both exceptionRateThreshold and exceptionCountThreshold cannot be provided simultaneously");
        }
        if (!hasCount && !hasRate) {
            throw new IllegalArgumentException("either exceptionCountThreshold or exceptionRateThreshold must be non null and >= 0");
        }
        return hasCount;
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

    public Duration getInitialDelay() {
        return initialDelay;
    }

    @Override
    public String toString() {
        return "CloseStateConfiguration{" +
                "observeTime=" + observeTime +
                ", windowSize=" + windowSize +
                ", exceptionRateThreshold=" + exceptionRateThreshold +
                ", exceptionCountThreshold=" + exceptionCountThreshold +
                ", initialDelay=" + initialDelay +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Duration observeTime;
        private Integer windowSize;
        private Double exceptionRateThreshold;
        private Integer exceptionCountThreshold;
        private Duration initialDelay;

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

        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public CloseStateConfiguration build() {
            return new CloseStateConfiguration(
                    observeTime,
                    windowSize,
                    exceptionRateThreshold,
                    exceptionCountThreshold,
                    initialDelay
            );
        }
    }
}
