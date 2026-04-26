package io.github.dmitriyiliyov.circuitbreaker.example;

import io.github.dmitriyiliyov.circuitbreaker.starter.EnableCircuitBreaker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCircuitBreaker
public class CircuitBreakerExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CircuitBreakerExampleApplication.class, args);
    }
}
