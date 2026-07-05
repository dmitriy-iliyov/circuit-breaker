package io.github.dmitriyiliyov.circuitbreaker.benchmark;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerRegistry;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
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
@Warmup(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
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

        @Param("100")
        int loopLimit;
        CircuitBreaker myLibSync;
        FailsafeExecutor<Object> failsafeCbExecutor;

        @Setup(Level.Trial)
        public void setup() {
            myLibSync = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_sync_closed")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(false)
                    .closeState(c -> c.windowSize(1000).exceptionRateThreshold(0.5))
                    .waitDurationInOpenState(Duration.ofMillis(1))
                    .build());

            dev.failsafe.CircuitBreaker<Object> failsafeCb = dev.failsafe.CircuitBreaker.builder()
                    .handle(RuntimeException.class)
                    .withFailureThreshold(500, 1000)
                    .build();

            failsafeCbExecutor = Failsafe.with(failsafeCb);
        }
    }

    @Benchmark
    public void testClosed_myLibSync(ClosedState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibSync, () -> {
            int sum = 0;
            for (int i = 0; i < state.loopLimit; i++) {
                sum += i;
            }
            return "ok" + sum;
        }));
    }

    @Benchmark
    public void testClosed_failsafe(ClosedState state, Blackhole bh) {
        bh.consume(executeFailsafe(state.failsafeCbExecutor, () -> {
            int sum = 0;
            for (int i = 0; i < state.loopLimit; i++) {
                sum += i;
            }
            return "ok" + sum;
        }));
    }

    @State(Scope.Benchmark)
    public static class OpenState {

        CircuitBreaker myLibSync;
        FailsafeExecutor<Object> failsafeCbExecutor;

        @Setup(Level.Trial)
        public void setup() {
            myLibSync = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_sync_open")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(false)
                    .closeState(c -> c.windowSize(2).exceptionRateThreshold(0.1))
                    .waitDurationInOpenState(Duration.ofHours(1))
                    .build());

            dev.failsafe.CircuitBreaker<Object> failsafeCb = dev.failsafe.CircuitBreaker.builder()
                    .handle(RuntimeException.class)
                    .withFailureThreshold(1)
                    .withDelay(Duration.ofHours(1))
                    .build();

            failsafeCbExecutor = Failsafe.with(failsafeCb);

            try {
                myLibSync.execute(() -> {
                    throw new RuntimeException();
                });
            } catch (Throwable ignored) {}

            failsafeCb.open();
        }
    }

    @Benchmark
    public void testOpen_myLibSync(OpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibSync, () -> "should_fail"));
    }

    @Benchmark
    public void testOpen_failsafe(OpenState state, Blackhole bh) {
        bh.consume(executeFailsafe(state.failsafeCbExecutor, () -> "should_fail"));
    }

    @State(Scope.Group)
    public static class HalfOpenState {

        CircuitBreaker myLibSync;
        FailsafeExecutor<Object> failsafeCbExecutor;

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

            dev.failsafe.CircuitBreaker<Object> failsafeCb = dev.failsafe.CircuitBreaker.builder()
                    .handle(RuntimeException.class)
                    .withFailureThreshold(1, 5)
                    .withDelay(Duration.ofMillis(1))
                    .withSuccessThreshold(3)
                    .build();

            failsafeCbExecutor = Failsafe.with(failsafeCb);
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
        bh.consume(executeFailsafe(state.failsafeCbExecutor, () -> { throw new RuntimeException(); }));
    }

    @Benchmark
    @Group("failsafe_halfOpenContention")
    @GroupThreads(7)
    public void breakerProber_failsafe(HalfOpenState state, Blackhole bh) {
        bh.consume(executeFailsafe(state.failsafeCbExecutor, () -> "probe"));
    }

    private static String executeMy(CircuitBreaker cb, java.util.function.Supplier<String> supplier) {
        try {
            return cb.execute(supplier::get);
        } catch (Throwable t) {
            return "fallback";
        }
    }

    private static String executeFailsafe(FailsafeExecutor<Object> cb, java.util.function.Supplier<String> supplier) {
        try {
            return cb.get(supplier::get);
        } catch (Throwable t) {
            return "fallback";
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SyncVsFailsafeBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("jmh_sync_failsafe_result.json")
                .build();
        new Runner(opt).run();
    }
}