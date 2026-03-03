package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.open;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenObserveStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
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

    static Stream<TestParams> testParams() {
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

    static Stream<Function<TestParams, OpenObserveStrategy>> strategySuppliers() {
        return Stream.of(
                testParams -> new FailFastFixedRequestWindowStrategy(testParams.windowSize()),
                testParams -> new LockFreeFailFastFixedRequestWindowStrategy(testParams.windowSize())
        );
    }

    static Stream<Arguments> arguments() {
        return testParams().flatMap(params -> strategySuppliers()
                .map(supplier -> Arguments.of(params, supplier))
        );
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №1: requests below window size should result in shouldTrip being false")
    public void requestsBelowWindowSize_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, OpenObserveStrategy> strategySupplier
    ) {
        OpenObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.windowSize() - 1; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №2: requests equal to window size should result in shouldTrip being true")
    public void requestsEqualToWindowSize_shouldTripShouldBeTrue(
            TestParams params, Function<TestParams, OpenObserveStrategy> strategySupplier
    ) {
        OpenObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №3: requests above window size should result in shouldTrip being true")
    public void requestsAboveWindowSize_shouldTripShouldBeTrue(
            TestParams params, Function<TestParams, OpenObserveStrategy> strategySupplier
    ) {
        OpenObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.windowSize() + 1; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(1));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("UT №4: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(
            TestParams params, Function<TestParams, OpenObserveStrategy> strategySupplier
    ) {
        OpenObserveStrategy strategy = strategySupplier.apply(params);
        for (int i = 0; i < params.windowSize(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(1));

        strategy.reset();

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(2));
        strategy.onRequest();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(3));
    }

    @ParameterizedTest
    @MethodSource("strategySuppliers")
    @DisplayName("should throw exception for negative window size")
    public void shouldThrowExceptionForNegativeWindowSize(Function<TestParams, OpenObserveStrategy> strategySupplier) {
        assertThatThrownBy(() -> strategySupplier.apply(new TestParams(-1, Collections.emptyMap())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("windowSize must be > 0");
    }
}
