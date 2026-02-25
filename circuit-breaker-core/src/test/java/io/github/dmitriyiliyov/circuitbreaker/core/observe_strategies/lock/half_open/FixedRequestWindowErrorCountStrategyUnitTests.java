package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FixedRequestWindowErrorCountStrategyUnitTests {

    public record TestParams(
            int windowSize,
            long threshold,
            long exceptionallyRequestCount,
            long successRequestCount,
            Map<Integer, Map<Integer, HalfOpenTransition>> answers
    ) {
        public static TestParams of(int windowSize, long threshold, Map<Integer, Map<Integer, HalfOpenTransition>> answers) {
            long exceptionallyRequestCount = threshold;
            long successRequestCount = windowSize - exceptionallyRequestCount;
            return new TestParams(windowSize, threshold, exceptionallyRequestCount, successRequestCount, answers);
        }
    }

    static Stream<TestParams> testConfig() {
        Map<Integer, Map<Integer, HalfOpenTransition>> commonAnswers = Map.of(
                1, Map.of(1, HalfOpenTransition.TO_CLOSE),
                2, Map.of(1, HalfOpenTransition.TO_CLOSE),
                3, Map.of(1, HalfOpenTransition.TO_OPEN),
                4, Map.of(1, HalfOpenTransition.NO_TRANSITION),
                5, Map.of(1, HalfOpenTransition.TO_OPEN, 2, HalfOpenTransition.TO_OPEN),
                6, Map.of(1, HalfOpenTransition.TO_CLOSE, 2, HalfOpenTransition.NO_TRANSITION, 3, HalfOpenTransition.NO_TRANSITION)
        );
        return Stream.of(
                TestParams.of(10, 2, commonAnswers),
                TestParams.of(17, 5, commonAnswers),
                TestParams.of(37, 10, commonAnswers),
                TestParams.of(169, 20, commonAnswers)
        );
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №1: all requests without exceptions should result in transition TO_CLOSE")
    public void allRequestWithoutExceptions_shouldTransitionToClose(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.getTransition()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №2: exception count threshold not reached should result in transition TO_CLOSE")
    public void exceptionCountThresholdNotReached_shouldTransitionToClose(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.windowSize() - 1; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 1; i++) {
            strategy.onException();
        }
        assertThat(strategy.getTransition()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №3: exception count threshold reached should result in transition TO_OPEN")
    public void exceptionCountThresholdReached_shouldTransitionToOpen(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }
        assertThat(strategy.getTransition()).isEqualTo(params.answers().get(3).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №4: not enough requests should result in NO_TRANSITION")
    public void notEnoughRequests_shouldResultInNoTransition(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.windowSize() - 1; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.getTransition()).isEqualTo(params.answers().get(4).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №5: transition to OPEN should not be overwritten by subsequent successful requests")
    public void transitionToOpen_shouldNotBeOverwrittenBySubsequentRequests(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }

        assertThat(strategy.getTransition()).isEqualTo(params.answers().get(5).get(1));

        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onRequest();
        }

        assertThat(strategy.getTransition()).isEqualTo(params.answers().get(5).get(2));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №6: reset should clear state and transition should be NO_TRANSITION")
    public void reset_shouldClearStateAndTransitionShouldBeNoTransition(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.getTransition()).isEqualTo(params.answers().get(6).get(1));

        strategy.reset();

        assertThat(strategy.getTransition()).isEqualTo(params.answers().get(6).get(2));
        strategy.onRequest();
        assertThat(strategy.getTransition()).isEqualTo(params.answers().get(6).get(3));
    }

    @Test
    @DisplayName("should throw exception for negative window size")
    public void shouldThrowExceptionForNegativeWindowSize() {
        assertThatThrownBy(() -> new FixedRequestWindowErrorCountStrategy(-1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("windowSize cannot be negative");
    }

    @Test
    @DisplayName("should throw exception for negative threshold")
    public void shouldThrowExceptionForNegativeThreshold() {
        assertThatThrownBy(() -> new FixedRequestWindowErrorCountStrategy(10, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionCountThreshold cannot be negative");
    }
}
