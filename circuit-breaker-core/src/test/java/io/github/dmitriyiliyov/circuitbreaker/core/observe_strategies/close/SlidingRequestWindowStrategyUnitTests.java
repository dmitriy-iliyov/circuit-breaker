package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.close;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SlidingRequestWindowStrategyUnitTests {

    public record TestParams(
            int windowSize,
            double threshold,
            int exceptionallyRequestCount,
            int successRequestCount,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(int windowSize, double threshold, Map<Integer, Map<Integer, Boolean>> answers) {
            // Use ceil to ensure we have enough exceptions to meet or exceed threshold
            int exceptionallyRequestCount = (int) Math.ceil(windowSize * threshold);
            // If threshold > 0 but calculated count is 0 (e.g. very small threshold), ensure at least 1
            if (threshold > 0 && exceptionallyRequestCount == 0) {
                exceptionallyRequestCount = 1;
            }
            
            int successRequestCount = windowSize - exceptionallyRequestCount;
            return new TestParams(windowSize, threshold, exceptionallyRequestCount, successRequestCount, answers);
        }
    }

    static Stream<TestParams> testParams() {
        Map<Integer, Map<Integer, Boolean>> standardAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, false),
                3, Map.of(1, true),
                4, Map.of(1, false, 2, false),
                5, Map.of(1, true, 2, false, 3, false)
        );
        
        Map<Integer, Map<Integer, Boolean>> sensitiveAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, false),
                3, Map.of(1, true),
                4, Map.of(1, false, 2, false),
                5, Map.of(1, true, 2, false, 3, true)
        );

        Map<Integer, Map<Integer, Boolean>> answersForThresholdZero = Map.of(
                1, Map.of(1, true),
                2, Map.of(1, true),
                3, Map.of(1, true),
                4, Map.of(1, true, 2, true),
                5, Map.of(1, true, 2, false, 3, true)
        );

        return Stream.of(
                TestParams.of(10, 0.1, sensitiveAnswers),
                TestParams.of(10, 1, standardAnswers),
                TestParams.of(10, 0, answersForThresholdZero),
                TestParams.of(10, 0.25, standardAnswers),
                TestParams.of(17, 0.2, standardAnswers),
                TestParams.of(37, 0.3, standardAnswers),
                TestParams.of(37, 0.31, standardAnswers),
                TestParams.of(169, 0.87, standardAnswers),
                TestParams.of(4123, 0.001, standardAnswers),
                TestParams.of(47, 0.21, standardAnswers)
        );
    }

    @ParameterizedTest
    @MethodSource("testParams")
    @DisplayName("UT №1: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse(TestParams params) {
        SlidingRequestWindowStrategy strategy = new SlidingRequestWindowStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.windowSize; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testParams")
    @DisplayName("UT №2: exception frequency threshold not reached should result in shouldTrip being false")
    public void exceptionFrequencyThresholdNotReached_shouldTripShouldBeFalse(TestParams params) {
        SlidingRequestWindowStrategy strategy = new SlidingRequestWindowStrategy(
                params.windowSize(), params.threshold()
        );
        
        int exceptions = 0;
        if (params.threshold() > 0) {
             double limit = params.windowSize() * params.threshold();
             int maxExceptions = (int) Math.ceil(limit) - 1;
             exceptions = Math.max(0, maxExceptions);
        }
        
        for (int i = 0; i < params.windowSize() - exceptions; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < exceptions; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("testParams")
    @DisplayName("UT №3: exception frequency threshold reached should result in shouldTrip being true")
    public void exceptionFrequencyThresholdReached_shouldTripShouldBeTrue(TestParams params) {
        SlidingRequestWindowStrategy strategy = new SlidingRequestWindowStrategy(
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
    @MethodSource("testParams")
    @DisplayName("UT №4: one success round followed by another success round should result in shouldTrip being false")
    public void oneSuccessRound_shouldTripShouldBeFalse(TestParams params) {
        SlidingRequestWindowStrategy strategy = new SlidingRequestWindowStrategy(
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
    @MethodSource("testParams")
    @DisplayName("UT №5: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(TestParams params) {
        SlidingRequestWindowStrategy strategy = new SlidingRequestWindowStrategy(
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

    @Test
    @DisplayName("should throw exception for negative window size")
    public void shouldThrowExceptionForNegativeWindowSize() {
        assertThatThrownBy(() -> new SlidingRequestWindowStrategy(-1, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("windowSize must be > 0");
    }

    @Test
    @DisplayName("should throw exception for negative threshold")
    public void shouldThrowExceptionForNegativeThreshold() {
        assertThatThrownBy(() -> new SlidingRequestWindowStrategy(10, -0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionRateThreshold must be >= 0");
    }
}
