package io.github.dmitriyiliyov.circuitbreaker.benchmark;

import io.github.dmitriyiliyov.circuitbreaker.core.CircuitBreaker;
import io.github.dmitriyiliyov.circuitbreaker.core.config.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.github.dmitriyiliyov.circuitbreaker.benchmark.BenchmarkSupport.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
@Threads(8)
public class LockFreeVsResilience4jBenchmark {

    @State(Scope.Benchmark)
    public static class ClosedState {

        @Param("100")
        int loopLimit;
        CircuitBreaker myLibLockFree;
        io.github.resilience4j.circuitbreaker.CircuitBreaker rs4j;

        @Setup(Level.Trial)
        public void setup() {
            myLibLockFree = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_lockfree_closed")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(true)
                    .closeState(c -> c.windowSize(1000).exceptionRateThreshold(0.5))
                    .waitDurationInOpenState(Duration.ofHours(1))
                    .build());

            rs4j = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("rs4j_closed",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(1000).failureRateThreshold(50f)
                            .waitDurationInOpenState(Duration.ofHours(1))
                            .writableStackTraceEnabled(false)
                            .build());
        }
    }

    @Benchmark
    public void testClosed_myLibLockFree(ClosedState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibLockFree, () -> workload(state.loopLimit)));
    }

    @Benchmark
    public void testClosed_rs4j(ClosedState state, Blackhole bh) {
        bh.consume(executeRs4j(state.rs4j, () -> workload(state.loopLimit)));
    }

    @State(Scope.Benchmark)
    public static class OpenState {

        CircuitBreaker myLibLockFree;
        io.github.resilience4j.circuitbreaker.CircuitBreaker rs4j;

        @Setup(Level.Trial)
        public void setup() {
            myLibLockFree = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_lockfree_open")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(true)
                    .closeState(c -> c.windowSize(2).exceptionRateThreshold(0.1))
                    .waitDurationInOpenState(Duration.ofHours(1))
                    .build());

            rs4j = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("rs4j_open",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(2).failureRateThreshold(10f)
                            .waitDurationInOpenState(Duration.ofHours(1))
                            .writableStackTraceEnabled(false)
                            .build());

            try {
                myLibLockFree.execute(() -> {
                    throw new RuntimeException();
                });
            } catch (Throwable ignored) {}

            rs4j.transitionToOpenState();
        }
    }

    @Benchmark
    public void testOpen_myLibLockFree(OpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibLockFree, () -> "should_fail"));
    }

    @Benchmark
    public void testOpen_rs4j(OpenState state, Blackhole bh) {
        bh.consume(executeRs4j(state.rs4j, () -> "should_fail"));
    }

    @State(Scope.Group)
    public static class HalfOpenState {

        CircuitBreaker myLibLockFree;
        io.github.resilience4j.circuitbreaker.CircuitBreaker rs4j;

        @Setup(Level.Trial)
        public void setup() {
            myLibLockFree = FACTORY.create(CircuitBreakerConfiguration.builder()
                    .name("myLib_lockfree_half_open")
                    .observableExceptions(Set.of(RuntimeException.class))
                    .lockFree(true)
                    .closeState(c -> c.windowSize(5).exceptionRateThreshold(0.1))
                    .waitDurationInOpenState(Duration.ofMillis(1))
                    .halfOpenState(h -> h.maxRequestInHalfOpenState(3)
                            .maxExceptionCountInHalfOpenState(1))
                    .build());

            // no automatic OPEN -> HALF_OPEN transition: like myLib and Failsafe, the transition happens on the next call
            rs4j = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("rs4j_half_open",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(5).failureRateThreshold(10f)
                            .waitDurationInOpenState(Duration.ofMillis(1))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .writableStackTraceEnabled(false)
                            .build());
        }
    }

    @Benchmark
    @Group("myLibLockFree_halfOpenContention")
    @GroupThreads(1)
    public void breakerOpener_myLibLockFree(HalfOpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibLockFree, () -> { throw new RuntimeException(); }));
    }

    @Benchmark
    @Group("myLibLockFree_halfOpenContention")
    @GroupThreads(7)
    public void breakerProber_myLibLockFree(HalfOpenState state, Blackhole bh) {
        bh.consume(executeMy(state.myLibLockFree, () -> "probe"));
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

    private static String executeRs4j(io.github.resilience4j.circuitbreaker.CircuitBreaker cb, Supplier<String> supplier) {
        try {
            return cb.executeSupplier(supplier);
        } catch (Throwable t) {
            return "fallback";
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LockFreeVsResilience4jBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("jmh_lockfree_rs4j_result.json")
                .build();
        new Runner(opt).run();
    }
}
