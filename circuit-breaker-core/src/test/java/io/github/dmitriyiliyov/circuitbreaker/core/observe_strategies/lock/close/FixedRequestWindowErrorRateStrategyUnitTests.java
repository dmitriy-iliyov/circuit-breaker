package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class FixedRequestWindowErrorRateStrategyUnitTests {

    public record TestParams(
            int windowSize,
            double threshold,
            int exceptionallyRequestCount,
            int successRequestCount,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(int windowSize, double threshold, Map<Integer, Map<Integer, Boolean>> answers) {
            int exceptionallyRequestCount = (int) (windowSize * threshold);
            int successRequestCount = windowSize - exceptionallyRequestCount;
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
        return Stream.of(
                TestParams.of(
                        10, 0.1,
                        Map.of(
                                1, Map.of(1, false),
                                2, Map.of(1, true),
                                3, Map.of(1, true),
                                4, Map.of(1, false, 2, false),
                                5,  Map.of(1, true, 2, false, 3, true),
                                6,  Map.of(1, true, 2, false, 3, true),
                                7,  Map.of(1, true, 2, true)
                        )
                ),
                TestParams.of(10, 1, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(
                        10, 0,
                        Map.of(
                                1, Map.of(1, false),
                                2, Map.of(1, true),
                                3, Map.of(1, false),
                                4, Map.of(1, false, 2, false),
                                5,  Map.of(1, false, 2, false, 3, true),
                                6,  Map.of(1, false, 2, false, 3, true),
                                7,  Map.of(1, false, 2, true)
                        )
                ),
                TestParams.of(10, 0.25, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(17, 0.2, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(37, 0.3, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(37, 0.31, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(169, 0.87, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(4123, 0.001, commonAnswersForThresholdGreaterThanOne),
                TestParams.of(47, 0.21, commonAnswersForThresholdGreaterThanOne)
        );
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №1: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse(TestParams params) {
        FixedRequestWindowErrorRateStrategy strategy = new FixedRequestWindowErrorRateStrategy(
                params.windowSize(), params.threshold()
        );
        for (int i = 0; i < params.windowSize; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №2: exception frequency threshold not reached should result in shouldTrip being false")
    public void exceptionFrequencyThresholdNotReached_shouldTripShouldBeFalse(TestParams params) {
        FixedRequestWindowErrorRateStrategy strategy = new FixedRequestWindowErrorRateStrategy(
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
    @DisplayName("UT №3: exception frequency threshold reached should result in shouldTrip being true")
    public void exceptionFrequencyThresholdReached_shouldTripShouldBeTrue(TestParams params) {
        FixedRequestWindowErrorRateStrategy strategy = new FixedRequestWindowErrorRateStrategy(
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
        FixedRequestWindowErrorRateStrategy strategy = new FixedRequestWindowErrorRateStrategy(
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
        FixedRequestWindowErrorRateStrategy strategy = new FixedRequestWindowErrorRateStrategy(
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
        FixedRequestWindowErrorRateStrategy strategy = new FixedRequestWindowErrorRateStrategy(
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
        FixedRequestWindowErrorRateStrategy strategy = new FixedRequestWindowErrorRateStrategy(
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
}
