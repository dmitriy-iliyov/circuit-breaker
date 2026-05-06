package io.github.dmitriyiliyov.circuitbreaker.benchmark;

import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker;
import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerFactory;
import io.github.dmitriyiliyov.circuitbreaker.core.DefaultCircuitBreakerRegistry;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.providers.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
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
@Threads(Threads.MAX)
public class LockFreeVsResilience4jBenchmark {

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
        CircuitBreaker myLib;
        io.github.resilience4j.circuitbreaker.CircuitBreaker rs4j;

        @Setup(Level.Trial)
        public void setup() {
            myLib = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_closed")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(true)
                    .closeState(c -> c.windowSize(1000).exceptionRateThreshold(0.5))
                    .waitDurationInOpenState(Duration.ofHours(1))
                    .build());

            rs4j = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("rs4j_closed",
                    CircuitBreakerConfig.custom().slidingWindowSize(1000).failureRateThreshold(0.5f).build());
        }
    }

    @Benchmark
    public void testClosed_myLib(ClosedState state, Blackhole bh) {
        bh.consume(executeMy(state.myLib, () -> "ok"));
    }

    @Benchmark
    public void testClosed_rs4j(ClosedState state, Blackhole bh) {
        bh.consume(executeRs4j(state.rs4j, () -> "ok"));
    }

    @State(Scope.Benchmark)
    public static class OpenState {
        CircuitBreaker myLib;
        io.github.resilience4j.circuitbreaker.CircuitBreaker rs4j;

        @Setup(Level.Trial)
        public void setup() {
            myLib = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_open")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(true)
                    .closeState(c -> c.windowSize(2).exceptionRateThreshold(0.1))
                    .waitDurationInOpenState(Duration.ofHours(1))
                    .build());

            rs4j = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("rs4j_open",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(2).failureRateThreshold(10f)
                            .waitDurationInOpenState(Duration.ofHours(1))
                            .build());

            try { myLib.execute(() -> { throw new RuntimeException(); }); } catch (Throwable ignored) {}
            rs4j.transitionToOpenState();
        }
    }

    @Benchmark
    public void testOpen_myLib(OpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLib, () -> "should_fail"));
    }

    @Benchmark
    public void testOpen_rs4j(OpenState state, Blackhole bh) {
        bh.consume(executeRs4j(state.rs4j, () -> "should_fail"));
    }

    @State(Scope.Group)
    public static class HalfOpenState {
        CircuitBreaker myLib;
        io.github.resilience4j.circuitbreaker.CircuitBreaker rs4j;

        @Setup(Level.Trial)
        public void setup() {
            myLib = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_half_open")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(true)
                    .closeState(c -> c.windowSize(5).exceptionRateThreshold(0.1))
                    .waitDurationInOpenState(Duration.ofMillis(1))
                    .halfOpenState(h -> h.maxRequestInHalfOpenState(3)
                            .maxExceptionCountInHalfOpenState(1))
                    .build());

            rs4j = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("rs4j_half_open",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(5).failureRateThreshold(10f)
                            .waitDurationInOpenState(Duration.ofMillis(1))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build());
        }
    }

    @Benchmark
    @Group("myLib_halfOpenContention")
    @GroupThreads(1)
    public void breakerOpener_myLib(HalfOpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLib, () -> { throw new RuntimeException(); }));
    }

    @Benchmark
    @Group("myLib_halfOpenContention")
    @GroupThreads(7)
    public void breakerProber_myLib(HalfOpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLib, () -> "probe"));
    }

    @Benchmark
    @Group("rs4j_halfOpenContention")
    @GroupThreads(1)
    public void breakerOpener_rs4j(HalfOpenState state, Blackhole bh) {
        bh.consume(executeRs4j(state.rs4j, () -> { throw new RuntimeException(); }));
    }

    @Benchmark
    @Group("rs4j_halfOpenContention")
    @GroupThreads(7)
    public void breakerProber_rs4j(HalfOpenState state, Blackhole bh) {
        bh.consume(executeRs4j(state.rs4j, () -> "probe"));
    }

    private static String executeMy(CircuitBreaker cb, java.util.function.Supplier<String> supplier) {
        try {
            return cb.execute(supplier::get);
        } catch (Throwable t) {
            return "fallback";
        }
    }

    private static String executeRs4j(io.github.resilience4j.circuitbreaker.CircuitBreaker cb, java.util.function.Supplier<String> supplier) {
        try {
            return cb.executeSupplier(supplier);
        } catch (Throwable t) {
            return "fallback";
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LockFreeVsResilience4jBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}