package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class FixedTimeWindowErrorCountStrategyUnitTests {

    private FixedTimeWindowErrorCountStrategy strategy;

    @BeforeEach
    public void refreshStrategy() {
        strategy = new FixedTimeWindowErrorCountStrategy(Duration.ofMillis(100), 2);
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
    @DisplayName("UT: exception count threshold not reached should result in shouldTrip being false")
    public void exceptionCountThresholdNotReached_shouldTripShouldBeFalse() {
        for (int i = 0; i < 9; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 1; i++) {
            strategy.onException();
        }
        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: exception count threshold reached should result in shouldTrip being true")
    public void exceptionCountThresholdReached_shouldTripShouldBeTrue() {
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
        assertThat(strategy.shouldTrip()).isFalse();

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
        strategy.onException();
        assertThat(strategy.shouldTrip()).isFalse();
    }
}
