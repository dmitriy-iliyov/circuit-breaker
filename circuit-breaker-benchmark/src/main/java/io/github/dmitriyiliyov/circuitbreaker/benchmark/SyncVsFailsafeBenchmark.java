package io.github.dmitriyiliyov.circuitbreaker.benchmark;

import dev.failsafe.Failsafe;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerRegistry;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
@Threads(8)
public class SyncVsFailsafeBenchmark {

    private static final CircuitBreakerFactory FACTORY = new DefaultCircuitBreakerFactory(
            new DefaultCircuitBreakerRegistry(),
            new DefaultStrategiesProvider(List.of(
                    new SlidingWindowCloseStrategyProvider(),
                    new LockFreeSlidingWindowCloseStrategyProvider(),
                    new TimeBasedOpenStrategyProvider(),
                    new CountBasedHalfOpenStrategyProvider(),
                    new LockFreeCountBasedHalfOpenStrategyProvider())
            )
    );

    @State(Scope.Benchmark)
    public static class ClosedState {
        CircuitBreaker myLibSync;
        dev.failsafe.CircuitBreaker<Object> failsafeCb;

        @Setup(Level.Trial)
        public void setup() {
            myLibSync = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_sync_closed")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(false)
                    .closeState(c -> c.windowSize(1000).exceptionRateThreshold(0.5))
                    .waitDurationInOpenState(Duration.ofMillis(1))
                    .build());

            failsafeCb = dev.failsafe.CircuitBreaker.builder()
                    .handle(RuntimeException.class)
                    .withFailureThreshold(500, 1000)
                    .build();
        }
    }

    @Benchmark
    public void testClosed_myLibSync(ClosedState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibSync, () -> {
            int sum = 0;
            for (int i = 0; i < 100; i++) {
                sum += i;
            }
            return "ok" + sum;
        }));
    }

    @Benchmark
    public void testClosed_failsafe(ClosedState state, Blackhole bh) {
        bh.consume(executeFailsafe(state.failsafeCb, () -> {
            int sum = 0;
            for (int i = 0; i < 100; i++) {
                sum += i;
            }
            return "ok" + sum;
        }));
    }

    @State(Scope.Benchmark)
    public static class OpenState {
        CircuitBreaker myLibSync;
        dev.failsafe.CircuitBreaker<Object> failsafeCb;

        @Setup(Level.Trial)
        public void setup() {
            myLibSync = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_sync_open")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(false)
                    .closeState(c -> c.windowSize(2).exceptionRateThreshold(0.1))
                    .waitDurationInOpenState(Duration.ofHours(1))
                    .build());

            failsafeCb = dev.failsafe.CircuitBreaker.builder()
                    .handle(RuntimeException.class)
                    .withFailureThreshold(1)
                    .withDelay(Duration.ofHours(1))
                    .build();

            try { myLibSync.execute(() -> { throw new RuntimeException(); }); } catch (Throwable ignored) {}
            failsafeCb.open();
        }
    }

    @Benchmark
    public void testOpen_myLibSync(OpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibSync, () -> "should_fail"));
    }

    @Benchmark
    public void testOpen_failsafe(OpenState state, Blackhole bh) {
        bh.consume(executeFailsafe(state.failsafeCb, () -> "should_fail"));
    }

    @State(Scope.Group)
    public static class HalfOpenState {
        CircuitBreaker myLibSync;
        dev.failsafe.CircuitBreaker<Object> failsafeCb;

        @Setup(Level.Trial)
        public void setup() {
            myLibSync = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_sync_half_open")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(false)
                    .closeState(c -> c.windowSize(5).exceptionRateThreshold(0.1))
                    .waitDurationInOpenState(Duration.ofMillis(1))
                    .halfOpenState(h -> h.maxRequestInHalfOpenState(3)
                            .maxExceptionCountInHalfOpenState(1))
                    .build());

            failsafeCb = dev.failsafe.CircuitBreaker.builder()
                    .handle(RuntimeException.class)
                    .withFailureThreshold(1, 5)
                    .withDelay(Duration.ofMillis(1))
                    .withSuccessThreshold(3)
                    .build();
        }
    }

    @Benchmark
    @Group("myLibSync_halfOpenContention")
    @GroupThreads(1)
    public void breakerOpener_myLibSync(HalfOpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibSync, () -> { throw new RuntimeException(); }));
    }

    @Benchmark
    @Group("myLibSync_halfOpenContention")
    @GroupThreads(7)
    public void breakerProber_myLibSync(HalfOpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibSync, () -> "probe"));
    }

    @Benchmark
    @Group("failsafe_halfOpenContention")
    @GroupThreads(1)
    public void breakerOpener_failsafe(HalfOpenState state, Blackhole bh) {
        bh.consume(executeFailsafe(state.failsafeCb, () -> { throw new RuntimeException(); }));
    }

    @Benchmark
    @Group("failsafe_halfOpenContention")
    @GroupThreads(7)
    public void breakerProber_failsafe(HalfOpenState state, Blackhole bh) {
        bh.consume(executeFailsafe(state.failsafeCb, () -> "probe"));
    }

    private static String executeMy(CircuitBreaker cb, java.util.function.Supplier<String> supplier) {
        try {
            return cb.execute(supplier::get);
        } catch (Throwable t) {
            return "fallback";
        }
    }

    private static String executeFailsafe(dev.failsafe.CircuitBreaker<Object> cb, java.util.function.Supplier<String> supplier) {
        try {
            return Failsafe.with(cb).get(supplier::get);
        } catch (Throwable t) {
            return "fallback";
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SyncVsFailsafeBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}