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

public class AggressiveFixedTimeWindowErrorRateStrategyUnitTests {

    public record TestParams(
            Duration windowTime,
            double threshold,
            int exceptionallyRequestCount,
            int successRequestCount,
            Map<Integer, Map<Integer, Boolean>> answers
    ) {
        public static TestParams of(Duration windowTime, double threshold, Map<Integer, Map<Integer, Boolean>> answers) {
            int totalRequests = 10;
            int exceptionallyRequestCount = (int) Math.ceil(totalRequests * threshold);
            if (threshold > 0 && exceptionallyRequestCount == 0) exceptionallyRequestCount = 1;
            
            int successRequestCount = totalRequests - exceptionallyRequestCount;
            return new TestParams(windowTime, threshold, exceptionallyRequestCount, successRequestCount, answers);
        }
    }

    static Stream<TestParams> testConfig() {
        Map<Integer, Map<Integer, Boolean>> commonAnswers = Map.of(
                1, Map.of(1, false),
                2, Map.of(1, false),
                3, Map.of(1, true),
                4, Map.of(1, false, 2, false),
                5, Map.of(1, true, 2, true),
                6, Map.of(1, true, 2, false, 3, true)
        );

        return Stream.of(
                TestParams.of(Duration.ofMillis(100), 0.2, commonAnswers),
                TestParams.of(Duration.ofMillis(200), 0.5, commonAnswers),
                TestParams.of(Duration.ofMillis(50), 0.1, commonAnswers)
        );
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №1: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse(TestParams params) {
        AggressiveFixedTimeWindowErrorRateStrategy strategy = new AggressiveFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold()
        );
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(1).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №2: exception frequency threshold not reached should result in shouldTrip being false")
    public void exceptionFrequencyThresholdNotReached_shouldTripShouldBeFalse(TestParams params) {
        AggressiveFixedTimeWindowErrorRateStrategy strategy = new AggressiveFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold()
        );
        
        int exceptions = 0;
        if (params.threshold() > 0.15) {
             exceptions = 1;
        }
        
        for (int i = 0; i < 10 - exceptions; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < exceptions; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(2).get(1));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №3: exception frequency threshold reached should result in shouldTrip being true")
    public void exceptionFrequencyThresholdReached_shouldTripShouldBeTrue(TestParams params) {
        AggressiveFixedTimeWindowErrorRateStrategy strategy = new AggressiveFixedTimeWindowErrorRateStrategy(
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
        AggressiveFixedTimeWindowErrorRateStrategy strategy = new AggressiveFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold()
        );
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(1));

        Thread.sleep(params.windowTime().toMillis() + 50);

        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(4).get(2));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №5: when time window expired should result in shouldTrip being true (aggressive)")
    public void timeWindowExpired_shouldTripShouldBeTrue(TestParams params) throws InterruptedException {
        AggressiveFixedTimeWindowErrorRateStrategy strategy = new AggressiveFixedTimeWindowErrorRateStrategy(
                params.windowTime(), params.threshold()
        );
        for (int i = 0; i < params.successRequestCount(); i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < params.exceptionallyRequestCount(); i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(1));

        Thread.sleep(params.windowTime().toMillis() + 50);

        strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(5).get(2));
    }

    @ParameterizedTest
    @MethodSource("testConfig")
    @DisplayName("UT №6: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse(TestParams params) {
        AggressiveFixedTimeWindowErrorRateStrategy strategy = new AggressiveFixedTimeWindowErrorRateStrategy(
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

        for (int i = 0; i < 5; i++) {
            strategy.onRequest();
        }
        strategy.onException();
        
        boolean expectedAfterOneException = (1.0 / 6.0) >= params.threshold();
        assertThat(strategy.shouldTrip()).isEqualTo(expectedAfterOneException);
        
        for(int i=0; i<10; i++) strategy.onException();
        assertThat(strategy.shouldTrip()).isEqualTo(params.answers().get(6).get(3));
    }

    @Test
    @DisplayName("should throw exception for null duration")
    public void shouldThrowExceptionForNullDuration() {
        assertThatThrownBy(() -> new AggressiveFixedTimeWindowErrorRateStrategy(null, 0.5))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("cannot be null");
    }

    @Test
    @DisplayName("should throw exception for negative threshold")
    public void shouldThrowExceptionForNegativeThreshold() {
        assertThatThrownBy(() -> new AggressiveFixedTimeWindowErrorRateStrategy(Duration.ofSeconds(1), -0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exceptionRateThreshold cannot be negative");
    }
}
