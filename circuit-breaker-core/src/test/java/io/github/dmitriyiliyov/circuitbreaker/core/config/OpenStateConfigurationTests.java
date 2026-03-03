package io.github.dmitriyiliyov.circuitbreaker.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OpenStateConfigurationTests {

    @Test
    @DisplayName("should throw exception when neither observeTime nor windowSize is provided")
    public void shouldThrowExceptionWhenNeitherObserveTimeNorWindowSizeIsProvided() {
        assertThatThrownBy(() -> OpenStateConfiguration.builder()
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("either observeTime or windowSize must be provided");
    }

    @Test
    @DisplayName("should throw exception when both observeTime and windowSize are provided")
    public void shouldThrowExceptionWhenBothObserveTimeAndWindowSizeAreProvided() {
        assertThatThrownBy(() -> OpenStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .windowSize(10)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("both observeTime and windowSize cannot be provided simultaneously");
    }

    @Test
    @DisplayName("should create configuration successfully with observeTime")
    public void shouldCreateConfigurationSuccessfullyWithObserveTime() {
        assertThatCode(() -> OpenStateConfiguration.builder()
                .observeTime(Duration.ofSeconds(1))
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should create configuration successfully with windowSize")
    public void shouldCreateConfigurationSuccessfullyWithWindowSize() {
        assertThatCode(() -> OpenStateConfiguration.builder()
                .windowSize(10)
                .build())
                .doesNotThrowAnyException();
    }
}
