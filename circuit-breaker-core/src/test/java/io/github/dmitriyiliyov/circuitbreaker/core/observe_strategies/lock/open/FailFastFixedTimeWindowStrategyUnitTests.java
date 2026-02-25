package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class FailFastFixedTimeWindowStrategyUnitTests {

    private FailFastFixedTimeWindowStrategy strategy;

    @BeforeEach
    public void refreshStrategy() {
        strategy = new FailFastFixedTimeWindowStrategy(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("UT: requests within time window should result in shouldTrip being false")
    public void requestsWithinTimeWindow_shouldTripShouldBeFalse() {
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: requests after time window should result in shouldTrip being true")
    public void requestsAfterTimeWindow_shouldTripShouldBeTrue() throws InterruptedException {
        Thread.sleep(150);

        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isTrue();
    }

    @Test
    @DisplayName("UT: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse() throws InterruptedException {
        Thread.sleep(150);
        strategy.onRequest();
        assertThat(strategy.shouldTrip()).isTrue();

        strategy.reset();

        assertThat(strategy.shouldTrip()).isFalse();
        strategy.onRequest();
        assertThat(strategy.shouldTrip()).isFalse();
    }
}
