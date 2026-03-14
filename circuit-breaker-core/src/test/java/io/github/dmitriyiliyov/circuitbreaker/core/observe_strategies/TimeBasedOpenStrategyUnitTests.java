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

public class TimeBasedOpenStrategyUnitTests {

    public record TestParams(
            Duration windowTime,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(Duration windowTime, Map<Integer, Map<Integer, Boolean>> answers) {
            return new TestParams(windowTime, answers);
        }
    }

    static Stream<TestParams> testParams() {
        Map<Integer, Map<Integer, Boolean>> commonAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, true),
                3, Map.of(1, true, 2, false, 3, false)
        );

        return Stream.of(
                TestParams.of(Duration.ofMillis(100), commonAnswers),
                TestParams.of(Duration.ofMillis(200), commonAnswers),
                TestParams.of(Duration.ofMillis(50), commonAnswers)
        );
    }

    static Stream<Function<TestParams, OpenStateStrategy>> strategySuppliers() {
        return Stream.of(
                testParams -> new TimeBasedOpenStrategy(testParams.windowTime())
        );
    }

    static Stream<Arguments> arguments() {
        return testParams().flatMap(
                params -> strategySuppliers()
                        .map(supplier -> Arguments.of(params, supplier))
        );
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №1: requests within time window should result in shouldTrip being false")
    public void requestsWithinTimeWindow_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, OpenStateStrategy> strategySupplier
    ) {
        OpenStateStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTransition()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №2: requests after time window should result in shouldTrip being true")
    public void requestsAfterTimeWindow_shouldTripShouldBeTrue(
            TestParams params, Function<TestParams, OpenStateStrategy> strategySupplier
    ) throws InterruptedException {
        OpenStateStrategy strategy = strategySupplier.apply(params);
        Thread.sleep(params.windowTime().toMillis() + 50);

        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTransition()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №3: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(
            TestParams params, Function<TestParams, OpenStateStrategy> strategySupplier
    ) throws InterruptedException {
        OpenStateStrategy strategy = strategySupplier.apply(params);
        Thread.sleep(params.windowTime().toMillis() + 50);
        strategy.onRequest();
        assertThat(strategy.shouldTransition()).isEqualTo(params.answers().get(3).get(1));

        strategy.reset();

        assertThat(strategy.shouldTransition()).isEqualTo(params.answers().get(3).get(2));
        strategy.onRequest();
        assertThat(strategy.shouldTransition()).isEqualTo(params.answers().get(3).get(3));
    }
}
