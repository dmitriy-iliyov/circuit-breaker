package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

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
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(int windowSize, long threshold, Map<Integer, Map<Integer, Boolean>> answers) {
            long exceptionallyRequestCount = threshold;
            long successRequestCount = windowSize - exceptionallyRequestCount;
            return new TestParams(windowSize, threshold, exceptionallyRequestCount, successRequestCount, answers);
        }
    }

    static Stream<TestParams> testConfig() {
        Map<Integer, Map<Integer, Boolean>> commonAnswersForThresholdGreaterThanOne = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, false),
                3, Map.of(1, true),
                4, Map.of(1, false, 2, false),
                5, Map.of(1, true, 2, false, 3, false),
                6, Map.of(1, true, 2, false, 3, false),
                7, Map.of(1, true, 2, false)
        );
        
        Map<Integer, Map<Integer, Boolean>> answersForThresholdOne = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, true),
                3, Map.of(1, true),
                4, Map.of(1, false, 2, false),
                5, Map.of(1, true, 2, false, 3, true),
                6, Map.of(1, true, 2, false, 3, true),
                7, Map.of(1, true, 2, true)
        );

        return Stream.of(
                TestParams.of(10, 2, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(5, 1, answersForThresholdOne),
                TestParams.of(20, 5, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(100, 10, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(10, 10, commonAnswersForThresholdGreaterThanOne)
        );
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №1: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.windowSize; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №2: exception count threshold not reached should result in shouldTrip being false")
    public void exceptionCountThresholdNotReached_shouldTripShouldBeFalse(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.windowSize() - 1; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 1; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №3: exception count threshold reached should result in shouldTrip being true")
    public void exceptionCountThresholdReached_shouldTripShouldBeTrue(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №4: one success round followed by another success round should result in shouldTrip being false")
    public void oneSuccessRound_shouldTripShouldBeFalse(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(1));

        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(2));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №5: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(1));

        strategy.reset();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(2));

        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(3));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №6: window reset on request overflow should clear state and shouldTrip should be false")
    public void windowResetOnRequestOverflow_shouldClearStateAndShouldTripShouldBeFalse(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(1));

        for (int i = 0; i < Math.max(params.successRequestCount() / 2, 1); i++) {
            strategy.onRequest();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(2));

        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(3));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №7: window reset on exception overflow should clear state and shouldTrip should be false")
    public void windowResetOnExceptionOverflow_shouldClearStateAndShouldTripShouldBeFalse(TestParams params) {
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(7).get(1));

        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(7).get(2));
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
