package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class SlidingWindowCloseStrategyConcurrentTests {

    public record TestParams(
            int windowSize,
            int threshold,
            int exceptionallyRequestCount,
            int successRequestCount
    ) {
        public static TestParams of(int windowSize, double thresholdRate) {
            int exceptionallyRequestCount = (int) Math.ceil(windowSize * thresholdRate);
            if (thresholdRate > 0 && exceptionallyRequestCount == 0) {
                exceptionallyRequestCount = 1;
            }
            int successRequestCount = windowSize - exceptionallyRequestCount;
            return new TestParams(windowSize, exceptionallyRequestCount, exceptionallyRequestCount, successRequestCount);
        }
    }

    static Stream<TestParams> testParams() {
        return Stream.of(
                TestParams.of(10, 0.25),
                TestParams.of(17, 0.2),
                TestParams.of(37, 0.3),
                TestParams.of(169, 0.87)
        );
    }

    static Stream<Function<TestParams, CloseStateStrategy>> strategySuppliers() {
        return Stream.of(
                p -> new SlidingWindowCloseStrategy(p.windowSize(), p.threshold(), Duration.ZERO),
                p -> new LockFreeSlidingWindowCloseStrategy(p.windowSize(), p.threshold(), Duration.ZERO)
        );
    }

    static Stream<Arguments> arguments() {
        return testParams().flatMap(params -> strategySuppliers()
                .map(supplier -> Arguments.of(params, supplier))
        );
    }

    private static void runConcurrently(List<Runnable> tasks) {
        List<CompletableFuture<Void>> futures = tasks.stream()
                .map(CompletableFuture::runAsync)
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("CT №1: all requests without exceptions concurrently should result in shouldTrip being false")
    public void allRequestsWithoutExceptions_concurrently_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseStateStrategy> strategySupplier
    ) {
        CloseStateStrategy strategy = strategySupplier.apply(params);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < params.windowSize(); i++) {
            tasks.add(strategy::onSuccess);
        }
        runConcurrently(tasks);

        assertThat(strategy.shouldTrip()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("CT №2: exception frequency threshold not reached concurrently should result in shouldTrip being false")
    public void exceptionFrequencyThresholdNotReached_concurrently_shouldTripShouldBeFalse(
            TestParams params, Function<TestParams, CloseStateStrategy> strategySupplier
    ) {
        CloseStateStrategy strategy = strategySupplier.apply(params);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < params.successRequestCount(); i++) {
            tasks.add(strategy::onSuccess);
        }
        for (int i = 0; i < params.exceptionallyRequestCount() - 1; i++) {
            tasks.add(strategy::onException);
        }
        runConcurrently(tasks);

        assertThat(strategy.shouldTrip()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("CT №3: exception frequency threshold reached concurrently should result in shouldTrip being true")
    public void exceptionFrequencyThresholdReached_concurrently_shouldTripShouldBeTrue(
            TestParams params, Function<TestParams, CloseStateStrategy> strategySupplier
    ) {
        CloseStateStrategy strategy = strategySupplier.apply(params);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < params.successRequestCount(); i++) {
            tasks.add(strategy::onSuccess);
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            tasks.add(strategy::onException);
        }
        runConcurrently(tasks);

        assertThat(strategy.shouldTrip()).isTrue();
    }
}
