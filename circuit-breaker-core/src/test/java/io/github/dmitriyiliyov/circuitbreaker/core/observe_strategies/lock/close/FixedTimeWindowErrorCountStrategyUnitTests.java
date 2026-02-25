package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FixedTimeWindowErrorCountStrategyUnitTests {

    public record TestParams(
            Duration windowTime,
            long threshold,
            long exceptionallyRequestCount,
            long successRequestCount,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(Duration windowTime, long threshold, Map<Integer, Map<Integer, Boolean>> answers) {
            long exceptionallyRequestCount = threshold;
            long successRequestCount = 10;
            return new TestParams(windowTime, threshold, exceptionallyRequestCount, successRequestCount, answers);
        }
    }

    static Stream<TestParams> testConfig() {
        Map<Integer, Map<Integer, Boolean>> commonAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, false),
                3, Map.of(1, true),
                4, Map.of(1, false, 2, false),
                5, Map.of(1, true, 2, false, 3, true),
                6, Map.of(1, true, 2, false, 3, false)
        );

        Map<Integer, Map<Integer, Boolean>> thresholdOneAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, true),
                3, Map.of(1, true),
                4, Map.of(1, false, 2, false),
                5, Map.of(1, true, 2, true, 3, true),
                6, Map.of(1, true, 2, false, 3, true)
        );

        return Stream.of(
                TestParams.of(Duration.ofMillis(100), 2, commonAnswers),
                TestParams.of(Duration.ofMillis(200), 5, commonAnswers),
                TestParams.of(Duration.ofMillis(50), 1, thresholdOneAnswers)
        );
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №1: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse(TestParams params) {
        FixedTimeWindowErrorCountStrategy strategy = new FixedTimeWindowErrorCountStrategy(
                params.windowTime(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №2: exception count threshold not reached should result in shouldTrip being false")
    public void exceptionCountThresholdNotReached_shouldTripShouldBeFalse(TestParams params) {
        FixedTimeWindowErrorCountStrategy strategy = new FixedTimeWindowErrorCountStrategy(
                params.windowTime(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        long exceptions = Math.max(1, params.threshold() - 1);
        if (params.threshold() == 1) exceptions = 1;

        for (int i = 0; i < exceptions; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №3: exception count threshold reached should result in shouldTrip being true")
    public void exceptionCountThresholdReached_shouldTripShouldBeTrue(TestParams params) {
        FixedTimeWindowErrorCountStrategy strategy = new FixedTimeWindowErrorCountStrategy(
                params.windowTime(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №4: one success round followed by another success round should result in shouldTrip being false")
    public void oneSuccessRound_shouldTripShouldBeFalse(TestParams params) throws InterruptedException {
        FixedTimeWindowErrorCountStrategy strategy = new FixedTimeWindowErrorCountStrategy(
                params.windowTime(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(1));

        Thread.sleep(params.windowTime().toMillis() + 50);

        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(2));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №5: when time window expired should result in shouldTrip being false")
    public void timeWindowExpired_shouldTripShouldBeFalse(TestParams params) throws InterruptedException {
        FixedTimeWindowErrorCountStrategy strategy = new FixedTimeWindowErrorCountStrategy(
                params.windowTime(), params.threshold()
        );
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(1));

        Thread.sleep(params.windowTime().toMillis() + 50);

        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(2));

        if (params.threshold() > 1) {
            for (int i = 0; i < params.exceptionallyRequestCount() - 1; i++) {
                strategy.onException();
            }
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(3));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №6: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(TestParams params) {
        FixedTimeWindowErrorCountStrategy strategy = new FixedTimeWindowErrorCountStrategy(
                params.windowTime(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(1));

        strategy.reset();

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(2));
        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(3));
    }

    @Test
    @DisplayName("should throw exception for null duration")
    public void shouldThrowExceptionForNullDuration() {
        assertThatThrownBy(() -> new FixedTimeWindowErrorCountStrategy(null, 5))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("cannot be null");
    }

    @Test
    @DisplayName("should throw exception for negative threshold")
    public void shouldThrowExceptionForNegativeThreshold() {
        assertThatThrownBy(() -> new FixedTimeWindowErrorCountStrategy(Duration.ofSeconds(1), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionCountThreshold cannot be negative");
    }
}
