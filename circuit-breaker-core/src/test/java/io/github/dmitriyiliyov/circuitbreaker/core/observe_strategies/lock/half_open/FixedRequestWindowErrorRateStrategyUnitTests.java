package io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.lock.half_open;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FixedRequestWindowErrorRateStrategyUnitTests  {

    private FixedRequestWindowErrorRateStrategy strategy;

    @BeforeEach
    public void refreshStrategy() {
        strategy = new FixedRequestWindowErrorRateStrategy(10, 0.2);
    }

    @Test
    @DisplayName("UT: all requests without exceptions should result in transition TO_CLOSE")
    public void allRequestWithoutExceptions_shouldTransitionToClose() {
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_CLOSE);
    }

    @Test
    @DisplayName("UT: exception frequency threshold not reached should result in transition TO_CLOSE")
    public void exceptionFrequencyThresholdNotReached_shouldTransitionToClose() {
        for (int i = 0; i < 9; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 1; i++) {
            strategy.onException();
        }
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_CLOSE);
    }

    @Test
    @DisplayName("UT: exception frequency threshold reached should result in transition TO_OPEN")
    public void exceptionFrequencyThresholdReached_shouldTransitionToOpen() {
        for (int i = 0; i < 8; i++) {
            strategy.onRequest();
        }
        for (int i = 0; i < 2; i++) {
            strategy.onException();
        }
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_OPEN);
    }

    @Test
    @DisplayName("UT: not enough requests should result in NO_TRANSITION")
    public void notEnoughRequests_shouldResultInNoTransition() {
        for (int i = 0; i < 9; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.NO_TRANSITION);
    }

    @Test
    @DisplayName("UT: transition to OPEN should not be overwritten by subsequent successful requests")
    public void transitionToOpen_shouldNotBeOverwrittenBySubsequentRequests() {
        for (int i = 0; i < 2; i++) {
            strategy.onException();
        }

        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_OPEN);

        for (int i = 0; i < 8; i++) {
            strategy.onRequest();
        }

        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_OPEN);
    }

    @Test
    @DisplayName("UT: reset should clear state and transition should be NO_TRANSITION")
    public void reset_shouldClearStateAndTransitionShouldBeNoTransition() {
        for (int i = 0; i < 10; i++) {
            strategy.onRequest();
        }
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.TO_CLOSE);

        strategy.reset();

        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.NO_TRANSITION);
        strategy.onRequest();
        assertThat(strategy.getTransition()).isEqualTo(HalfOpenTransition.NO_TRANSITION);
    }
}
