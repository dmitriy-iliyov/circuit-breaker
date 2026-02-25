package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FailFastFixedRequestWindowStrategyUnitTests {

    public record TestParams(
            int windowSize,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(int windowSize, Map<Integer, Map<Integer, Boolean>> answers) {
            return new TestParams(windowSize, answers);
        }
    }

    static Stream<TestParams> testConfig() {
        Map<Integer, Map<Integer, Boolean>> commonAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, true),
                3, Map.of(1, true),
                4, Map.of(1, true, 2, false, 3, false)
        );
        return Stream.of(
                TestParams.of(10, commonAnswers),
                TestParams.of(17, commonAnswers),
                TestParams.of(37, commonAnswers),
                TestParams.of(169, commonAnswers)
        );
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №1: requests below window size should result in shouldTrip being false")
    public void requestsBelowWindowSize_shouldTripShouldBeFalse(TestParams params) {
        FailFastFixedRequestWindowStrategy strategy = new FailFastFixedRequestWindowStrategy(params.windowSize());
        for (int i = 0; i < params.windowSize() - 1; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №2: requests equal to window size should result in shouldTrip being true")
    public void requestsEqualToWindowSize_shouldTripShouldBeTrue(TestParams params) {
        FailFastFixedRequestWindowStrategy strategy = new FailFastFixedRequestWindowStrategy(params.windowSize());
        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №3: requests above window size should result in shouldTrip being true")
    public void requestsAboveWindowSize_shouldTripShouldBeTrue(TestParams params) {
        FailFastFixedRequestWindowStrategy strategy = new FailFastFixedRequestWindowStrategy(params.windowSize());
        for (int i = 0; i < params.windowSize() + 1; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №4: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(TestParams params) {
        FailFastFixedRequestWindowStrategy strategy = new FailFastFixedRequestWindowStrategy(params.windowSize());
        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(1));

        strategy.reset();

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(2));
        strategy.onRequest();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(3));
    }

    @Test
    @DisplayName("should throw exception for negative window size")
    public void shouldThrowExceptionForNegativeWindowSize() {
        assertThatThrownBy(() -> new FailFastFixedRequestWindowStrategy(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("windowSize cannot be negative");
    }
}
