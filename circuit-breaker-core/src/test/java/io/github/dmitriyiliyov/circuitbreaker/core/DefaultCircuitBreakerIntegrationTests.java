package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

public class DefaultCircuitBreakerIntegrationTests {

    private static final Logger log = LoggerFactory.getLogger(DefaultCircuitBreakerIntegrationTests.class);

    public record CompletableFutureSupplierPair<T>(
            Supplier<CompletableFuture<T>> successSupplier,
            Supplier<CompletableFuture<T>> exceptionSupplier
    ) {}

    private static final CircuitBreaker circuitBreaker = new DefaultCircuitBreaker(
            Set.of(RuntimeException.class, SlowRequestException.class),
            Set.of(ArrayIndexOutOfBoundsException.class)
    );

    private static final Duration maxRequestExeecutionDuration = Duration.ofSeconds(1);

    static {
        RequestTimer timer = new DefaultRequestTimer(maxRequestExeecutionDuration);
        HalfOpenState halfOpenState = new HalfOpenState(
                circuitBreaker,
                new CountBasedHalfOpenStrategy(20, 2),
                timer
        );

        CircuitState openState = new OpenState(
                circuitBreaker,
                halfOpenState,
                new TimeBasedOpenStrategy(Duration.ofMillis(100))
        );

        CircuitState closeState = new CloseState(
                circuitBreaker,
                openState,
                new SlidingWindowCloseStrategy(20, 4, Duration.ZERO),
                timer
        );
        ((ConfigurableCircuitBreaker) circuitBreaker).setState(closeState);

        halfOpenState.setOpenState(openState);
        halfOpenState.setCloseState(closeState);
    }

    private static Stream<?> attributes() {
        return Stream.of(
                new CompletableFutureSupplierPair<>(
                        () -> CompletableFuture.runAsync(() -> {
                            try {
                                circuitBreaker.execute(() -> log.info("Success HTTP request"));
                            } catch (Throwable e) {
                                if (e instanceof CircuitBreakerOpenException) {
                                    log.warn("Request failed: {}", e.getMessage());
                                } else {
                                    throw new RuntimeException(e);
                                }
                            }
                        }),
                        () -> CompletableFuture.runAsync(() -> {
                            try {
                                CheckedRunnable runnable = () -> { throw new RuntimeException("External HTTP error"); };
                                circuitBreaker.execute(runnable);
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }).exceptionally(ex -> {
                            log.warn("Request failed: {}", ex.getMessage());
                            return null;
                        })
                ),
                new CompletableFutureSupplierPair<>(
                        () -> CompletableFuture.runAsync(() -> {
                            try {
                                circuitBreaker.execute(() -> {
                                    log.info("Success HTTP request");
                                    return 1;
                                });
                            } catch (Throwable e) {
                                if (e instanceof CircuitBreakerOpenException) {
                                    log.warn("Request failed: {}", e.getMessage());
                                } else {
                                    throw new RuntimeException(e);
                                }
                            }
                        }),
                        () -> CompletableFuture.runAsync(() -> {
                            try {
                                circuitBreaker.execute(() -> { throw new RuntimeException("External HTTP error"); });
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }).exceptionally(ex -> {
                            log.warn("Request failed: {}", ex.getMessage());
                            return null;
                        })
                )
        );
    }

    @MethodSource("attributes")
    @ParameterizedTest
    @DisplayName("IT: CLOSE -> OPEN -> HALF_OPEN -> CLOSE full cycle should complete successfully")
    public void stateMachine_fullCycleCloseToOpenToHalfOpenToClose_shouldSucceed(CompletableFutureSupplierPair<?> pair) throws Throwable {
        List<CompletableFuture<?>> closeStateFutures = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            closeStateFutures.add(pair.successSupplier().get());
        }
        for (int i = 0; i < 4; i++) {
            closeStateFutures.add(pair.exceptionSupplier().get());
        }
        CompletableFuture.allOf(closeStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        Thread.sleep(Duration.ofSeconds(3).toMillis());
        circuitBreaker.execute(() -> log.info("Success HTTP request"));
        assertThat(circuitBreaker.getState()).isInstanceOf(HalfOpenState.class);

        List<CompletableFuture<?>> halfOpenStateFutures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            halfOpenStateFutures.add(pair.successSupplier().get());
        }
        CompletableFuture.allOf(halfOpenStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(CloseState.class);
    }

    @MethodSource("attributes")
    @ParameterizedTest
    @DisplayName("IT: CLOSE -> OPEN -> HALF_OPEN -> OPEN -> HALF_OPEN -> CLOSE two full cycles should complete successfully")
    public void stateMachine_twoFullCycles_shouldSucceed(CompletableFutureSupplierPair<?> pair) throws Throwable {
        List<CompletableFuture<?>> closeStateFutures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            closeStateFutures.add(pair.exceptionSupplier().get());
        }
        CompletableFuture.allOf(closeStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        Thread.sleep(Duration.ofSeconds(3).toMillis());
        circuitBreaker.execute(() -> log.info("Success HTTP request"));
        assertThat(circuitBreaker.getState()).isInstanceOf(HalfOpenState.class);

        List<CompletableFuture<?>> halfOpenStateFutures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            halfOpenStateFutures.add(pair.exceptionSupplier().get());
        }
        CompletableFuture.allOf(halfOpenStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        Thread.sleep(Duration.ofSeconds(3).toMillis());
        circuitBreaker.execute(() -> log.info("Success HTTP request"));
        assertThat(circuitBreaker.getState()).isInstanceOf(HalfOpenState.class);

        List<CompletableFuture<?>> secondHalfOpenStateFutures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            secondHalfOpenStateFutures.add(pair.successSupplier().get());
        }
        CompletableFuture.allOf(secondHalfOpenStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("IT: CLOSE -> OPEN -> CLOSE without HALF_OPEN state should complete successfully")
    public void stateMachine_fullCycleWithoutHalfOpenState_shouldSucceed() throws Throwable {
        CircuitBreaker circuitBreaker = new DefaultCircuitBreaker(
                Set.of(RuntimeException.class),
                Set.of(ArrayIndexOutOfBoundsException.class)
        );

        RequestTimer timer = new DefaultRequestTimer(Duration.ofSeconds(60));

        CircuitState closeState = new CloseState(
                circuitBreaker,
                new SlidingWindowCloseStrategy(20, 4, Duration.ZERO),
                timer
        );

        ((ConfigurableCircuitBreaker) circuitBreaker).setState(closeState);

        CircuitState openState = new OpenState(
                circuitBreaker,
                closeState,
                new TimeBasedOpenStrategy(Duration.ofMillis(100))
        );

        ((ConfigurableCircuitState) closeState).setNextState(openState);

        List<CompletableFuture<Void>> closeStateFutures = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            closeStateFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    circuitBreaker.execute(() -> log.info("Success HTTP request"));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (int i = 0; i < 4; i++) {
            closeStateFutures.add(
                    CompletableFuture.runAsync(() -> {
                        try {
                            circuitBreaker.execute(() -> { throw new RuntimeException("External HTTP error"); });
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    }).exceptionally(ex -> {
                        log.warn("Request failed: {}", ex.getMessage());
                        return null;
                    })
            );
        }
        CompletableFuture.allOf(closeStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        Thread.sleep(Duration.ofSeconds(3).toMillis());
        circuitBreaker.execute(() -> log.info("Success HTTP request"));
        assertThat(circuitBreaker.getState()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("IT: slow requests should be counted as failures and trigger OPEN state")
    public void stateMachine_slowRequestsCountedAsFailures_shouldTripToOpen() throws Throwable {
        CircuitBreaker circuitBreaker = new DefaultCircuitBreaker(
                Set.of(RuntimeException.class, SlowRequestException.class),
                Set.of(ArrayIndexOutOfBoundsException.class)
        );

        Duration maxRequestExeecutionDuration = Duration.ofSeconds(1);
        RequestTimer timer = new DefaultRequestTimer(maxRequestExeecutionDuration);

        CircuitState closeState = new CloseState(
                circuitBreaker,
                new SlidingWindowCloseStrategy(20, 4, Duration.ZERO),
                timer
        );

        ((ConfigurableCircuitBreaker) circuitBreaker).setState(closeState);

        CircuitState openState = new OpenState(
                circuitBreaker,
                closeState,
                new TimeBasedOpenStrategy(Duration.ofMillis(100))
        );

        ((ConfigurableCircuitState) closeState).setNextState(openState);

        List<CompletableFuture<Void>> closeStateFutures = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            closeStateFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    circuitBreaker.execute(() -> log.info("Success HTTP request"));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (int i = 0; i < 2; i++) {
            closeStateFutures.add(
                    CompletableFuture.runAsync(() -> {
                        try {
                            circuitBreaker.execute(() -> { throw new RuntimeException("External HTTP error"); });
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    }).exceptionally(ex -> {
                        log.warn("Request failed: {}", ex.getMessage());
                        return null;
                    })
            );
        }
        for (int i = 0; i < 2; i++) {
            closeStateFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    circuitBreaker.execute(() -> Thread.sleep(maxRequestExeecutionDuration.toMillis()));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }).exceptionally(ex -> {
                log.warn("Slow request rejected: {}", ex.getMessage());
                return null;
            }));
        }
        CompletableFuture.allOf(closeStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        Thread.sleep(Duration.ofSeconds(3).toMillis());
        circuitBreaker.execute(() -> log.info("Success HTTP request"));
        assertThat(circuitBreaker.getState()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("IT: CLOSE -> OPEN -> GRADUAL_HALF_OPEN -> CLOSE full progressive cycle should succeed under concurrent load")
    public void stateMachine_gradualHalfOpen_shouldProgressivelyRampUpToClose() throws Throwable {
        CircuitBreaker cb = new DefaultCircuitBreaker(Set.of(RuntimeException.class), Set.of());
        RequestTimer timer = new DefaultRequestTimer(Duration.ofSeconds(1));

        HalfOpenStateStrategy strategy = new CountBasedHalfOpenStrategy(2, 1);
        GradualHalfOpenState gradualState = new GradualHalfOpenState(cb, strategy, timer, 2.0);

        CircuitState closeState = new CloseState(
                cb,
                new SlidingWindowCloseStrategy(1, 1, Duration.ZERO),
                timer
        );

        CircuitState openState = new OpenState(
                cb,
                gradualState,
                new TimeBasedOpenStrategy(Duration.ofMillis(100))
        );

        ((ConfigurableCircuitState) closeState).setNextState(openState);
        gradualState.setCloseState(closeState);
        gradualState.setOpenState(openState);
        ((ConfigurableCircuitBreaker) cb).setState(closeState);

        try {
            cb.execute(() -> { throw new RuntimeException("Trip"); });
        } catch (Throwable ignored) {}
        assertThat(cb.getState()).isInstanceOf(OpenState.class);

        Thread.sleep(150);
        cb.execute(() -> "First success to transition");
        assertThat(cb.getState()).isInstanceOf(GradualHalfOpenState.class);

        java.util.concurrent.atomic.AtomicInteger successfulExecutions = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger refusals = new java.util.concurrent.atomic.AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    cb.execute(() -> "Success");
                    successfulExecutions.incrementAndGet();
                } catch (GradualHalfOpenRefuseException e) {
                    refusals.incrementAndGet();
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertThat(cb.getState()).isInstanceOf(CloseState.class);
        assertThat(refusals.get()).isGreaterThan(0);
        assertThat(successfulExecutions.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("IT: GRADUAL_HALF_OPEN should trip back to OPEN if an exception passes the sampler under concurrent load")
    public void stateMachine_gradualHalfOpen_shouldTripBackToOpenOnError() throws Throwable {
        CircuitBreaker cb = new DefaultCircuitBreaker(Set.of(RuntimeException.class), Set.of());
        RequestTimer timer = new DefaultRequestTimer(Duration.ofSeconds(1));

        HalfOpenStateStrategy strategy = new CountBasedHalfOpenStrategy(10, 1);
        GradualHalfOpenState gradualState = new GradualHalfOpenState(cb, strategy, timer, 2.0);

        CircuitState closeState = new CloseState(
                cb,
                new SlidingWindowCloseStrategy(1, 1, Duration.ZERO),
                timer
        );

        CircuitState openState = new OpenState(
                cb,
                gradualState,
                new TimeBasedOpenStrategy(Duration.ofMillis(100))
        );

        ((ConfigurableCircuitState) closeState).setNextState(openState);
        gradualState.setCloseState(closeState);
        gradualState.setOpenState(openState);
        ((ConfigurableCircuitBreaker) cb).setState(closeState);

        try {
            cb.execute(() -> { throw new RuntimeException("Trip"); });
        } catch (Throwable ignored) {}
        assertThat(cb.getState()).isInstanceOf(OpenState.class);

        Thread.sleep(150);
        cb.execute(() -> "First success to transition");
        assertThat(cb.getState()).isInstanceOf(GradualHalfOpenState.class);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    cb.execute(() -> { throw new RuntimeException("Fail"); });
                } catch (RuntimeException ignore) {
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertThat(cb.getState()).isInstanceOf(OpenState.class);
    }
}