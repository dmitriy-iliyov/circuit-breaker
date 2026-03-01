package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.time.Duration;

public final class OpenStateConfiguration {

    private final Duration observeTime;
    private final Integer windowSize;

    private OpenStateConfiguration(Duration observeTime, Integer windowSize) {
        if (observeTime == null && windowSize == null) {
            throw new IllegalArgumentException("either observeTime or windowSize must be provided");
        }
        if (observeTime != null && windowSize != null) {
            throw new IllegalArgumentException("both observeTime and windowSize cannot be provided simultaneously");
        }
        this.observeTime = observeTime;
        this.windowSize = windowSize;
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

    public static class Builder {

        private Duration observeTime;
        private Integer windowSize;

        public Builder observeTime(Duration observeTime) {
            this.observeTime = observeTime;
            return this;
        }

        public Builder windowSize(Integer windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        public OpenStateConfiguration build() {
            return new OpenStateConfiguration(
                    observeTime,
                    windowSize
            );
        }
    }
}
