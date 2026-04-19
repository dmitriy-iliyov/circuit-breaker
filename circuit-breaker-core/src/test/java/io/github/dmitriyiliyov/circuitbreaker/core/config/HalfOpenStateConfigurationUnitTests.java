package io.github.dmitriyiliyov.circuitbreaker.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class HalfOpenStateConfigurationUnitTests {

    @Test
    @DisplayName("should create disabled configuration when flag is null")
    public void shouldCreateDisabledConfigurationWhenFlagIsNull() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(null)
                .build();

        assertFalse(config.isHalfOpenStateEnabled());
        assertNull(config.getType());
        assertEquals(0, config.getMaxRequestInHalfOpenState());
        assertEquals(0, config.getMaxExceptionCountInHalfOpenState());
        assertEquals(0.0, config.getMultiplier());
    }

    @Test
    @DisplayName("should create disabled configuration when flag is explicitly false")
    public void shouldCreateDisabledConfigurationWhenFlagIsFalse() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(false)
                .maxRequestInHalfOpenState(10)
                .maxExceptionCountInHalfOpenState(5)
                .type(HalfOpenType.NORMAL)
                .multiplier(5.0)
                .build();

        assertFalse(config.isHalfOpenStateEnabled());
        assertNull(config.getType());
        assertEquals(0, config.getMaxRequestInHalfOpenState());
        assertEquals(0, config.getMaxExceptionCountInHalfOpenState());
        assertEquals(0.0, config.getMultiplier());
    }

    @Test
    @DisplayName("should throw exception when enabled but maxRequestInHalfOpenState is null")
    public void shouldThrowExceptionWhenEnabledButMaxRequestIsNull() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRequestInHalfOpenState cannot be null or == 0 when isHalfOpenStateEnabled == true");
    }

    @Test
    @DisplayName("should throw exception when enabled but maxRequestInHalfOpenState is 0")
    public void shouldThrowExceptionWhenEnabledButMaxRequestIsZero() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRequestInHalfOpenState cannot be null or == 0 when isHalfOpenStateEnabled == true");
    }

    @Test
    @DisplayName("should throw exception when enabled but type is null")
    public void shouldThrowExceptionWhenEnabledButTypeIsNull() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("halfOpenType cannot be null");
    }

    @Test
    @DisplayName("should throw exception when enabled but maxRequestInHalfOpenState is negative")
    public void shouldThrowExceptionWhenEnabledButMaxRequestIsNegative() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(-5)
                .type(HalfOpenType.NORMAL)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRequestInHalfOpenState cannot be < 0");
    }

    @Test
    @DisplayName("should throw exception when enabled but neither count nor rate is provided")
    public void shouldThrowExceptionWhenEnabledButNoCountOrRateProvided() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .type(HalfOpenType.NORMAL)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either maxExceptionCountInHalfOpenState or maxExceptionRateInHalfOpenState must be non null and > 0");
    }

    @Test
    @DisplayName("should throw exception when enabled but both count and rate are zero or negative")
    public void shouldThrowExceptionWhenEnabledButCountAndRateAreInvalid() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .type(HalfOpenType.NORMAL)
                .maxExceptionCountInHalfOpenState(0)
                .maxExceptionRateInHalfOpenState(-0.5)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either maxExceptionCountInHalfOpenState or maxExceptionRateInHalfOpenState must be non null and > 0");
    }

    @Test
    @DisplayName("should prioritize exception count when both count and rate are provided")
    public void shouldPrioritizeCountWhenBothAreProvided() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .type(HalfOpenType.NORMAL)
                .maxExceptionCountInHalfOpenState(4)
                .maxExceptionRateInHalfOpenState(0.9)
                .build();

        assertEquals(4, config.getMaxExceptionCountInHalfOpenState());
    }

    @Test
    @DisplayName("should calculate from rate when count is explicitly negative")
    public void shouldCalculateFromRateWhenCountIsNegative() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .type(HalfOpenType.NORMAL)
                .maxExceptionCountInHalfOpenState(-5)
                .maxExceptionRateInHalfOpenState(0.5)
                .build();

        assertEquals(5, config.getMaxExceptionCountInHalfOpenState());
    }

    @Test
    @DisplayName("should create configuration successfully with exception count and gradual type")
    public void shouldCreateConfigurationSuccessfullyWithCountAndGradual() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .type(HalfOpenType.GRADUAL)
                .maxExceptionCountInHalfOpenState(3)
                .multiplier(3.0)
                .build();

        assertTrue(config.isHalfOpenStateEnabled());
        assertEquals(HalfOpenType.GRADUAL, config.getType());
        assertEquals(10, config.getMaxRequestInHalfOpenState());
        assertEquals(3, config.getMaxExceptionCountInHalfOpenState());
        assertEquals(3.0, config.getMultiplier());
    }

    @Test
    @DisplayName("should create configuration successfully with exception rate and normal type")
    public void shouldCreateConfigurationSuccessfullyWithRateAndNormal() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .type(HalfOpenType.NORMAL)
                .maxExceptionRateInHalfOpenState(0.5)
                .build();

        assertTrue(config.isHalfOpenStateEnabled());
        assertEquals(HalfOpenType.NORMAL, config.getType());
        assertEquals(10, config.getMaxRequestInHalfOpenState());
        assertEquals(5, config.getMaxExceptionCountInHalfOpenState());
        assertEquals(0.0, config.getMultiplier());
    }

    @Test
    @DisplayName("should ceil exception count when calculating from rate")
    public void shouldCeilExceptionCountWhenCalculatingFromRate() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .type(HalfOpenType.NORMAL)
                .maxExceptionRateInHalfOpenState(0.25)
                .build();

        assertEquals(3, config.getMaxExceptionCountInHalfOpenState());
    }

    @Test
    @DisplayName("should not throw exception on valid configuration")
    public void shouldNotThrowExceptionOnValidConfiguration() {
        assertThatCode(() -> HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(20)
                .type(HalfOpenType.NORMAL)
                .maxExceptionCountInHalfOpenState(5)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should set multiplier to 0.0 when type is NORMAL, even if multiplier is provided")
    public void shouldSetMultiplierToZeroWhenTypeIsNormal() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .maxExceptionCountInHalfOpenState(5)
                .type(HalfOpenType.NORMAL)
                .multiplier(5.0)
                .build();

        assertEquals(0.0, config.getMultiplier());
    }

    @Test
    @DisplayName("should use default multiplier when type is GRADUAL and multiplier is negative")
    public void shouldUseDefaultMultiplierWhenTypeIsGradualAndMultiplierIsNegative() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .halfOpenStateEnabled(true)
                .maxRequestInHalfOpenState(10)
                .maxExceptionCountInHalfOpenState(5)
                .type(HalfOpenType.GRADUAL)
                .multiplier(-1.5)
                .build();

        assertEquals(2.0, config.getMultiplier());
    }
}