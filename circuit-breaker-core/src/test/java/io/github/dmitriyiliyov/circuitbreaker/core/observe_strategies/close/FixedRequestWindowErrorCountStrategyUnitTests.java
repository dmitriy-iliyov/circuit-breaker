package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseObserveStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close.FixedRequestWindowErrorCountStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FixedRequestWindowErrorCountStrategyUnitTests {

    public record TestParams(
            int windowSize,
            int threshold,
            long exceptionallyRequestCount,
            long successRequestCount,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(int windowSize, int threshold, Map<Integer, Map<Integer, Boolean>> answers) {
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

    static Stream<Function<TestParams, CloseObserveStrategy>> strategySuppliers() {
        return Stream.of(
                testParams -> new FixedRequestWindowErrorCountStrategy(
                        testParams.windowSize(), testParams.threshold(), Duration.ZERO
                ),
                testParams -> new io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock_free.close.
                        FixedRequestWindowErrorCountStrategy(testParams.windowSize(), testParams.threshold(), Duration.ZERO)
        );
    }

    static Stream<Arguments> arguments() {
        return testConfig().flatMap(params -> strategySuppliers()
                .map(supplier -> Arguments.of(params, supplier))
        );
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №1: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseObserveStrategy> strategySupplier
    ) {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.windowSize; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №2: exception count threshold not reached should result in shouldTrip being false")
    public void exceptionCountThresholdNotReached_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseObserveStrategy> strategySupplier
    ) {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.windowSize() - 1; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 1; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №3: exception count threshold reached should result in shouldTrip being true")
    public void exceptionCountThresholdReached_shouldTripShouldBeTrue(
            TestParams params, Function<TestParams, CloseObserveStrategy> strategySupplier
    ) {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №4: one success round followed by another success round should result in shouldTrip being false")
    public void oneSuccessRound_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseObserveStrategy> strategySupplier
    ) {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
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
    @MethodSource("arguments")
    @DisplayName("UT №5: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseObserveStrategy> strategySupplier
    ) {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
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
    @MethodSource("arguments")
    @DisplayName("UT №6: window reset on request overflow should clear state and shouldTrip should be false")
    public void windowResetOnRequestOverflow_shouldClearStateAndShouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseObserveStrategy> strategySupplier
    ) {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
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
    @MethodSource("arguments")
    @DisplayName("UT №7: window reset on exception overflow should clear state and shouldTrip should be false")
    public void windowResetOnExceptionOverflow_shouldClearStateAndShouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseObserveStrategy> strategySupplier
    ) {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
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

    @ParameterizedTest
    @MethodSource("strategySuppliers")
    @DisplayName("should throw exception for negative window size")
    public void shouldThrowExceptionForNegativeWindowSize(Function<TestParams, CloseObserveStrategy> strategySupplier) {
        assertThatThrownBy(() -> strategySupplier.apply(new TestParams(-1, 5, 0, 0, Collections.emptyMap())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("windowSize must be > 0");
    }

    @ParameterizedTest
    @MethodSource("strategySuppliers")
    @DisplayName("should throw exception for negative threshold")
    public void shouldThrowExceptionForNegativeThreshold(Function<TestParams, CloseObserveStrategy> strategySupplier) {
        assertThatThrownBy(() -> strategySupplier.apply(new TestParams(10, -1, 0, 0, Collections.emptyMap())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionCountThreshold must be >= 0");
    }
    
    @ParameterizedTest
    @MethodSource("strategySuppliers")
    @DisplayName("should ignore requests before observe start time")
    public void shouldIgnoreRequestsBeforeObserveStartTime(Function<TestParams, CloseObserveStrategy> strategySupplier) throws InterruptedException {
        Duration observeStartTime = Duration.ofMillis(200);
        
        FixedRequestWindowErrorCountStrategy strategy = new FixedRequestWindowErrorCountStrategy(
                10, 1, observeStartTime
        );
        
        strategy.onException();
        assertThat(strategy.shouldTrip()).isFalse();
        
        Thread.sleep(observeStartTime.toMillis() + 50);
        
        strategy.onException();
        assertThat(strategy.shouldTrip()).isTrue();
    }
}
