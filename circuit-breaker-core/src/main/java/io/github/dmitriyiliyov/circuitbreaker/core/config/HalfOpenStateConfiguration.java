package io.github.dmitriyiliyov.circuitbreaker.core.config;

import java.util.Objects;

public final class HalfOpenStateConfiguration {

    private static final double DEFAULT_GRANULAR_MULTIPLIER = 2.0;
    private final boolean isHalfOpenStateEnabled;
    private final HalfOpenType type;
    private final int maxRequestInHalfOpenState;
    private final int maxExceptionCountInHalfOpenState;
    private final double multiplier;

    private HalfOpenStateConfiguration(Boolean isHalfOpenStateEnabled,
                                       HalfOpenType type,
                                       Integer maxRequestInHalfOpenState,
                                       Integer maxExceptionCountInHalfOpenState,
                                       Double maxExceptionRateInHalfOpenState,
                                       Double multiplier) {
        this.isHalfOpenStateEnabled = isHalfOpenStateEnabled != null && isHalfOpenStateEnabled;
        if (this.isHalfOpenStateEnabled && (maxRequestInHalfOpenState == null || maxRequestInHalfOpenState == 0)) {
            throw new IllegalArgumentException("maxRequestInHalfOpenState cannot be null or == 0 when isHalfOpenStateEnabled == true");
        } else if (!this.isHalfOpenStateEnabled) {
            this.type = null;
            this.maxRequestInHalfOpenState = 0;
            this.maxExceptionCountInHalfOpenState = 0;
            this.multiplier = 0.0;
        } else {
            this.type = Objects.requireNonNull(type, "halfOpenType cannot be null");
            if (HalfOpenType.GRADUAL.equals(type)) {
                this.multiplier = multiplier == null || multiplier < 0 ? DEFAULT_GRANULAR_MULTIPLIER : multiplier;
            } else {
                this.multiplier = 0.0;
            }
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

        private Boolean isHalfOpenStateEnabled;
        private HalfOpenType type;
        private Integer maxRequestInHalfOpenState;
        private Integer maxExceptionCountInHalfOpenState;
        private Double maxExceptionRateInHalfOpenState;
        private Double multiplier;

        public Builder halfOpenStateEnabled(Boolean isHalfOpenStateEnabled) {
            this.isHalfOpenStateEnabled = Objects.requireNonNull(isHalfOpenStateEnabled, "isHalfOpenStateEnabled cannot be null");
            return this;
        }

        public Builder type(HalfOpenType type) {
            this.type = Objects.requireNonNull(type, "type cannot be null");;
            return this;
        }

        public Builder maxRequestInHalfOpenState(Integer maxRequestInHalfOpenState) {
            this.maxRequestInHalfOpenState = Objects.requireNonNull(maxRequestInHalfOpenState, "maxRequestInHalfOpenState cannot be null");;
            return this;
        }

        public Builder maxExceptionCountInHalfOpenState(Integer maxExceptionCountInHalfOpenState) {
            this.maxExceptionCountInHalfOpenState = Objects.requireNonNull(maxExceptionCountInHalfOpenState, "maxExceptionCountInHalfOpenState cannot be null");;
            return this;
        }

        public Builder maxExceptionRateInHalfOpenState(Double maxExceptionRateInHalfOpenState) {
            this.maxExceptionRateInHalfOpenState = Objects.requireNonNull(maxExceptionRateInHalfOpenState, "maxExceptionRateInHalfOpenState cannot be null");;
            return this;
        }

        public Builder multiplier(Double multiplier) {
            this.multiplier = Objects.requireNonNull(multiplier, "multiplier cannot be null");;
            return this;
        }

        public HalfOpenStateConfiguration build() {
            return new HalfOpenStateConfiguration(
                    isHalfOpenStateEnabled,
                    type,
                    maxRequestInHalfOpenState,
                    maxExceptionCountInHalfOpenState,
                    maxExceptionRateInHalfOpenState,
                    multiplier
            );
        }
    }
}
