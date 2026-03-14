package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenStateStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CircuitBreakerStateMachineInitializerUnitTests {

    private final CloseStateStrategy closeStateStrategy = mock(CloseStateStrategy.class);
    private final HalfOpenStateStrategy halfOpenStateStrategy = mock(HalfOpenStateStrategy.class);
    private final OpenStateStrategy openStateStrategy = mock(OpenStateStrategy.class);

    private final Strategies strategiesWithHalfOpen = new Strategies(
            closeStateStrategy, halfOpenStateStrategy, openStateStrategy
    );
    private final Strategies strategiesWithoutHalfOpen = new Strategies(
            closeStateStrategy, null, openStateStrategy
    );

    private interface TestCircuitBreaker extends CircuitBreaker, ConfigurableCircuitBreaker {}

    private final TestCircuitBreaker circuitBreaker = mock(TestCircuitBreaker.class);

    @Test
    @DisplayName("init: should call setState exactly once")
    public void init_shouldCallSetStateExactlyOnce() {
        CircuitBreakerStateMachineInitializer.init(circuitBreaker, strategiesWithHalfOpen);

        verify(circuitBreaker, times(1)).setState(any());
    }

    @Test
    @DisplayName("init: should set CloseState as initial state")
    public void init_shouldSetCloseStateAsInitialState() {
        CircuitBreakerStateMachineInitializer.init(circuitBreaker, strategiesWithHalfOpen);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("init: CloseState should link to OpenState as next state")
    public void init_closeStateShouldLinkToOpenState() {
        CircuitBreakerStateMachineInitializer.init(circuitBreaker, strategiesWithHalfOpen);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        assertThat(closeState.getNextState()).isInstanceOf(OpenState.class);
    }

    @Test
    @DisplayName("init: OpenState should link to HalfOpenState as next state")
    public void init_openStateShouldLinkToHalfOpenState() {
        CircuitBreakerStateMachineInitializer.init(circuitBreaker, strategiesWithHalfOpen);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        OpenState openState = (OpenState) closeState.getNextState();
        assertThat(openState.getNextState()).isInstanceOf(HalfOpenState.class);
    }

    @Test
    @DisplayName("init: HalfOpenState should link to CloseState and OpenState")
    public void init_halfOpenStateShouldLinkToCloseAndOpenState() {
        CircuitBreakerStateMachineInitializer.init(circuitBreaker, strategiesWithHalfOpen);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        OpenState openState = (OpenState) closeState.getNextState();
        HalfOpenState halfOpenState = (HalfOpenState) openState.getNextState();

        assertThat(halfOpenState.getCloseState()).isSameAs(closeState);
        assertThat(halfOpenState.getOpenState()).isSameAs(openState);
    }

    @Test
    @DisplayName("initWithoutHalfOpenState: should call setState exactly once")
    public void initWithoutHalfOpenState_shouldCallSetStateExactlyOnce() {
        CircuitBreakerStateMachineInitializer.initWithoutHalfOpenState(circuitBreaker, strategiesWithoutHalfOpen);

        verify(circuitBreaker, times(1)).setState(any());
    }

    @Test
    @DisplayName("initWithoutHalfOpenState: should set CloseState as initial state")
    public void initWithoutHalfOpenState_shouldSetCloseStateAsInitialState() {
        CircuitBreakerStateMachineInitializer.initWithoutHalfOpenState(circuitBreaker, strategiesWithoutHalfOpen);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("initWithoutHalfOpenState: CloseState should link to OpenState as next state")
    public void initWithoutHalfOpenState_closeStateShouldLinkToOpenState() {
        CircuitBreakerStateMachineInitializer.initWithoutHalfOpenState(circuitBreaker, strategiesWithoutHalfOpen);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        assertThat(closeState.getNextState()).isInstanceOf(OpenState.class);
    }

    @Test
    @DisplayName("initWithoutHalfOpenState: OpenState should link back to CloseState")
    public void initWithoutHalfOpenState_openStateShouldLinkBackToCloseState() {
        CircuitBreakerStateMachineInitializer.initWithoutHalfOpenState(circuitBreaker, strategiesWithoutHalfOpen);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        OpenState openState = (OpenState) closeState.getNextState();
        assertThat(openState.getNextState()).isSameAs(closeState);
    }
}