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

public class WaitingTimeFixedTimeWindowErrorRateStrategyUnitTests {

    public record TestParams(
            Duration windowTime,
            double threshold,
            Duration observeStartTime,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(Duration windowTime, double threshold, Duration observeStartTime, Map<Integer, Map<Integer, Boolean>> answers) {
            return new TestParams(windowTime, threshold, observeStartTime, answers);
        }
    }

    static Stream<TestParams> testConfig() {
        Map<Integer, Map<Integer, Boolean>> commonAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, false),
                3, Map.of(1, true),
                4, Map.of(1, true, 2, false),
                5, Map.of(1, true, 2, false, 3, true)
        );

        return Stream.of(
                TestParams.of(Duration.ofMillis(200), 0.5, Duration.ofMillis(100), commonAnswers),
                TestParams.of(Duration.ofMillis(300), 0.2, Duration.ofMillis(150), commonAnswers),
                TestParams.of(Duration.ofMillis(100), 0.8, Duration.ofMillis(50), commonAnswers)
        );
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №1: should NOT trip when time is below observe start time, even if error rate is high")
    public void shouldNotTrip_whenTimeBelowObserveStartTime(TestParams params) {
        WaitingTimeFixedTimeWindowErrorRateStrategy strategy = new WaitingTimeFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold(), params.observeStartTime()
        );
        for (int i = 0; i < 1; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 3; i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №2: should NOT trip when time is sufficient but error rate is below threshold")
    public void shouldNotTrip_whenThresholdNotReached(TestParams params) throws InterruptedException {
        WaitingTimeFixedTimeWindowErrorRateStrategy strategy = new WaitingTimeFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold(), params.observeStartTime()
        );
        Thread.sleep(params.observeStartTime().toMillis() + 10);

        for (int i = 0; i < 3; i++) {
            strategy.onRequest();
        }
        
        int exceptions = 0;
        if (params.threshold() > 0.4) exceptions = 2;
        else if (params.threshold() > 0.2) exceptions = 1;
        
        for (int i = 0; i < exceptions; i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №3: should trip when time is sufficient and error rate reaches threshold")
    public void shouldTrip_whenThresholdReached(TestParams params) throws InterruptedException {
        WaitingTimeFixedTimeWindowErrorRateStrategy strategy = new WaitingTimeFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold(), params.observeStartTime()
        );
        Thread.sleep(params.observeStartTime().toMillis() + 10);

        for (int i = 0; i < 2; i++) {
            strategy.onRequest();
        }
        
        int exceptions = 3;
        if (params.threshold() > 0.6) exceptions = 10;
        
        for (int i = 0; i < exceptions; i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №4: should reset state after time window expires")
    public void shouldResetState_afterTimeWindowExpires(TestParams params) throws InterruptedException {
        WaitingTimeFixedTimeWindowErrorRateStrategy strategy = new WaitingTimeFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold(), params.observeStartTime()
        );
        Thread.sleep(params.observeStartTime().toMillis() + 10);

        int exceptions = 3;
        if (params.threshold() > 0.6) exceptions = 10;
        for (int i = 0; i < exceptions; i++) {
            strategy.onException();
        }
        for (int i = 0; i < 2; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(1));

        Thread.sleep(params.windowTime().toMillis() + 50);

        strategy.onRequest();

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(2));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №5: reset() method should clear state and shouldTrip should be false")
    public void resetMethod_shouldClearState(TestParams params) throws InterruptedException {
        WaitingTimeFixedTimeWindowErrorRateStrategy strategy = new WaitingTimeFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold(), params.observeStartTime()
        );
        Thread.sleep(params.observeStartTime().toMillis() + 10);

        int exceptions = 3;
        if (params.threshold() > 0.6) exceptions = 10;
        for (int i = 0; i < exceptions; i++) {
            strategy.onException();
        }
        for (int i = 0; i < 2; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(1));

        strategy.reset();

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(2));

        strategy.onException();
        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(3));
    }

    @Test
    @DisplayName("should throw exception for null duration")
    public void shouldThrowExceptionForNullDuration() {
        assertThatThrownBy(() -> new WaitingTimeFixedTimeWindowErrorRateStrategy(null, 0.5, Duration.ofSeconds(1)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("cannot be null");
    }

    @Test
    @DisplayName("should throw exception for negative threshold")
    public void shouldThrowExceptionForNegativeThreshold() {
        assertThatThrownBy(() -> new WaitingTimeFixedTimeWindowErrorRateStrategy(Duration.ofSeconds(1), -0.1, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionRateThreshold cannot be negative");
    }
}
