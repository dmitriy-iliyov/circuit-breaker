package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.ExceptionPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DefaultCircuitBreakerUnitTests {

    private DefaultCircuitBreaker circuitBreaker;
    private CircuitState initialState;
    private CircuitState nextState;
    private CircuitState otherState;

    @BeforeEach
    public void setUp() {
        initialState = mock(CircuitState.class);
        nextState = mock(CircuitState.class);
        otherState = mock(CircuitState.class);
        circuitBreaker = new DefaultCircuitBreaker(
                Set.of(RuntimeException.class),
                Set.of(IOException.class)
        );
    }

    @Test
    @DisplayName("UT: trySetState should return true and update state when current state matches previousState")
    void trySetState_shouldReturnTrueAndUpdateState_whenCurrentStateMatches() {
        circuitBreaker.setState(initialState);

        boolean result = circuitBreaker.trySetState(initialState, nextState);

        assertThat(result).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo(nextState);
    }

    @Test
    @DisplayName("UT: trySetState should return false and not update state when current state does not match previousState")
    void trySetState_shouldReturnFalseAndNotUpdateState_whenCurrentStateDoesNotMatch() {
        circuitBreaker.setState(initialState);

        boolean result = circuitBreaker.trySetState(otherState, nextState);

        assertThat(result).isFalse();
        assertThat(circuitBreaker.getState()).isEqualTo(initialState);
    }

    @Test
    @DisplayName("UT: trySetState should be thread-safe")
    void trySetState_shouldBeThreadSafe() throws InterruptedException {
        circuitBreaker.setState(initialState);
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean successFlag = new AtomicBoolean(false);
        int[] successCount = {0};

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await();
                    if (circuitBreaker.trySetState(initialState, nextState)) {
                        synchronized (successCount) {
                            successCount[0]++;
                        }
                        successFlag.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        assertThat(successCount[0]).isEqualTo(1);
        assertThat(circuitBreaker.getState()).isEqualTo(nextState);
    }

    @Test
    @DisplayName("setState should initialize state when not initialized")
    void setState_shouldInitializeState_whenNotInitialized() {
        circuitBreaker.setState(initialState);

        assertThat(circuitBreaker.getState()).isEqualTo(initialState);
    }

    @Test
    @DisplayName("setState should throw ConcurrentModificationException when state already initialized")
    void setState_shouldThrowException_whenStateAlreadyInitialized() {
        circuitBreaker.setState(initialState);
        assertThatThrownBy(() -> circuitBreaker.setState(nextState))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot modify state with this method");
    }

    @Test
    @DisplayName("prepareObservableAndIgnorableExceptions should not remove anything when no intersection and priority is OBSERVABLE")
    void prepareExceptions_shouldNotRemoveAnything_whenNoIntersection_andPriorityIsObservable() {
        Set<Class<? extends Throwable>> observable = new HashSet<>();
        observable.add(RuntimeException.class);

        Set<Class<? extends Throwable>> ignorable = new HashSet<>();
        ignorable.add(IOException.class);

        DefaultCircuitBreaker cb = new DefaultCircuitBreaker(
                observable,
                ignorable
        );

        assertThat(cb.getObservableExceptions()).containsExactly(RuntimeException.class);
        assertThat(cb.getIgnorableExceptions()).containsExactly(IOException.class);
    }

    @ParameterizedTest
    @EnumSource(ExceptionPriority.class)
    @DisplayName("getChecker should correctly identify observable exceptions")
    void getChecker_shouldIdentifyObservableExceptions(ExceptionPriority priority) {
        DefaultCircuitBreaker cb = new DefaultCircuitBreaker(
                Set.of(IllegalArgumentException.class),
                Set.of(IOException.class)
        );

        Function<Throwable, Boolean> checker = cb.getChecker();

        assertThat(checker.apply(new IllegalArgumentException())).isTrue();

        assertThat(checker.apply(new IOException())).isFalse();

        assertThat(checker.apply(new RuntimeException())).isFalse();
    }

    @Test
    @DisplayName("getChecker should handle inheritance correctly")
    void getChecker_shouldHandleInheritance() {
        DefaultCircuitBreaker cb = new DefaultCircuitBreaker(
                Set.of(RuntimeException.class),
                Set.of(IOException.class)
        );

        Function<Throwable, Boolean> checker = cb.getChecker();

        assertThat(checker.apply(new NullPointerException())).isTrue();

        assertThat(checker.apply(new java.io.FileNotFoundException())).isFalse();
    }

    @Test
    @DisplayName("getChecker should respect priority when exceptionSupplier matches both hierarchies")
    void getChecker_shouldRespectPriority_whenMatchesBoth() {
        class BaseEx extends RuntimeException {}
        class ChildEx extends BaseEx {}

        DefaultCircuitBreaker cbIgnorable = new DefaultCircuitBreaker(
                Set.of(BaseEx.class),
                Set.of(ChildEx.class)
        );

        assertThat(cbIgnorable.getChecker().apply(new ChildEx())).isFalse();

        DefaultCircuitBreaker cbObservable = new DefaultCircuitBreaker(
                Set.of(BaseEx.class),
                Set.of(ChildEx.class)
        );

        assertThat(cbObservable.getChecker().apply(new ChildEx())).isFalse();
    }

    static Stream<Arguments> checkerTestCases() {
        class BaseEx extends RuntimeException {}
        class ChildEx extends BaseEx {}

        return Stream.of(
                Arguments.of(Set.of(RuntimeException.class), Collections.emptySet(), new RuntimeException(), true),

                Arguments.of(Set.of(RuntimeException.class), Collections.emptySet(), new NullPointerException(), true),

                Arguments.of(Set.of(RuntimeException.class), Set.of(IOException.class), new IOException(), false),

                Arguments.of(Set.of(RuntimeException.class), Set.of(IOException.class), new java.io.FileNotFoundException(), false),

                Arguments.of(Set.of(RuntimeException.class), Collections.emptySet(), new Exception(), false),

                Arguments.of(Set.of(BaseEx.class), Set.of(ChildEx.class), new ChildEx(), false),

                Arguments.of(Set.of(BaseEx.class), Set.of(ChildEx.class), new ChildEx(), false),

                Arguments.of(Set.of(RuntimeException.class), Set.of(RuntimeException.class), new RuntimeException(), false),

                Arguments.of(Set.of(NumberFormatException.class), Set.of(IllegalArgumentException.class), new NumberFormatException(), false)
        );
    }

    @ParameterizedTest
    @MethodSource("checkerTestCases")
    @DisplayName("checker should return expected result for various scenarios")
    void checkerShouldWorkCorrectly(Set<Class<? extends Throwable>> observable,
                                    Set<Class<? extends Throwable>> ignorable,
                                    Throwable exceptionToCheck,
                                    boolean expectedResult) {
        DefaultCircuitBreaker cb = new DefaultCircuitBreaker(observable, ignorable);
        Function<Throwable, Boolean> checker = cb.getChecker();

        assertThat(checker.apply(exceptionToCheck))
                .as("Checking exceptionSupplier %s with observable=%s, ignorable=%s",
                        exceptionToCheck.getClass().getSimpleName(), observable, ignorable)
                .isEqualTo(expectedResult);
    }
}
