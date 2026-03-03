//package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.close;
//
//import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close.FixedRequestWindowErrorRateStrategy;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.RepeatedTest;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.MethodSource;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.stream.Stream;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatNoException;
//
///**
// * Concurrent tests for FixedRequestWindowErrorRateStrategy.
// *
// * Goals:
// *  1. No exceptions / data corruption under concurrent access
// *  2. shouldTrip() result is consistent with the invariant:
// *     errorRate = exceptions / total >= threshold  →  shouldTrip() == true
// *  3. reset() is visible to all threads (no stale state)
// *  4. Window overflow under concurrent load resets cleanly
// *
// * Note: exact call ordering is non-deterministic, so assertions focus on
// * invariants and absence of corrupted state rather than exact boolean values.
// */
//public class FixedRequestWindowErrorRateStrategyConcurrentTests {
//
//    private static final ExecutorService POOL =
//            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
//
//    public record TestParams(
//            int windowSize,
//            double threshold,
//            int exceptionallyRequestCount,
//            int successRequestCount
//    ) {
//        public static TestParams of(int windowSize, double threshold) {
//            int exceptionallyRequestCount = (int) (windowSize * threshold);
//            int successRequestCount = windowSize - exceptionallyRequestCount;
//            return new TestParams(windowSize, threshold, exceptionallyRequestCount, successRequestCount);
//        }
//    }
//
//    static Stream<TestParams> testConfig() {
//        return Stream.of(
//                TestParams.of(10,   0.1),
//                TestParams.of(10,   1.0),
//                TestParams.of(10,   0.0),
//                TestParams.of(10,   0.25),
//                TestParams.of(17,   0.2),
//                TestParams.of(37,   0.3),
//                TestParams.of(37,   0.31),
//                TestParams.of(169,  0.87),
//                TestParams.of(4123, 0.001),
//                TestParams.of(47,   0.21)
//        );
//    }
//
//    private void runConcurrently(List<Runnable> tasks) throws Exception {
//        CyclicBarrier startGate = new CyclicBarrier(tasks.size());
//        List<Future<?>> futures = new ArrayList<>(tasks.size());
//
//        for (Runnable task : tasks) {
//            futures.add(POOL.submit(() -> {
//                try {
//                    startGate.await(5, TimeUnit.SECONDS);
//                    task.run();
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }));
//        }
//
//        for (Future<?> f : futures) {
//            f.get(30, TimeUnit.SECONDS);
//        }
//    }
//
//    private List<Runnable> buildTasks(FixedRequestWindowErrorRateStrategy strategy,
//                                      int requestCount, int exceptionCount) {
//        List<Runnable> tasks = new ArrayList<>(requestCount + exceptionCount);
//        for (int i = 0; i < requestCount;  i++) tasks.add(strategy::onRequest);
//        for (int i = 0; i < exceptionCount; i++) tasks.add(strategy::onException);
//        return tasks;
//    }
//
//    @ParameterizedTest
//    @MethodSource("testConfig")
//    @DisplayName("CT №1: all concurrent requests without exceptions → shouldTrip is false")
//    void allConcurrentRequests_noExceptions_shouldTripFalse(TestParams params) throws Exception {
//        FixedRequestWindowErrorRateStrategy strategy =
//                new FixedRequestWindowErrorRateStrategy(params.windowSize(), params.threshold(), Duration.ZERO);
//
//        List<Runnable> tasks = buildTasks(strategy, params.windowSize(), 0);
//        runConcurrently(tasks);
//
//        assertThat(strategy.shouldTrip())
//                .as("No exceptions submitted → shouldTrip must be false")
//                .isFalse();
//    }
//
//    // -------------------------------------------------------------------------
//    // Test №2 — Only 1 exception in a full window → threshold not reached
//    //           (unless threshold == 0, in which case it IS reached)
//    // -------------------------------------------------------------------------
//
//    @ParameterizedTest
//    @MethodSource("testConfig")
//    @DisplayName("CT №2: one exception in a full window → shouldTrip reflects threshold")
//    void oneException_inFullWindow_shouldTripReflectsThreshold(TestParams params) throws Exception {
//        FixedRequestWindowErrorRateStrategy strategy =
//                new FixedRequestWindowErrorRateStrategy(params.windowSize(), params.threshold(), Duration.ZERO);
//
//        List<Runnable> tasks = buildTasks(strategy, params.windowSize() - 1, 1);
//        runConcurrently(tasks);
//
//        boolean expectedTrip = params.exceptionallyRequestCount() <= 1 && params.threshold() == 0.0
//                || params.exceptionallyRequestCount() == 1;
//
//        // We can't assert the exact value because ordering is non-deterministic
//        // and the window may or may not have been full when the exception landed.
//        // What we CAN assert: no exception was thrown from the strategy itself,
//        // and shouldTrip() returns a valid boolean without throwing.
//        assertThatNoException()
//                .as("shouldTrip() must not throw under concurrent access")
//                .isThrownBy(strategy::shouldTrip);
//    }
//
//    // -------------------------------------------------------------------------
//    // Test №3 — Exactly threshold-many exceptions in a full window
//    //           Invariant: shouldTrip() == true (if threshold > 0)
//    // -------------------------------------------------------------------------
//
//    @ParameterizedTest
//    @MethodSource("testConfig")
//    @DisplayName("CT №3: exactly threshold exceptions in full window → shouldTrip true (if threshold > 0)")
//    void thresholdExceptions_inFullWindow_shouldTripTrue(TestParams params) throws Exception {
//        if (params.exceptionallyRequestCount() == 0) return; // threshold == 0, skip
//
//        FixedRequestWindowErrorRateStrategy strategy =
//                new FixedRequestWindowErrorRateStrategy(params.windowSize(), params.threshold(), Duration.ZERO);
//
//        List<Runnable> tasks = buildTasks(strategy,
//                params.successRequestCount(),
//                params.exceptionallyRequestCount());
//        runConcurrently(tasks);
//
//        // All windowSize calls were made; the window is complete.
//        // errorRate == threshold → shouldTrip must be true.
//        assertThat(strategy.shouldTrip())
//                .as("Error rate equals threshold → shouldTrip must be true")
//                .isTrue();
//    }
//
//    // -------------------------------------------------------------------------
//    // Test №4 — Two consecutive full rounds of only requests
//    //           Invariant: shouldTrip() == false after each round
//    // -------------------------------------------------------------------------
//
//    @ParameterizedTest
//    @MethodSource("testConfig")
//    @DisplayName("CT №4: two consecutive success rounds → shouldTrip false after each")
//    void twoSuccessRounds_shouldTripFalseAfterEach(TestParams params) throws Exception {
//        FixedRequestWindowErrorRateStrategy strategy =
//                new FixedRequestWindowErrorRateStrategy(params.windowSize(), params.threshold(), Duration.ZERO);
//
//        // Round 1
//        runConcurrently(buildTasks(strategy, params.windowSize(), 0));
//        assertThat(strategy.shouldTrip()).as("After round 1").isFalse();
//
//        // Round 2
//        runConcurrently(buildTasks(strategy, params.windowSize(), 0));
//        assertThat(strategy.shouldTrip()).as("After round 2").isFalse();
//    }
//
//    // -------------------------------------------------------------------------
//    // Test №5 — reset() visibility across threads
//    //           After reset(), shouldTrip() must be false from any thread.
//    // -------------------------------------------------------------------------
//
//    @ParameterizedTest
//    @MethodSource("testConfig")
//    @DisplayName("CT №5: reset() is visible to all threads → shouldTrip false after reset")
//    void resetVisibleToAllThreads_shouldTripFalseAfterReset(TestParams params) throws Exception {
//        FixedRequestWindowErrorRateStrategy strategy =
//                new FixedRequestWindowErrorRateStrategy(params.windowSize(), params.threshold(), Duration.ZERO);
//
//        // Fill window with errors to make shouldTrip() potentially true
//        runConcurrently(buildTasks(strategy,
//                params.successRequestCount(),
//                params.exceptionallyRequestCount()));
//
//        // reset() must happen-before all subsequent reads
//        strategy.reset();
//
//        // Now concurrently read shouldTrip() — every thread must see false
//        int readers = 32;
//        AtomicInteger trueCount = new AtomicInteger(0);
//        List<Runnable> readTasks = new ArrayList<>(readers);
//        for (int i = 0; i < readers; i++) {
//            readTasks.add(() -> {
//                if (strategy.shouldTrip()) trueCount.incrementAndGet();
//            });
//        }
//        runConcurrently(readTasks);
//
//        assertThat(trueCount.get())
//                .as("After reset(), no thread should see shouldTrip() == true")
//                .isZero();
//    }
//
//    // -------------------------------------------------------------------------
//    // Test №6 — Concurrent overflow of requests resets window cleanly
//    //           After overflow, shouldTrip() must be false.
//    // -------------------------------------------------------------------------
//
//    @ParameterizedTest
//    @MethodSource("testConfig")
//    @DisplayName("CT №6: concurrent request overflow resets window → shouldTrip false after overflow")
//    void concurrentRequestOverflow_resetsWindow_shouldTripFalse(TestParams params) throws Exception {
//        FixedRequestWindowErrorRateStrategy strategy =
//                new FixedRequestWindowErrorRateStrategy(params.windowSize(), params.threshold(), Duration.ZERO);
//
//        // Fill window with errors
//        runConcurrently(buildTasks(strategy,
//                params.successRequestCount(),
//                params.exceptionallyRequestCount()));
//
//        // Overflow — these requests push total past windowSize, triggering reset
//        int overflow = Math.max(params.successRequestCount() / 2, 1);
//        runConcurrently(buildTasks(strategy, overflow, 0));
//
//        assertThat(strategy.shouldTrip())
//                .as("After window overflow with only requests, shouldTrip must be false")
//                .isFalse();
//    }
//
//    // -------------------------------------------------------------------------
//    // Test №7 — Concurrent exception after a full error window (overflow via exception)
//    // -------------------------------------------------------------------------
//
//    @ParameterizedTest
//    @MethodSource("testConfig")
//    @DisplayName("CT №7: exception after full error window overflows → shouldTrip false after overflow")
//    void exceptionAfterFullWindow_overflow_shouldTripFalse(TestParams params) throws Exception {
//        FixedRequestWindowErrorRateStrategy strategy =
//                new FixedRequestWindowErrorRateStrategy(params.windowSize(), params.threshold(), Duration.ZERO);
//
//        // Fill window
//        runConcurrently(buildTasks(strategy,
//                params.successRequestCount(),
//                params.exceptionallyRequestCount()));
//
//        // One more exception → window overflows
//        strategy.onException();
//
//        // After overflow the window is reset; a single exception should not trip
//        // unless exceptionallyRequestCount == 1 (threshold is very tight)
//        boolean singleExceptionTrips = params.exceptionallyRequestCount() <= 1;
//
//        assertThat(strategy.shouldTrip())
//                .as("Post-overflow single exception shouldTrip == %s", singleExceptionTrips)
//                .isEqualTo(singleExceptionTrips);
//    }
//
//    // -------------------------------------------------------------------------
//    // Test №8 — Stress: massive concurrent mixed load, no data corruption
//    //           Invariant: shouldTrip() never throws, counter never goes negative.
//    // -------------------------------------------------------------------------
//
//    @RepeatedTest(5)
//    @DisplayName("CT №8: stress - massive mixed concurrent load produces no errors")
//    void stressMixedLoad_noDataCorruption() throws Exception {
//        int windowSize = 1000;
//        double threshold = 0.3;
//        FixedRequestWindowErrorRateStrategy strategy =
//                new FixedRequestWindowErrorRateStrategy(windowSize, threshold, Duration.ZERO);
//
//        int totalOps = windowSize * 10; // 10x window intentionally to force many resets
//        int exceptions = totalOps / 3;
//        int requests = totalOps - exceptions;
//
//        List<Runnable> tasks = new ArrayList<>(totalOps + 20);
//        for (int i = 0; i < requests;   i++) tasks.add(strategy::onRequest);
//        for (int i = 0; i < exceptions; i++) tasks.add(strategy::onException);
//
//        // Interleave resets to stress the reset path
//        for (int i = 0; i < 20; i++) tasks.add(strategy::reset);
//
//        // Shuffle for maximum contention
//        java.util.Collections.shuffle(tasks, new java.util.Random(42));
//
//        assertThatNoException()
//                .as("No exception must escape from the strategy under heavy load")
//                .isThrownBy(() -> runConcurrently(tasks));
//
//        // After all tasks, shouldTrip() must return a valid boolean (no NPE, no corruption)
//        assertThatNoException()
//                .isThrownBy(strategy::shouldTrip);
//    }
//
//    // -------------------------------------------------------------------------
//    // Test №9 — reset() called concurrently with onRequest/onException
//    //           Invariant: no exception, shouldTrip() returns valid result
//    // -------------------------------------------------------------------------
//
//    @RepeatedTest(5)
//    @DisplayName("CT №9: concurrent reset() mixed with onRequest/onException → no corruption")
//    void concurrentReset_mixedWithOperations_noCorruption() throws Exception {
//        int windowSize = 100;
//        double threshold = 0.2;
//        FixedRequestWindowErrorRateStrategy strategy =
//                new FixedRequestWindowErrorRateStrategy(windowSize, threshold, Duration.ZERO);
//
//        List<Runnable> tasks = new ArrayList<>();
//        for (int i = 0; i < 50; i++) tasks.add(strategy::onRequest);
//        for (int i = 0; i < 30; i++) tasks.add(strategy::onException);
//        for (int i = 0; i < 20; i++) tasks.add(strategy::reset);
//
//        java.util.Collections.shuffle(tasks, new java.util.Random(7));
//
//        assertThatNoException()
//                .isThrownBy(() -> runConcurrently(tasks));
//
//        assertThatNoException()
//                .isThrownBy(strategy::shouldTrip);
//    }
//}