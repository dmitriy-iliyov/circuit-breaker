package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCircuitBreakerStressTests {

    private static final Logger log = LoggerFactory.getLogger(DefaultCircuitBreakerStressTests.class);

    private static final int STRESS_THREAD_COUNT = 1000;
    private static final int CYCLE_COUNT = 5;
    private static final Duration OPEN_WAIT = Duration.ofMillis(200);
    private static final Duration SLOW_REQUEST_THRESHOLD = Duration.ofMillis(500);

    public record StrategiesConfig(
            String description,
            Supplier<CloseStateStrategy> closeStrategy,
            Supplier<HalfOpenStateStrategy> halfOpenStrategy
    ) {
        @Override
        public String toString() {
            return description;
        }
    }

    static Stream<StrategiesConfig> strategiesConfigs() {
        return Stream.of(
                new StrategiesConfig(
                        "SlidingWindow + CountBased",
                        () -> new SlidingWindowCloseStrategy(50, 10, Duration.ZERO),
                        () -> new CountBasedHalfOpenStrategy(40, 5)
                ),
                new StrategiesConfig(
                        "SlidingWindow + LockFreeCountBased",
                        () -> new SlidingWindowCloseStrategy(50, 10, Duration.ZERO),
                        () -> new LockFreeCountBasedHalfOpenStrategy(40, 5)
                )
        );
    }

    public record CircuitBreakerBundle(
            CircuitBreaker circuitBreaker,
            Supplier<CompletableFuture<Void>> successTask,
            Supplier<CompletableFuture<Void>> failureTask,
            Supplier<CompletableFuture<Void>> slowTask
    ) {}

    private static CircuitBreakerBundle buildBundle(StrategiesConfig config) {
        CircuitBreaker cb = new DefaultCircuitBreaker(
                Set.of(RuntimeException.class, SlowRequestException.class),
                Set.of(ArrayIndexOutOfBoundsException.class)
        );

        RequestTimer timer = new DefaultRequestTimer(SLOW_REQUEST_THRESHOLD);

        HalfOpenState halfOpenState = new HalfOpenState(cb, config.halfOpenStrategy().get(), timer);

        CircuitState openState = new OpenState(
                cb, halfOpenState, new TimeBasedOpenStrategy(OPEN_WAIT)
        );

        CircuitState closeState = new CloseState(
                cb, openState, config.closeStrategy().get(), timer
        );

        ((ConfigurableCircuitBreaker) cb).setState(closeState);
        halfOpenState.setOpenState(openState);
        halfOpenState.setCloseState(closeState);

        Supplier<CompletableFuture<Void>> successTask = () -> CompletableFuture.runAsync(() -> {
            try {
                cb.execute(() -> log.debug("Success"));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });

        Supplier<CompletableFuture<Void>> failureTask = () -> CompletableFuture
                .runAsync(() -> {
                    try {
                        cb.execute(() -> { throw new RuntimeException("failure"); });
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                })
                .exceptionally(ex -> null);

        Supplier<CompletableFuture<Void>> slowTask = () -> CompletableFuture
                .runAsync(() -> {
                    try {
                        cb.execute(() -> Thread.sleep(SLOW_REQUEST_THRESHOLD.toMillis()));
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                })
                .exceptionally(ex -> null);

        return new CircuitBreakerBundle(cb, successTask, failureTask, slowTask);
    }

    private static void runConcurrently(List<Supplier<CompletableFuture<Void>>> tasks) {
        List<CompletableFuture<Void>> futures = tasks.stream()
                .map(Supplier::get)
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    static Stream<Arguments> arguments() {
        return strategiesConfigs().map(config -> Arguments.of(config, config.toString()));
    }

    @ParameterizedTest(name = "ST: [{1}]")
    @MethodSource("arguments")
    @DisplayName("ST: CLOSE -> OPEN under high concurrent load should trip exactly once")
    void stress_highConcurrentFailures_shouldTripToOpen(StrategiesConfig config, String ignored) {
        CircuitBreakerBundle bundle = buildBundle(config);
        CircuitBreaker cb = bundle.circuitBreaker();

        List<Supplier<CompletableFuture<Void>>> tasks = new ArrayList<>();
        for (int i = 0; i < STRESS_THREAD_COUNT - 10; i++) {
            tasks.add(bundle.successTask());
        }
        for (int i = 0; i < 10; i++) {
            tasks.add(bundle.failureTask());
        }
        runConcurrently(tasks);

        assertThat(cb.getState()).isInstanceOf(OpenState.class);
    }

    @ParameterizedTest(name = "ST: [{1}]")
    @MethodSource("arguments")
    @DisplayName("ST: CLOSE -> OPEN -> HALF_OPEN -> CLOSE full cycle under high load should complete successfully")
    void stress_fullCycle_shouldCompleteSuccessfully(StrategiesConfig config, String ignored) throws Throwable {
        CircuitBreakerBundle bundle = buildBundle(config);
        CircuitBreaker cb = bundle.circuitBreaker();

        // CLOSE -> OPEN
        List<Supplier<CompletableFuture<Void>>> closeStateTasks = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            closeStateTasks.add(bundle.successTask());
        }
        for (int i = 0; i < 10; i++) {
            closeStateTasks.add(bundle.failureTask());
        }
        runConcurrently(closeStateTasks);
        assertThat(cb.getState()).isInstanceOf(OpenState.class);

        // OPEN -> HALF_OPEN
        Thread.sleep(OPEN_WAIT.toMillis() + 100);
        cb.execute(() -> log.debug("Probe after open"));
        assertThat(cb.getState()).isInstanceOf(HalfOpenState.class);

        // HALF_OPEN -> CLOSE
        List<Supplier<CompletableFuture<Void>>> halfOpenTasks = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            halfOpenTasks.add(bundle.successTask());
        }
        runConcurrently(halfOpenTasks);
        assertThat(cb.getState()).isInstanceOf(CloseState.class);
    }

    @ParameterizedTest(name = "ST: [{1}]")
    @MethodSource("arguments")
    @DisplayName("ST: CLOSE -> OPEN -> HALF_OPEN -> OPEN -> HALF_OPEN -> CLOSE two cycles under high load should complete successfully")
    void stress_twoCycles_shouldCompleteSuccessfully(StrategiesConfig config, String ignored) throws Throwable {
        CircuitBreakerBundle bundle = buildBundle(config);
        CircuitBreaker cb = bundle.circuitBreaker();

        for (int cycle = 0; cycle < CYCLE_COUNT; cycle++) {
            // CLOSE -> OPEN
            List<Supplier<CompletableFuture<Void>>> closeStateTasks = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                closeStateTasks.add(bundle.successTask());
            }
            for (int i = 0; i < 10; i++) {
                closeStateTasks.add(bundle.failureTask());
            }
            runConcurrently(closeStateTasks);
            assertThat(cb.getState()).isInstanceOf(OpenState.class);

            // OPEN -> HALF_OPEN
            Thread.sleep(OPEN_WAIT.toMillis() + 100);
            final int c = cycle;
            cb.execute(() -> log.debug("Probe after open, cycle {}", c));
            assertThat(cb.getState()).isInstanceOf(HalfOpenState.class);

            if (cycle == 0) {
                // HALF_OPEN -> OPEN
                List<Supplier<CompletableFuture<Void>>> halfOpenFailTasks = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    halfOpenFailTasks.add(bundle.failureTask());
                }
                runConcurrently(halfOpenFailTasks);
                assertThat(cb.getState()).isInstanceOf(OpenState.class);
                Thread.sleep(OPEN_WAIT.toMillis() + 100);
                cb.execute(() -> log.debug("Probe after second open"));
                assertThat(cb.getState()).isInstanceOf(HalfOpenState.class);
            }

            // HALF_OPEN -> CLOSE
            List<Supplier<CompletableFuture<Void>>> halfOpenSuccessTasks = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                halfOpenSuccessTasks.add(bundle.successTask());
            }
            runConcurrently(halfOpenSuccessTasks);
            assertThat(cb.getState()).isInstanceOf(CloseState.class);
        }
    }

    @ParameterizedTest(name = "ST: [{1}]")
    @MethodSource("arguments")
    @DisplayName("ST: slow requests mixed with failures under high load should trip to OPEN")
    void stress_slowAndFailedRequests_shouldTripToOpen(StrategiesConfig config, String ignored) {
        CircuitBreakerBundle bundle = buildBundle(config);
        CircuitBreaker cb = bundle.circuitBreaker();

        List<Supplier<CompletableFuture<Void>>> tasks = new ArrayList<>();
        for (int i = 0; i < STRESS_THREAD_COUNT - 10; i++) {
            tasks.add(bundle.successTask());
        }
        for (int i = 0; i < 5; i++) {
            tasks.add(bundle.failureTask());
        }
        for (int i = 0; i < 5; i++) {
            tasks.add(bundle.slowTask());
        }
        runConcurrently(tasks);

        assertThat(cb.getState()).isInstanceOf(OpenState.class);
    }
}
