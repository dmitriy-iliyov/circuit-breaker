package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class AggressiveFixedTimeWindowErrorRateStrategyUnitTests {

    private AggressiveFixedTimeWindowErrorRateStrategy strategy;

    @BeforeEach
    public void refreshStrategy() {
        strategy = new AggressiveFixedTimeWindowErrorRateStrategy(Duration.ofMillis(100), 0.2);
    }

    @Test
    @DisplayName("UT: all requests without exceptions should result in shouldTrip being false")
    public void allRequestWithoutExceptions_shouldTripShouldBeFalse() {
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: exception frequency threshold not reached should result in shouldTrip being false")
    public void exceptionFrequencyThresholdNotReached_shouldTripShouldBeFalse() {
        for (int i = 0; i < 9; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 1; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: exception frequency threshold reached should result in shouldTrip being true")
    public void exceptionFrequencyThresholdReached_shouldTripShouldBeTrue() {
        for (int i = 0; i < 8; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 2; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isTrue();
    }

    @Test
    @DisplayName("UT: one success round followed by another success round should result in shouldTrip being false")
    public void oneSuccessRound_shouldTripShouldBeFalse() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isFalse();

        Thread.sleep(150);

        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: when time window expired should result in shouldTrip being false")
    public void timeWindowExpired_shouldTripShouldBeFalse() throws InterruptedException {
        for (int i = 0; i < 2; i++) {
            strategy.onException();
        }
        for (int i = 0; i < 7; i++) {
            strategy.onRequest();
        }

        assertThat(strategy.shouldTrip()).isTrue();

        Thread.sleep(150);

        strategy.onException();
        assertThat(strategy.shouldTrip()).isTrue();
    }

    @Test
    @DisplayName("UT: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse() {
        for (int i = 0; i < 8; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 2; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isTrue();

        strategy.reset();

        assertThat(strategy.shouldTrip()).isFalse();

        for (int i = 0; i < 5; i++) {
            strategy.onRequest();
        }
        strategy.onException();
        assertThat(strategy.shouldTrip()).isFalse();
        strategy.onException();
        assertThat(strategy.shouldTrip()).isTrue();
    }
}
