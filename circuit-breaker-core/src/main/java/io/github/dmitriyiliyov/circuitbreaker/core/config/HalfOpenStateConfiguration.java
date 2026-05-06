package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.util.Objects;

public final class HalfOpenStateConfiguration {

    private static final double DEFAULT_GRANULAR_MULTIPLIER = 2.0;
    private final boolean isHalfOpenStateEnabled;
    private final HalfOpenType type;
    private final int maxRequestInHalfOpenState;
    private final int maxExceptionCountInHalfOpenState;
    private final double multiplier;

    private HalfOpenStateConfiguration(HalfOpenType type,
                                       Integer maxRequestInHalfOpenState,
                                       Integer maxExceptionCountInHalfOpenState,
                                       Double maxExceptionRateInHalfOpenState,
                                       Double multiplier) {
        if(isDisabled(type, maxRequestInHalfOpenState, maxExceptionCountInHalfOpenState, maxExceptionRateInHalfOpenState)) {
            this.isHalfOpenStateEnabled = false;
            this.type = null;
            this.maxRequestInHalfOpenState = 0;
            this.maxExceptionCountInHalfOpenState = 0;
            this.multiplier = 0.0;
        } else {
            this.isHalfOpenStateEnabled = true;
            this.type = type == null ? HalfOpenType.NORMAL : type;

            this.maxRequestInHalfOpenState = Objects.requireNonNull(maxRequestInHalfOpenState, "maxRequestInHalfOpenState cannot be null");;

            boolean hasCount = maxExceptionCountInHalfOpenState != null;
            boolean hasRate = maxExceptionRateInHalfOpenState != null;
            if (hasCount && hasRate) {
                throw new IllegalArgumentException("is not possible to supply both maxExceptionCountInHalfOpenState and maxExceptionRateInHalfOpenState parameters");
            }
            if (!hasCount && !hasRate) {
                throw new IllegalArgumentException("either maxExceptionCountInHalfOpenState or maxExceptionRateInHalfOpenState must be non null and > 0");
            }
            this.maxExceptionCountInHalfOpenState = hasCount
                    ? maxExceptionCountInHalfOpenState
                    : (int) Math.ceil(maxRequestInHalfOpenState * maxExceptionRateInHalfOpenState);

            if (HalfOpenType.GRADUAL.equals(this.type)) {
                this.multiplier = multiplier == null ? DEFAULT_GRANULAR_MULTIPLIER : multiplier;
            } else {
                this.multiplier = 0.0;
            }
        }
    }

    private boolean isDisabled(HalfOpenType type,
                               Integer maxRequestInHalfOpenState,
                               Integer maxExceptionCountInHalfOpenState,
                               Double maxExceptionRateInHalfOpenState) {
        return type == null &&
                maxRequestInHalfOpenState == null &&
                maxExceptionCountInHalfOpenState == null &&
                maxExceptionRateInHalfOpenState == null;
    }

    public boolean isHalfOpenStateEnabled() {
        return isHalfOpenStateEnabled;
    }

    public HalfOpenType getType() {
        return type;
    }

    public int getMaxRequestInHalfOpenState() {
        return maxRequestInHalfOpenState;
    }

    public int getMaxExceptionCountInHalfOpenState() {
        return maxExceptionCountInHalfOpenState;
    }

    public double getMultiplier() {
        return multiplier;
    }

    @Override
    public String toString() {
        return "HalfOpenStateConfiguration{" +
                "isHalfOpenStateEnabled=" + isHalfOpenStateEnabled +
                ", type=" + type +
                ", maxRequestInHalfOpenState=" + maxRequestInHalfOpenState +
                ", maxExceptionCountInHalfOpenState=" + maxExceptionCountInHalfOpenState +
                ", multiplier=" + multiplier +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private HalfOpenType type;
        private Integer maxRequestInHalfOpenState;
        private Integer maxExceptionCountInHalfOpenState;
        private Double maxExceptionRateInHalfOpenState;
        private Double multiplier;

        public Builder type(HalfOpenType type) {
            this.type = Objects.requireNonNull(type, "type cannot be null");
            return this;
        }

        public Builder maxRequestInHalfOpenState(Integer maxRequestInHalfOpenState) {
            Objects.requireNonNull(maxRequestInHalfOpenState, "maxRequestInHalfOpenState cannot be null");
            if (maxRequestInHalfOpenState <= 0) {
                throw new IllegalArgumentException("maxRequestInHalfOpenState must be > 0 when half-open is enabled");
            }
            this.maxRequestInHalfOpenState = maxRequestInHalfOpenState;
            return this;
        }

        public Builder maxExceptionCountInHalfOpenState(Integer maxExceptionCountInHalfOpenState) {
            Objects.requireNonNull(maxExceptionCountInHalfOpenState, "maxExceptionCountInHalfOpenState cannot be null");
            if (maxExceptionCountInHalfOpenState < 0) {
                throw new IllegalArgumentException("maxExceptionCountInHalfOpenState cannot be < 0");
            }
            this.maxExceptionCountInHalfOpenState = maxExceptionCountInHalfOpenState;
            return this;
        }

        public Builder maxExceptionRateInHalfOpenState(Double maxExceptionRateInHalfOpenState) {
            Objects.requireNonNull(maxExceptionRateInHalfOpenState, "maxExceptionRateInHalfOpenState cannot be null");
            if (maxExceptionRateInHalfOpenState < 0) {
                throw new IllegalArgumentException("maxExceptionRateInHalfOpenState cannot be < 0");
            }
            this.maxExceptionRateInHalfOpenState = maxExceptionRateInHalfOpenState;
            return this;
        }

        public Builder multiplier(Double multiplier) {
            Objects.requireNonNull(multiplier, "multiplier cannot be null");
            if (multiplier <= 0) {
                throw new IllegalArgumentException("multiplier cannot be <= 0");
            }
            this.multiplier = multiplier;
            return this;
        }

        public HalfOpenStateConfiguration build() {
            return new HalfOpenStateConfiguration(
                    type,
                    maxRequestInHalfOpenState,
                    maxExceptionCountInHalfOpenState,
                    maxExceptionRateInHalfOpenState,
                    multiplier
            );
        }
    }
}
