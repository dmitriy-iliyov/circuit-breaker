package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.time.Duration;
import java.util.Objects;

public final class CloseStateConfiguration {

    private final Integer windowSize;
    private final Integer exceptionCountThreshold;
    private final Duration initialDelay;

    private CloseStateConfiguration(Integer windowSize,
                                    Double exceptionRateThreshold,
                                    Integer exceptionCountThreshold,
                                    Duration initialDelay) {
        this.windowSize = Objects.requireNonNull(windowSize, "windowSize cannot be null");
        this.exceptionCountThreshold = hasCount(exceptionRateThreshold, exceptionCountThreshold)
                ? exceptionCountThreshold
                : (int) Math.ceil(windowSize * exceptionRateThreshold);
        this.initialDelay = initialDelay == null ? Duration.ZERO : initialDelay;
    }

    private static boolean hasCount(Double exceptionRateThreshold, Integer exceptionCountThreshold) {
        boolean hasCount = exceptionCountThreshold != null;
        boolean hasRate = exceptionRateThreshold != null;
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
            Objects.requireNonNull(windowSize, "windowSize cannot be null");
            if (windowSize <= 0) {
                throw new IllegalArgumentException("windowSize cannot be <= 0");
            }
            this.windowSize = windowSize;
            return this;
        }

        public Builder exceptionRateThreshold(Double exceptionRateThreshold) {
            Objects.requireNonNull(exceptionRateThreshold, "exceptionRateThreshold cannot be null");
            if (exceptionRateThreshold < 0) {
                throw new IllegalArgumentException("exceptionRateThreshold cannot be < 0");
            }
            this.exceptionRateThreshold = exceptionRateThreshold;
            return this;
        }

        public Builder exceptionCountThreshold(Integer exceptionCountThreshold) {
            Objects.requireNonNull(exceptionCountThreshold, "exceptionCountThreshold cannot be null");
            if (exceptionCountThreshold < 0) {
                throw new IllegalArgumentException("exceptionCountThreshold cannot be < 0");
            }
            this.exceptionCountThreshold = exceptionCountThreshold;
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
