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

public class WaitingRequestFixedTimeWindowErrorRateStrategyUnitTests {

    public record TestParams(
            Duration windowTime,
            double threshold,
            int minRequests,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(Duration windowTime, double threshold, int minRequests, Map<Integer, Map<Integer, Boolean>> answers) {
            return new TestParams(windowTime, threshold, minRequests, answers);
        }
    }

    static Stream<TestParams> testConfig() {
        Map<Integer, Map<Integer, Boolean>> commonAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, false),
                3, Map.of(1, true)
        );

        return Stream.of(
                TestParams.of(Duration.ofMillis(1000), 0.5, 5, commonAnswers),
                TestParams.of(Duration.ofMillis(500), 0.2, 3, commonAnswers),
                TestParams.of(Duration.ofMillis(200), 0.8, 10, commonAnswers)
        );
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №1: should NOT trip when request count is below the minimum, even if error rate is high")
    public void shouldNotTrip_whenRequestCountBelowMinimum(TestParams params) {
        WaitingRequestFixedTimeWindowErrorRateStrategy strategy = new WaitingRequestFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold(), params.minRequests()
        );
        for (int i = 0; i < 1; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.minRequests() - 2; i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №2: should NOT trip when request count is sufficient but error rate is below threshold")
    public void shouldNotTrip_whenThresholdNotReached(TestParams params) {
        WaitingRequestFixedTimeWindowErrorRateStrategy strategy = new WaitingRequestFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold(), params.minRequests()
        );
        for (int i = 0; i < params.minRequests(); i++) {
            strategy.onRequest();
        }
        
        int exceptions = 0;
        if (params.threshold() > 0.5) exceptions = 1;
        
        for (int i = 0; i < exceptions; i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №3: should trip when request count is sufficient and error rate reaches threshold")
    public void shouldTrip_whenThresholdReached(TestParams params) {
        WaitingRequestFixedTimeWindowErrorRateStrategy strategy = new WaitingRequestFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold(), params.minRequests()
        );
        
        for (int i = 0; i < params.minRequests(); i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(3).get(1));
    }

    @Test
    @DisplayName("should throw exception for null duration")
    public void shouldThrowExceptionForNullDuration() {
        assertThatThrownBy(() -> new WaitingRequestFixedTimeWindowErrorRateStrategy(null, 0.5, 5))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("cannot be null");
    }

    @Test
    @DisplayName("should throw exception for negative threshold")
    public void shouldThrowExceptionForNegativeThreshold() {
        assertThatThrownBy(() -> new WaitingRequestFixedTimeWindowErrorRateStrategy(Duration.ofSeconds(1), -0.1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionRateThreshold cannot be negative");
    }

    @Test
    @DisplayName("should throw exception for negative start request count")
    public void shouldThrowExceptionForNegativeStartRequestCount() {
        assertThatThrownBy(() -> new WaitingRequestFixedTimeWindowErrorRateStrategy(Duration.ofSeconds(1), 0.5, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("observeStartRequestCount cannot be negative");
    }
}
