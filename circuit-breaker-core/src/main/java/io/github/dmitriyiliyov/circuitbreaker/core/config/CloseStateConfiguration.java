package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.time.Duration;
import java.util.Objects;

public final class CloseStateConfiguration {

    private final Integer windowSize;
    private final Double exceptionRateThreshold;
    private final Integer exceptionCountThreshold;
    private final Duration initialDelay;

    private CloseStateConfiguration(Integer windowSize,
                                    Double exceptionRateThreshold,
                                    Integer exceptionCountThreshold,
                                    Duration initialDelay) {
        this.windowSize = Objects.requireNonNull(windowSize, "windowSize cannot be null");
        this.exceptionRateThreshold = exceptionRateThreshold;
        this.exceptionCountThreshold = hasCount(exceptionRateThreshold, exceptionCountThreshold)
                ? exceptionCountThreshold
                : (int) Math.ceil(windowSize * exceptionRateThreshold);
        this.initialDelay = initialDelay == null ? Duration.ZERO : initialDelay;
    }

    private static boolean hasCount(Double exceptionRateThreshold, Integer exceptionCountThreshold) {
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
                "windowSize=" + windowSize +
                ", exceptionRateThreshold=" + exceptionRateThreshold +
                ", exceptionCountThreshold=" + exceptionCountThreshold +
                ", initialDelay=" + initialDelay +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer windowSize;
        private Double exceptionRateThreshold;
        private Integer exceptionCountThreshold;
        private Duration initialDelay;

        public Builder windowSize(Integer windowSize) {
            this.windowSize = Objects.requireNonNull(windowSize, "windowSize cannot be null");
            return this;
        }

        public Builder exceptionRateThreshold(Double exceptionRateThreshold) {
            this.exceptionRateThreshold = Objects.requireNonNull(exceptionRateThreshold, "exceptionRateThreshold cannot be null");
            return this;
        }

        public Builder exceptionCountThreshold(Integer exceptionCountThreshold) {
            this.exceptionCountThreshold = Objects.requireNonNull(exceptionCountThreshold, "exceptionCountThreshold cannot be null");
            return this;
        }

        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = Objects.requireNonNull(initialDelay, "initialDelay cannot be null");
            return this;
        }

        public CloseStateConfiguration build() {
            return new CloseStateConfiguration(
                    windowSize,
                    exceptionRateThreshold,
                    exceptionCountThreshold,
                    initialDelay
            );
        }
    }
}
