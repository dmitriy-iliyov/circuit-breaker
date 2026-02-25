package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.open;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FailFastFixedRequestWindowStrategyUnitTests {

    private FailFastFixedRequestWindowStrategy strategy;

    @BeforeEach
    public void refreshStrategy() {
        strategy = new FailFastFixedRequestWindowStrategy(10);
    }

    @Test
    @DisplayName("UT: requests below window size should result in shouldTrip being false")
    public void requestsBelowWindowSize_shouldTripShouldBeFalse() {
        for (int i = 0; i < 9; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: requests equal to window size should result in shouldTrip being true")
    public void requestsEqualToWindowSize_shouldTripShouldBeTrue() {
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isTrue();
    }

    @Test
    @DisplayName("UT: requests above window size should result in shouldTrip being true")
    public void requestsAboveWindowSize_shouldTripShouldBeTrue() {
        for (int i = 0; i < 11; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isTrue();
    }

    @Test
    @DisplayName("UT: reset should clear state and shouldTrip should be false")
    public void reset_shouldClearStateAndShouldTripShouldBeFalse() {
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.shouldTrip()).isTrue();

        strategy.reset();

        assertThat(strategy.shouldTrip()).isFalse();
        strategy.onRequest();
        assertThat(strategy.shouldTrip()).isFalse();
    }
}
