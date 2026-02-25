package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.close;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class WaitingRequestFixedTimeWindowErrorRateStrategyUnitTests {

    private WaitingRequestFixedTimeWindowErrorRateStrategy strategy;
    private static final int MIN_REQUESTS = 5;
    private static final double THRESHOLD = 0.5;
    private static final Duration WINDOW_TIME = Duration.ofMillis(1000);

    @BeforeEach
    public void refreshStrategy() {
        strategy = new WaitingRequestFixedTimeWindowErrorRateStrategy(WINDOW_TIME, THRESHOLD, MIN_REQUESTS);
    }

    @Test
    @DisplayName("UT: should NOT trip when request count is below the minimum, even if error rate is high")
    public void shouldNotTrip_whenRequestCountBelowMinimum() {
        for (int i = 0; i < 1; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 3; i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: should NOT trip when request count is sufficient but error rate is below threshold")
    public void shouldNotTrip_whenThresholdNotReached() {
        for (int i = 0; i < 3; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 2; i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isFalse();
    }

    @Test
    @DisplayName("UT: should trip when request count is sufficient and error rate reaches threshold")
    public void shouldTrip_whenThresholdReached() {
        for (int i = 0; i < 2; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 3; i++) {
            strategy.onException();
        }

        assertThat(strategy.shouldTrip()).isTrue();
    }
}
