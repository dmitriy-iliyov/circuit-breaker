package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class SlidingWindowCloseStrategyUnitTests {

    public record TestParams(
            int windowSize,
            int threshold,
            Duration initialDelay,
            int exceptionallyRequestCount,
            int successRequestCount,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(int windowSize, double threshold, Duration initialDelay, Map<Integer, Map<Integer, Boolean>> answers) {
            // Use ceil to ensure we have enough exceptions to meet or exceed threshold
            int exceptionallyRequestCount = (int) Math.ceil(windowSize * threshold);
            // If threshold > 0 but calculated count is 0 (e.g. very small threshold), ensure at least 1
            if (threshold > 0 && exceptionallyRequestCount == 0) {
                exceptionallyRequestCount = 1;
            }
            
            int successRequestCount = windowSize - exceptionallyRequestCount;
            return new TestParams(windowSize, exceptionallyRequestCount, initialDelay, exceptionallyRequestCount, successRequestCount, answers);
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
                TestParams.of(10, 0.1, Duration.ZERO, sensitiveAnswers),
                TestParams.of(10, 1, Duration.ZERO, standardAnswers),
                TestParams.of(10, 0, Duration.ZERO, answersForThresholdZero),
                TestParams.of(10, 0.25, Duration.ZERO, standardAnswers),
                TestParams.of(17, 0.2, Duration.ZERO, standardAnswers),
                TestParams.of(37, 0.3, Duration.ZERO, standardAnswers),
                TestParams.of(37, 0.31, Duration.ZERO, standardAnswers),
                TestParams.of(169, 0.87, Duration.ZERO, standardAnswers),
                TestParams.of(4123, 0.001, Duration.ZERO, standardAnswers),
                TestParams.of(47, 0.21, Duration.ZERO, standardAnswers)
        );
    }

    static Stream<Function<TestParams, CloseStateStrategy>> strategySuppliers() {
        return Stream.of(
                testParams -> new SlidingWindowCloseStrategy(testParams.windowSize(), testParams.threshold(), testParams.initialDelay()),
                testParams -> new LockFreeSlidingWindowCloseStrategy(testParams.windowSize(), testParams.threshold(), testParams.initialDelay())
        );
    }

    static Stream<Arguments> arguments() {
        return testParams().flatMap(params -> strategySuppliers()
                .map(supplier -> Arguments.of(params, supplier))
        );
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №1: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseStateStrategy> strategySupplier
    ) {
        CloseStateStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.windowSize; i++) {
            strategy.onSuccess();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №2: exceptionSupplier frequency threshold not reached should result in shouldTrip being false")
    public void exceptionFrequencyThresholdNotReached_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseStateStrategy> strategySupplier
    ) {
        CloseStateStrategy strategy = strategySupplier.apply(params);

        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onSuccess();
        }
        for (int i = 0; i < params.exceptionallyRequestCount() - 1; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №3: exceptionSupplier frequency threshold reached should result in shouldTrip being true")
    public void exceptionFrequencyThresholdReached_shouldTripShouldBeTrue(
            TestParams params, Function<TestParams, CloseStateStrategy> strategySupplier
    ) {
        CloseStateStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onSuccess();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №4: one successSupplier round followed by another successSupplier round should result in shouldTrip being false")
    public void oneSuccessRound_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseStateStrategy> strategySupplier
    ) {
        CloseStateStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onSuccess();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(1));

        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onSuccess();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(2));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №5: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseStateStrategy> strategySupplier
    ) {
        CloseStateStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onSuccess();
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
}
