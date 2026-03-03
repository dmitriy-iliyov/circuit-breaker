package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.close;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseObserveStrategy;
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

public class FixedTimeWindowErrorCountStrategyUnitTests {

    public record TestParams(
            Duration windowTime,
            int threshold,
            long exceptionallyRequestCount,
            long successRequestCount,
            Duration waitBeforeStartTime,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(Duration windowTime, int threshold, Map<Integer, Map<Integer, Boolean>> answers) {
            long exceptionallyRequestCount = threshold;
            long successRequestCount = 10;
            return new TestParams(windowTime, threshold, exceptionallyRequestCount, successRequestCount, Duration.ZERO, answers);
        }

        public static TestParams of(Duration windowTime, int threshold, Duration waitBeforeStartTime, Map<Integer, Map<Integer, Boolean>> answers) {
            long exceptionallyRequestCount = threshold;
            long successRequestCount = 10;
            return new TestParams(windowTime, threshold, exceptionallyRequestCount, successRequestCount, waitBeforeStartTime, answers);
        }
    }

    static Stream<TestParams> testParams() {
        Map<Integer, Map<Integer, Boolean>> commonAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, false),
                3, Map.of(1, true),
                4, Map.of(1, false, 2, false),
                5, Map.of(1, true, 2, false, 3, true),
                6, Map.of(1, true, 2, false, 3, false)
        );

        Map<Integer, Map<Integer, Boolean>> thresholdOneAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, true),
                3, Map.of(1, true),
                4, Map.of(1, false, 2, false),
                5, Map.of(1, true, 2, true, 3, true),
                6, Map.of(1, true, 2, false, 3, true)
        );

        return Stream.of(
                TestParams.of(Duration.ofMillis(100), 2, commonAnswers),
                TestParams.of(Duration.ofMillis(200), 5, commonAnswers),
                TestParams.of(Duration.ofMillis(50), 1, thresholdOneAnswers)
        );
    }

    static Stream<Function<TestParams, CloseObserveStrategy>> strategySuppliers() {
        return Stream.of(
                testParams -> new FixedTimeWindowErrorCountStrategy(
                        testParams.windowTime(), testParams.threshold(), testParams.waitBeforeStartTime()
                )
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
            TestParams params, Function<TestParams, CloseObserveStrategy> strategySupplier
    ) {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.successRequestCount(); i++) {
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
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        long exceptions = Math.max(1, params.threshold() - 1);
        if (params.threshold() == 1) exceptions = 1;

        for (int i = 0; i < exceptions; i++) {
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
    ) throws InterruptedException {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(1));

        Thread.sleep(params.windowTime().toMillis() + 50);

        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(2));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №5: when time window expired should result in shouldTrip being false")
    public void timeWindowExpired_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseObserveStrategy> strategySupplier
    ) throws InterruptedException {
        CloseObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(1));

        Thread.sleep(params.windowTime().toMillis() + 50);

        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(2));

        if (params.threshold() > 1) {
            for (int i = 0; i < params.exceptionallyRequestCount() - 1; i++) {
                strategy.onException();
            }
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(3));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №6: reset should clear state and shouldTrip should be false")
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
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(1));

        strategy.reset();

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(2));
        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(3));
    }

    @ParameterizedTest
    @MethodSource("strategySuppliers")
    @DisplayName("should throw exception for null duration")
    public void shouldThrowExceptionForNullDuration(Function<TestParams, CloseObserveStrategy> strategySupplier) {
        assertThatThrownBy(() -> strategySupplier.apply(TestParams.of(null, 5, Collections.emptyMap())))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("observeTime cannot be null");
    }

    @ParameterizedTest
    @MethodSource("strategySuppliers")
    @DisplayName("should throw exception for negative threshold")
    public void shouldThrowExceptionForNegativeThreshold(Function<TestParams, CloseObserveStrategy> strategySupplier) {
        assertThatThrownBy(() -> strategySupplier.apply(TestParams.of(Duration.ofSeconds(1), -1, Collections.emptyMap())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionCountThreshold must be >= 0");
    }
    
    @ParameterizedTest
    @MethodSource("strategySuppliers")
    @DisplayName("should ignore requests before observe start time")
    public void shouldIgnoreRequestsBeforeObserveStartTime(Function<TestParams, CloseObserveStrategy> strategySupplier) throws InterruptedException {
        TestParams params = TestParams.of(Duration.ofMillis(100), 1, Duration.ofMillis(200), Collections.emptyMap());
        CloseObserveStrategy strategy = strategySupplier.apply(params);
        
        strategy.onException();
        assertThat(strategy.shouldTrip()).isFalse();
        
        Thread.sleep(params.waitBeforeStartTime().toMillis() + 50);
        
        strategy.onException();
        assertThat(strategy.shouldTrip()).isTrue();
    }
}
