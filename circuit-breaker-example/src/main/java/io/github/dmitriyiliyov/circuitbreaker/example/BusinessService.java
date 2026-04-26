package io.github.dmitriyiliyov.circuitbreaker.example;

import io.github.dmitriyiliyov.circuitbreaker.aop.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class BusinessService {

    @CircuitBreaker(name = "exampleCircuitBreaker")
    public void businessOp() {
        // HTTP call
    }

    @CircuitBreaker(name = "exampleCircuitBreaker")
    public BusinessEvent businessOpWithResult() {
        // real HTTP call with return
        return BusinessEvent.of();
    }

    @CircuitBreaker(name = "exampleCircuitBreaker")
    public BusinessEvent businessGetOp() {
        // real HTTP call with return
        return BusinessEvent.of();
    }

    @CircuitBreaker(name = "exampleCircuitBreaker")
    public void businessOpWithObservableException() {
        // exceptionally HTTP call with observable exception
        throw new SpecificBusinessException("business exception");
    }

    @CircuitBreaker(name = "exampleCircuitBreaker")
    public void businessOpWithIgnorableException() {
        // exceptionally HTTP call with ignorable exception
        throw new IllegalArgumentException();
    }

    @CircuitBreaker(name = "exampleCircuitBreaker")
    public void unexpectableSlowBusinessOp() throws InterruptedException {
        // slow HTTP call
        Thread.sleep(110);
    }
}
