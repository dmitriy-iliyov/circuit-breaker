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

public class TimeBasedOpenStrategyConcurrentTests {

    public record TestParams(Duration windowTime) {
        public static TestParams of(Duration windowTime) {
            return new TestParams(windowTime);
        }
    }

    static Stream<TestParams> testParams() {
        return Stream.of(
                TestParams.of(Duration.ofMillis(100)),
                TestParams.of(Duration.ofMillis(200)),
                TestParams.of(Duration.ofMillis(50))
        );
    }

    static Stream<Function<TestParams, OpenStateStrategy>> strategySuppliers() {
        return Stream.of(
                p -> new TimeBasedOpenStrategy(p.windowTime())
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
    @DisplayName("CT №1: concurrent requests within time window should result in shouldTransition being false")
    public void requestsWithinTimeWindow_concurrently_shouldTransitionShouldBeFalse(
            TestParams params, Function<TestParams, OpenStateStrategy> strategySupplier
    ) {
        OpenStateStrategy strategy = strategySupplier.apply(params);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tasks.add(strategy::onRequest);
        }
        runConcurrently(tasks);

        assertThat(strategy.shouldTransition()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("CT №2: concurrent requests after time window should result in shouldTransition being true")
    public void requestsAfterTimeWindow_concurrently_shouldTransitionShouldBeTrue(
            TestParams params, Function<TestParams, OpenStateStrategy> strategySupplier
    ) throws InterruptedException {
        OpenStateStrategy strategy = strategySupplier.apply(params);
        Thread.sleep(params.windowTime().toMillis() + 50);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tasks.add(strategy::onRequest);
        }
        runConcurrently(tasks);

        assertThat(strategy.shouldTransition()).isTrue();
    }
}
