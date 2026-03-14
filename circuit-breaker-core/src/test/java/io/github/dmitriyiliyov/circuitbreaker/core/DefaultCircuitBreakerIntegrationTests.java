package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CountBasedHalfOpenStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.SlidingWindowCloseStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.TimeBasedOpenStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCircuitBreakerIntegrationTests {

    public record CompletableFutureSupplierPair<T> (
            Supplier<CompletableFuture<T>> successSupplier,
            Supplier<CompletableFuture<T>> exceptionSupplier
    ) { }

    private static final CircuitBreaker circuitBreaker = new DefaultCircuitBreaker(
            Set.of(RuntimeException.class),
            Set.of(ArrayIndexOutOfBoundsException.class)
    );

    static {
        HalfOpenState halfOpenState = new HalfOpenState(
                circuitBreaker,
                new CountBasedHalfOpenStrategy(20, 2)
        );

        CircuitState openState = new OpenState(
                circuitBreaker,
                halfOpenState,
                new TimeBasedOpenStrategy(Duration.ofMillis(100))
        );

        CircuitState closeState = new CloseState(
                circuitBreaker,
                openState,
                new SlidingWindowCloseStrategy(20, 4, Duration.ZERO)
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
                                circuitBreaker.execute(() -> System.out.println("Success HTTP request"));
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }),
                        () -> CompletableFuture.runAsync(() -> {
                            try {
                                CheckedRunnable runnable = () -> {throw new RuntimeException("External HTTP error");};
                                circuitBreaker.execute(runnable);
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }).exceptionally(ex -> {
                            System.out.println(ex.getMessage());
                            return null;
                        })
                ),
                new CompletableFutureSupplierPair<>(
                        () -> CompletableFuture.runAsync(() -> {
                            try {
                                circuitBreaker.execute(() -> {
                                    System.out.println("Success HTTP request");
                                    return 1;
                                });
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }),
                        () -> CompletableFuture.runAsync(() -> {
                            try {
                                circuitBreaker.execute(() -> {throw new RuntimeException("External HTTP error");});
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }).exceptionally(ex -> {
                            System.out.println(ex.getMessage());
                            return null;
                        })
                )
        );
    }

    @MethodSource("attributes")
    @ParameterizedTest
    @DisplayName("IT state machine, should successSupplier run")
    public void stateMachine_shouldSuccessRun(CompletableFutureSupplierPair<?> pair) throws Throwable {
        // close state
        List<CompletableFuture<?>> closeStateFutures = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            closeStateFutures.add(pair.successSupplier().get());
        }
        for (int i = 0; i < 4; i++) {
            closeStateFutures.add(pair.exceptionSupplier().get());
        }
        CompletableFuture.allOf(closeStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        // open state
        Thread.sleep(Duration.ofSeconds(1).toMillis());
        circuitBreaker.execute(() -> System.out.println("Success HTTP request"));
        assertThat(circuitBreaker.getState()).isInstanceOf(HalfOpenState.class);

        // half open state
        List<CompletableFuture<?>> halfOpenStateFutures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            halfOpenStateFutures.add(pair.successSupplier().get());
        }
        CompletableFuture.allOf(halfOpenStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(CloseState.class);
    }

    @MethodSource("attributes")
    @ParameterizedTest
    @DisplayName("IT state machine, should successSupplier run")
    public void stateMachineWithSecondRound_shouldSuccessRun(CompletableFutureSupplierPair<?> pair) throws Throwable {
        // close state
        List<CompletableFuture<?>> closeStateFutures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            closeStateFutures.add(pair.exceptionSupplier().get());
        }
        CompletableFuture.allOf(closeStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        // open state
        Thread.sleep(Duration.ofSeconds(1).toMillis());
        circuitBreaker.execute(() -> System.out.println("Success HTTP request"));
        assertThat(circuitBreaker.getState()).isInstanceOf(HalfOpenState.class);

        // half open state
        List<CompletableFuture<?>> halfOpenStateFutures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            halfOpenStateFutures.add(pair.exceptionSupplier().get());
        }
        CompletableFuture.allOf(halfOpenStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        // second open state
        Thread.sleep(Duration.ofSeconds(1).toMillis());
        circuitBreaker.execute(() -> System.out.println("Success HTTP request"));
        assertThat(circuitBreaker.getState()).isInstanceOf(HalfOpenState.class);

        // second half open state
        List<CompletableFuture<?>> secondHalfOpenStateFutures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            secondHalfOpenStateFutures.add(pair.successSupplier().get());
        }
        CompletableFuture.allOf(secondHalfOpenStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("IT state machine, should successSupplier run")
    public void stateMachineWithoutHalfOpen_shouldSuccessRun() throws Throwable {

        CircuitBreaker circuitBreaker = new DefaultCircuitBreaker(
                Set.of(RuntimeException.class),
                Set.of(ArrayIndexOutOfBoundsException.class)
        );

        CircuitState closeState = new CloseState(
                circuitBreaker,
                new SlidingWindowCloseStrategy(20, 4, Duration.ZERO)
        );

        ((ConfigurableCircuitBreaker) circuitBreaker).setState(closeState);


        CircuitState openState = new OpenState(
                circuitBreaker,
                closeState,
                new TimeBasedOpenStrategy(Duration.ofMillis(100))
        );

        ((ConfigurableCircuitState) closeState).setNextState(openState);

        // close state
        List<CompletableFuture<Void>> closeStateFutures = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            closeStateFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    circuitBreaker.execute(() -> System.out.println("Success HTTP request"));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (int i = 0; i < 4; i++) {
            closeStateFutures.add(
                    CompletableFuture.runAsync(() -> {
                                try {
                                    circuitBreaker.execute(() -> {
                                        throw new RuntimeException("External HTTP error");
                                    });
                                } catch (Throwable e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .exceptionally(ex -> {
                                System.out.println(ex.getMessage());
                                return null;
                            })
            );
        }
        CompletableFuture.allOf(closeStateFutures.toArray(new CompletableFuture[0])).join();
        assertThat(circuitBreaker.getState()).isInstanceOf(OpenState.class);

        // open state
        Thread.sleep(Duration.ofSeconds(1).toMillis());
        circuitBreaker.execute(() -> System.out.println("Success HTTP request"));
        assertThat(circuitBreaker.getState()).isInstanceOf(CloseState.class);
    }
}
