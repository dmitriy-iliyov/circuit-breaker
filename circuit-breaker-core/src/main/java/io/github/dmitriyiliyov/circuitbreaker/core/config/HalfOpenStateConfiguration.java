package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.time.Duration;

public final class HalfOpenStateConfiguration {

    private final Duration observeTime;
    private final Integer windowSize;
    private final Double exceptionRateThreshold;
    private final Integer exceptionCountThreshold;

    private HalfOpenStateConfiguration(Duration observeTime, Integer windowSize, Double exceptionRateThreshold,
                                       Integer exceptionCountThreshold) {
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
    }

    public static Builder builder() {
        return new Builder();
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

    public static class Builder {

        private Duration observeTime;
        private Integer windowSize;
        private Double exceptionRateThreshold;
        private Integer exceptionCountThreshold;

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

        public HalfOpenStateConfiguration build() {
            return new HalfOpenStateConfiguration(
                    observeTime,
                    windowSize,
                    exceptionRateThreshold,
                    exceptionCountThreshold
            );
        }
    }
}
