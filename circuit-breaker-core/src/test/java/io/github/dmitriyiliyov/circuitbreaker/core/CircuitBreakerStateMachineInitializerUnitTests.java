package io.github.dmitriyiliyov.circuitbreaker.core;

import io.github.dmitriyiliyov.circuitbreaker.core.config.HalfOpenStateConfiguration;
import io.github.dmitriyiliyov.circuitbreaker.core.config.HalfOpenType;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.CloseStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.HalfOpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.OpenStateStrategy;
import io.github.dmitriyiliyov.circuitbreaker.core.observe_strategies.RequestTimer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class CircuitBreakerStateMachineInitializerUnitTests {

    private final CloseStateStrategy closeStateStrategy = mock(CloseStateStrategy.class);
    private final HalfOpenStateStrategy halfOpenStateStrategy = mock(HalfOpenStateStrategy.class);
    private final OpenStateStrategy openStateStrategy = mock(OpenStateStrategy.class);

    private final RequestTimer timer = mock(RequestTimer.class);

    private final Strategies strategiesWithHalfOpen = new Strategies(
            closeStateStrategy, halfOpenStateStrategy, openStateStrategy
    );
    private final Strategies strategiesWithoutHalfOpen = new Strategies(
            closeStateStrategy, null, openStateStrategy
    );

    private final HalfOpenStateConfiguration normalHalfOpenStateConfiguration = HalfOpenStateConfiguration.builder()
            .halfOpenStateEnabled(true)
            .type(HalfOpenType.NORMAL)
            .maxRequestInHalfOpenState(20)
            .maxExceptionCountInHalfOpenState(2)
            .build();

    private final HalfOpenStateConfiguration gradualHalfOpenStateConfiguration = HalfOpenStateConfiguration.builder()
            .halfOpenStateEnabled(true)
            .type(HalfOpenType.GRADUAL)
            .maxRequestInHalfOpenState(20)
            .maxExceptionCountInHalfOpenState(2)
            .multiplier(2.0)
            .build();

    private interface TestCircuitBreaker extends CircuitBreaker, ConfigurableCircuitBreaker {}

    private final TestCircuitBreaker circuitBreaker = mock(TestCircuitBreaker.class);

    @Test
    @DisplayName("UT: init() should call setState exactly once")
    public void init_WithHalfOpen_shouldCallSetStateExactlyOnce() {
        CircuitBreakerStateMachineInitializer.initWithHalfOpen(circuitBreaker, strategiesWithHalfOpen, timer, normalHalfOpenStateConfiguration);

        verify(circuitBreaker, times(1)).setState(any());
    }

    @Test
    @DisplayName("UT: init() should set CloseState as initial state")
    public void init_WithHalfOpen_shouldSetCloseStateAsInitialState() {
        CircuitBreakerStateMachineInitializer.initWithHalfOpen(circuitBreaker, strategiesWithHalfOpen, timer, normalHalfOpenStateConfiguration);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("UT: init() CloseState should link to OpenState as next state")
    public void init_WithHalfOpen_closeStateShouldLinkToOpenState() {
        CircuitBreakerStateMachineInitializer.initWithHalfOpen(circuitBreaker, strategiesWithHalfOpen, timer, normalHalfOpenStateConfiguration);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        assertThat(closeState.getNextState()).isInstanceOf(OpenState.class);
    }

    @Test
    @DisplayName("UT: init() OpenState should link to HalfOpenState as next state")
    public void init_WithHalfOpen_openStateShouldLinkToHalfOpenState() {
        CircuitBreakerStateMachineInitializer.initWithHalfOpen(circuitBreaker, strategiesWithHalfOpen, timer, normalHalfOpenStateConfiguration);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        OpenState openState = (OpenState) closeState.getNextState();
        assertThat(openState.getNextState()).isInstanceOf(HalfOpenState.class);
    }

    @Test
    @DisplayName("UT: init() HalfOpenState should link to CloseState and OpenState")
    public void init_WithHalfOpen_halfOpenStateShouldLinkToCloseAndOpenState() {
        CircuitBreakerStateMachineInitializer.initWithHalfOpen(circuitBreaker, strategiesWithHalfOpen, timer, normalHalfOpenStateConfiguration);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        OpenState openState = (OpenState) closeState.getNextState();
        HalfOpenState halfOpenState = (HalfOpenState) openState.getNextState();

        assertThat(halfOpenState.getCloseState()).isSameAs(closeState);
        assertThat(halfOpenState.getOpenState()).isSameAs(openState);
    }

    @Test
    @DisplayName("UT: init() with GRADUAL type should create GradualHalfOpenState and link correctly")
    public void init_WithGradualHalfOpen_shouldCreateGradualHalfOpenStateAndLink() {
        CircuitBreakerStateMachineInitializer.initWithHalfOpen(circuitBreaker, strategiesWithHalfOpen, timer, gradualHalfOpenStateConfiguration);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        OpenState openState = (OpenState) closeState.getNextState();
        CircuitState halfOpenState = openState.getNextState();

        assertThat(halfOpenState).isInstanceOf(GradualHalfOpenState.class);
    }

    @Test
    @DisplayName("UT: init() should throw IllegalStateException for unknown or null HalfOpenType")
    public void init_WithUnknownHalfOpenType_shouldThrowException() {
        HalfOpenStateConfiguration mockConfig = mock(HalfOpenStateConfiguration.class);
        when(mockConfig.getType()).thenReturn(null);

        assertThatThrownBy(() -> CircuitBreakerStateMachineInitializer.initWithHalfOpen(circuitBreaker, strategiesWithHalfOpen, timer, mockConfig))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown HalfOpenType");
    }

    @Test
    @DisplayName("UT: initWithoutHalfOpenState() should call setState exactly once")
    public void initWithHalfOpenWithoutHalfOpenState_shouldCallSetStateExactlyOnce() {
        CircuitBreakerStateMachineInitializer.initWithoutHalfOpen(circuitBreaker, strategiesWithoutHalfOpen, timer);

        verify(circuitBreaker, times(1)).setState(any());
    }

    @Test
    @DisplayName("UT: initWithoutHalfOpenState() should set CloseState as initial state")
    public void initWithHalfOpenWithoutHalfOpenState_shouldSetCloseStateAsInitialState() {
        CircuitBreakerStateMachineInitializer.initWithoutHalfOpen(circuitBreaker, strategiesWithoutHalfOpen, timer);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CloseState.class);
    }

    @Test
    @DisplayName("UT: initWithoutHalfOpenState() CloseState should link to OpenState as next state")
    public void initWithHalfOpenWithoutHalfOpenState_closeStateShouldLinkToOpenState() {
        CircuitBreakerStateMachineInitializer.initWithoutHalfOpen(circuitBreaker, strategiesWithoutHalfOpen, timer);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        assertThat(closeState.getNextState()).isInstanceOf(OpenState.class);
    }

    @Test
    @DisplayName("UT: initWithoutHalfOpenState() OpenState should link back to CloseState")
    public void initWithHalfOpenWithoutHalfOpenState_openStateShouldLinkBackToCloseState() {
        CircuitBreakerStateMachineInitializer.initWithoutHalfOpen(circuitBreaker, strategiesWithoutHalfOpen, timer);

        ArgumentCaptor<CircuitState> captor = ArgumentCaptor.forClass(CircuitState.class);
        verify(circuitBreaker).setState(captor.capture());

        CloseState closeState = (CloseState) captor.getValue();
        OpenState openState = (OpenState) closeState.getNextState();
        assertThat(openState.getNextState()).isSameAs(closeState);
    }
}