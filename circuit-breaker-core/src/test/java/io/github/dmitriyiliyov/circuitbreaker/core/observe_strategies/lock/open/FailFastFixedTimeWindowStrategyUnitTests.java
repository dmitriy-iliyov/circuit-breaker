package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FailFastFixedTimeWindowStrategyUnitTests {

    public record TestParams(
            Duration windowTime,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(Duration windowTime, Map<Integer, Map<Integer, Boolean>> answers) {
            return new TestParams(windowTime, answers);
        }
    }

    static Stream<TestParams> testConfig() {
        Map<Integer, Map<Integer, Boolean>> commonAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, true),
                3, Map.of(1, true, 2, false, 3, false)
        );

        return Stream.of(
                TestParams.of(Duration.ofMillis(100), commonAnswers),
                TestParams.of(Duration.ofMillis(200), commonAnswers),
                TestParams.of(Duration.ofMillis(50), commonAnswers)
        );
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №1: requests within time window should result in shouldTrip being false")
    public void requestsWithinTimeWindow_shouldTripShouldBeFalse(TestParams params) {
        FailFastFixedTimeWindowStrategy strategy = new FailFastFixedTimeWindowStrategy(params.windowTime());
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №2: requests after time window should result in shouldTrip being true")
    public void requestsAfterTimeWindow_shouldTripShouldBeTrue(TestParams params) throws InterruptedException {
        FailFastFixedTimeWindowStrategy strategy = new FailFastFixedTimeWindowStrategy(params.windowTime());
        Thread.sleep(params.windowTime().toMillis() + 50);

        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №3: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(TestParams params) throws InterruptedException {
        FailFastFixedTimeWindowStrategy strategy = new FailFastFixedTimeWindowStrategy(params.windowTime());
        Thread.sleep(params.windowTime().toMillis() + 50);
        strategy.onRequest();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(1));

        strategy.reset();

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(2));
        strategy.onRequest();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(3));
    }

    @Test
    @DisplayName("should throw exception for null duration")
    public void shouldThrowExceptionForNullDuration() {
        assertThatThrownBy(() -> new FailFastFixedTimeWindowStrategy(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("observeTime cannot be null");
    }
}
