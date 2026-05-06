package io.github.dmitriyiliyov.circuitbreaker.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class HalfOpenStateConfigurationUnitTests {

    @Test
    @DisplayName("UT should throw exception when maxRequestInHalfOpenState is 0 or negative")
    public void shouldThrowExceptionWhenMaxRequestIsZeroOrNegative() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRequestInHalfOpenState must be > 0 when half-open is enabled");
    }

    @Test
    @DisplayName("UT should throw exception when type is null")
    public void shouldThrowExceptionWhenTypeIsNull() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .type(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type cannot be null");
    }

    @Test
    @DisplayName("should throw exception when maxExceptionCountInHalfOpenState is negative")
    public void shouldThrowExceptionWhenMaxExceptionCountIsNegative() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .maxExceptionCountInHalfOpenState(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxExceptionCountInHalfOpenState cannot be < 0");
    }

    @Test
    @DisplayName("UT should throw exception when maxExceptionRateInHalfOpenState is negative")
    public void shouldThrowExceptionWhenMaxExceptionRateIsNegative() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .maxExceptionRateInHalfOpenState(-0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxExceptionRateInHalfOpenState cannot be < 0");
    }

    @Test
    @DisplayName("UT should throw exception when multiplier is zero or negative")
    public void shouldThrowExceptionWhenMultiplierIsInvalid() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .multiplier(0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("multiplier cannot be <= 0");
    }

    @Test
    @DisplayName("UT should throw exception when neither count nor rate is > 0")
    public void shouldThrowExceptionWhenNoCountOrRateProvided() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(10)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either maxExceptionCountInHalfOpenState or maxExceptionRateInHalfOpenState must be non null and > 0");
    }

    @Test
    @DisplayName("UT should prioritize exception count when both count and rate are provided")
    public void shouldPrioritizeCountWhenBothAreProvided() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(10)
                .maxExceptionCountInHalfOpenState(4)
                .maxExceptionRateInHalfOpenState(0.9)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("is not possible to supply both maxExceptionCountInHalfOpenState and maxExceptionRateInHalfOpenState parameters");
    }

    @Test
    @DisplayName("UT should create configuration successfully with exception count and gradual type")
    public void shouldCreateConfigurationSuccessfullyWithCountAndGradual() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
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
    @DisplayName("UT should create configuration successfully with exception rate and default normal type")
    public void shouldCreateConfigurationSuccessfullyWithRateAndNormal() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(10)
                .maxExceptionRateInHalfOpenState(0.5)
                .build();

        assertTrue(config.isHalfOpenStateEnabled());
        assertEquals(HalfOpenType.NORMAL, config.getType());
        assertEquals(10, config.getMaxRequestInHalfOpenState());
        assertEquals(5, config.getMaxExceptionCountInHalfOpenState());
        assertEquals(0.0, config.getMultiplier());
    }

    @Test
    @DisplayName("UT should ceil exception count when calculating from rate")
    public void shouldCeilExceptionCountWhenCalculatingFromRate() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(10)
                .maxExceptionRateInHalfOpenState(0.25)
                .build();

        assertEquals(3, config.getMaxExceptionCountInHalfOpenState());
    }

    @Test
    @DisplayName("UT should not throw exception on valid configuration")
    public void shouldNotThrowExceptionOnValidConfiguration() {
        assertThatCode(() -> HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(20)
                .maxExceptionCountInHalfOpenState(5)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT should set multiplier to 0.0 when type is NORMAL, even if multiplier is provided")
    public void shouldSetMultiplierToZeroWhenTypeIsNormal() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(10)
                .maxExceptionCountInHalfOpenState(5)
                .type(HalfOpenType.NORMAL)
                .multiplier(5.0)
                .build();

        assertEquals(0.0, config.getMultiplier());
    }

    @Test
    @DisplayName("UT should use default multiplier when type is GRADUAL and multiplier is missing")
    public void shouldUseDefaultMultiplierWhenTypeIsGradualAndMultiplierIsMissing() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(10)
                .maxExceptionCountInHalfOpenState(5)
                .type(HalfOpenType.GRADUAL)
                .build();

        assertEquals(2.0, config.getMultiplier());
    }

    @Test
    @DisplayName("UT should return correct toString representation")
    public void shouldReturnCorrectToStringRepresentation() {
        HalfOpenStateConfiguration config = HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(10)
                .maxExceptionCountInHalfOpenState(5)
                .type(HalfOpenType.NORMAL)
                .build();

        String toString = config.toString();

        assertTrue(toString.startsWith("HalfOpenStateConfiguration{"));
        assertTrue(toString.contains("isHalfOpenStateEnabled=true"));
        assertTrue(toString.contains("type=NORMAL"));
        assertTrue(toString.contains("maxRequestInHalfOpenState=10"));
        assertTrue(toString.contains("maxExceptionCountInHalfOpenState=5"));
        assertTrue(toString.contains("multiplier=0.0"));
        assertTrue(toString.endsWith("}"));
    }

    @Test
    @DisplayName("UT should throw exception when maxRequestInHalfOpenState is explicitly null in builder")
    public void shouldThrowExceptionWhenMaxRequestIsNullInBuilder() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .maxRequestInHalfOpenState(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("maxRequestInHalfOpenState cannot be null");
    }

    @Test
    @DisplayName("UT should throw exception when maxExceptionCountInHalfOpenState is explicitly null in builder")
    public void shouldThrowExceptionWhenMaxExceptionCountIsNullInBuilder() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .maxExceptionCountInHalfOpenState(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("maxExceptionCountInHalfOpenState cannot be null");
    }

    @Test
    @DisplayName("UT should throw exception when maxExceptionRateInHalfOpenState is explicitly null in builder")
    public void shouldThrowExceptionWhenMaxExceptionRateIsNullInBuilder() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .maxExceptionRateInHalfOpenState(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("maxExceptionRateInHalfOpenState cannot be null");
    }

    @Test
    @DisplayName("UT should throw exception when multiplier is explicitly null in builder")
    public void shouldThrowExceptionWhenMultiplierIsNullInBuilder() {
        assertThatThrownBy(() -> HalfOpenStateConfiguration.builder()
                .multiplier(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("multiplier cannot be null");
    }

    @Test
    @DisplayName("UT should cover disabled state branch via reflection since Builder defaults to NORMAL type")
    public void shouldCreateDisabledConfigurationViaReflection() throws Exception {
        java.lang.reflect.Constructor<HalfOpenStateConfiguration> constructor =
                HalfOpenStateConfiguration.class.getDeclaredConstructor(
                        HalfOpenType.class, Integer.class, Integer.class, Double.class, Double.class);

        constructor.setAccessible(true);
        HalfOpenStateConfiguration config = constructor.newInstance(null, null, null, null, null);

        assertFalse(config.isHalfOpenStateEnabled());
        assertNull(config.getType());
        assertEquals(0, config.getMaxRequestInHalfOpenState());
        assertEquals(0, config.getMaxExceptionCountInHalfOpenState());
        assertEquals(0.0, config.getMultiplier());
    }
}