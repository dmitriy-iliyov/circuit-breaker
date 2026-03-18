package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CountBasedHalfOpenStrategyConcurrentTests {

    public record TestParams(
            int windowSize,
            int threshold,
            long exceptionallyRequestCount,
            long successRequestCount
    ) {
        public static TestParams of(int windowSize, int threshold) {
            long exceptionallyRequestCount = threshold;
            long successRequestCount = windowSize - exceptionallyRequestCount;
            return new TestParams(windowSize, threshold, exceptionallyRequestCount, successRequestCount);
        }
    }

    static Stream<TestParams> testParams() {
        return Stream.of(
                TestParams.of(10, 2),
                TestParams.of(17, 5),
                TestParams.of(37, 10),
                TestParams.of(169, 20)
        );
    }

    static Stream<Function<TestParams, HalfOpenStateStrategy>> strategySuppliers() {
        return Stream.of(
                p -> new CountBasedHalfOpenStrategy(p.windowSize(), p.threshold()),
                p -> new LockFreeCountBasedHalfOpenStrategy(p.windowSize(), p.threshold())
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
    @DisplayName("CT №1: all requests without exceptions concurrently should result in transition TO_CLOSE")
    public void allRequestsWithoutExceptions_concurrently_shouldTransitionToClose(
            TestParams params, Function<TestParams, HalfOpenStateStrategy> strategySupplier
    ) {
        HalfOpenStateStrategy strategy = strategySupplier.apply(params);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < params.windowSize(); i++) {
            tasks.add(strategy::onSuccess);
        }
        runConcurrently(tasks);

        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_CLOSE);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("CT №2: exception count threshold not reached concurrently should result in transition TO_CLOSE")
    public void exceptionCountThresholdNotReached_concurrently_shouldTransitionToClose(
            TestParams params, Function<TestParams, HalfOpenStateStrategy> strategySupplier
    ) {
        HalfOpenStateStrategy strategy = strategySupplier.apply(params);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < params.windowSize() - 1; i++) {
            tasks.add(strategy::onSuccess);
        }
        tasks.add(strategy::onException); // threshold - 1 exceptions, не достигаем порога
        runConcurrently(tasks);

        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_CLOSE);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("CT №3: exception count threshold reached concurrently should result in transition TO_OPEN")
    public void exceptionCountThresholdReached_concurrently_shouldTransitionToOpen(
            TestParams params, Function<TestParams, HalfOpenStateStrategy> strategySupplier
    ) {
        HalfOpenStateStrategy strategy = strategySupplier.apply(params);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < params.successRequestCount(); i++) {
            tasks.add(strategy::onSuccess);
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            tasks.add(strategy::onException);
        }
        runConcurrently(tasks);

        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_OPEN);
    }
}