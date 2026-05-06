[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/circuit-breaker/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/circuit-breaker)
[![codecov](https://codecov.io/github/dmitriy-iliyov/circuit-breaker/graph/badge.svg?token=8HOK2CVJRH)](https://codecov.io/github/dmitriy-iliyov/circuit-breaker)
[![CI](https://github.com/dmitriy-iliyov/circuit-breaker/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/circuit-breaker/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dmitriy-iliyov/circuit-breaker-starter.svg?label=maven-central&color=blue)](https://central.sonatype.com/artifact/io.github.dmitriy-iliyov/circuit-breaker-starter)
![Release](https://img.shields.io/github/release/dmitriy-iliyov/circuit-breaker)
[![GitHub Release Date](https://img.shields.io/github/release-date/dmitriy-iliyov/circuit-breaker)](https://github.com/dmitriy-iliyov/circuit-breaker/releases/latest)
![GitHub last commit](https://img.shields.io/github/last-commit/dmitriy-iliyov/circuit-breaker)

## Overview
This library is an exploratory implementation of the [Circuit Breaker Pattern](https://microservices.io/patterns/reliability/circuit-breaker.html) in Java, designed to improve system resilience by preventing cascading failures. It is not intended as a replacement for mature libraries like Resilience4j but serves as a research project that may be suitable for small to medium-sized applications where its specific design trade-offs are a good fit.

## Key Features
- **Observation Strategies**: 
    - **Sliding Window** - monitors recent requests to decide when to trip the circuit based on failure rate or count.
    - **Time-based** - keeps the circuit open for a configurable duration, allowing the downstream service time to recover.
    - **Count-based** - allows a limited number of trial requests to pass through to test if the downstream service has recovered.
- **Slow request detector** - detect and consider slow requests as exceptions.
- **Lock-Free Implementations** - each strategy has a corresponding lock-free version.
- **Gradual Half-Open State** - extra state that implements a gradually increasing load in accordance with the multiplier.

## Quick Start

1. Add dependency
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>circuit-breaker-starter</artifactId>
      <version>1.0.0</version>
  </dependency>
```

2. Create circuit breaker as Bean
```java
@Bean
public CircuitBreaker circuitBreaker(CircuitBreakerFactory circuitBreakerFactory) {
  CircuitBreakerConfiguration configuration = CircuitBreakerConfiguration.builder()
          .name("circuitBreakerInstance")
          .observableExceptions(Set.of(SpecificBusinessException.class))
          .ignorableExceptions(Set.of(IgnorableException.class))
          .exceptionPriority(ExceptionPriority.IGNORABLE)
          .maxRequestExecutionDuration(Duration.ofMillis(100))
          .lockFree(true)
          .closeState(closeState ->
                  closeState.windowSize(100)
                          .exceptionRateThreshold(0.5)
                          .initialDelay(Duration.ofMinutes(1))
          )
          .waitDurationInOpenState(Duration.ofMinutes(1))
          .halfOpenState(halfOpenState -> halfOpenState
                  .type(HalfOpenType.NORMAL)
                  .maxRequestInHalfOpenState(20)
                  .maxExceptionCountInHalfOpenState(2)
          )
          .build();
  return circuitBreakerFactory.of(configuration);
}
```

3. Use in service
```java
@Service
public class BusinessService {

    private final RestClient restClient;

    public BusinessService(RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "exampleCircuitBreaker")
    public void businessOp() {
        restClient.post()
                .body(BusinessEvent.of())
                .retrieve()
                .toBodilessEntity();
    }
}
```

## Benchmarks

Environment: Java 21, Core i9

Sync version vs Failsafe:

    Benchmark                                                                      Mode  Cnt  Score   Error   Units
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention                           thrpt   10  3.775 ± 0.403  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerOpener_failsafe    thrpt   10  0.470 ± 0.059  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerProber_failsafe    thrpt   10  3.305 ± 0.345  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention                          thrpt   10  4.578 ± 0.190  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerOpener_myLibSync  thrpt   10  0.597 ± 0.022  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerProber_myLibSync  thrpt   10  3.981 ± 0.171  ops/us
    SyncVsFailsafeBenchmark.testClosed_failsafe                                   thrpt   10  2.876 ± 0.196  ops/us
    SyncVsFailsafeBenchmark.testClosed_myLibSync                                  thrpt   10  6.756 ± 0.138  ops/us
    SyncVsFailsafeBenchmark.testOpen_failsafe                                     thrpt   10  2.727 ± 0.529  ops/us
    SyncVsFailsafeBenchmark.testOpen_myLibSync                                    thrpt   10  3.941 ± 0.305  ops/us


Lock free version vs Resilience4j:

    Benchmark                                                                      Mode  Cnt  Score   Error   Units
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention                      thrpt   10  4.883 ± 0.601  ops/us
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:breakerOpener_myLib  thrpt   10  0.631 ± 0.086  ops/us
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:breakerProber_myLib  thrpt   10  4.252 ± 0.517  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention                       thrpt   10  2.851 ± 0.135  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerOpener_rs4j    thrpt   10  0.350 ± 0.014  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerProber_rs4j    thrpt   10  2.501 ± 0.121  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_myLib                              thrpt   10  2.807 ± 0.842  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_rs4j                               thrpt   10  3.605 ± 0.202  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_myLib                                thrpt   10  3.769 ± 0.183  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_rs4j                                 thrpt   10  2.712 ± 0.113  ops/us

Sync version vs Failsafe (with GC allocation):

    Benchmark                                                                      Mode  Cnt     Score     Error   Units
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention                           thrpt   10     3.262 ±   0.459  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerOpener_failsafe    thrpt   10     0.411 ±   0.054  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerProber_failsafe    thrpt   10     2.851 ±   0.406  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.alloc.rate             thrpt   10  3777.491 ± 535.196  MB/sec
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.alloc.rate.norm        thrpt   10  1214.606 ±   0.956    B/op
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.count                  thrpt   10   172.000            counts
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.time                   thrpt   10   167.000                ms
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention                          thrpt   10     4.202 ±   0.265  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerOpener_myLibSync  thrpt   10     0.548 ±   0.032  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerProber_myLibSync  thrpt   10     3.654 ±   0.235  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.alloc.rate            thrpt   10  2902.033 ± 189.250  MB/sec
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.alloc.rate.norm       thrpt   10   724.554 ±   3.039    B/op
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.count                 thrpt   10   126.000            counts
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.time                  thrpt   10   125.000                ms
    SyncVsFailsafeBenchmark.testClosed_failsafe                                   thrpt   10     2.943 ±   0.300  ops/us
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.alloc.rate                     thrpt   10  1526.137 ± 155.715  MB/sec
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.alloc.rate.norm                thrpt   10   544.002 ±   0.001    B/op
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.count                          thrpt   10   103.000            counts
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.time                           thrpt   10    61.000                ms
    SyncVsFailsafeBenchmark.testClosed_myLibSync                                  thrpt   10     6.864 ±   0.254  ops/us
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.alloc.rate                    thrpt   10   425.345 ±  15.655  MB/sec
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.alloc.rate.norm               thrpt   10    64.998 ±   0.033    B/op
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.count                         thrpt   10    29.000            counts
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.time                          thrpt   10    30.000                ms
    SyncVsFailsafeBenchmark.testOpen_failsafe                                     thrpt   10     2.635 ±   0.509  ops/us
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.alloc.rate                       thrpt   10  2973.720 ± 574.187  MB/sec
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.alloc.rate.norm                  thrpt   10  1184.002 ±   0.001    B/op
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.count                            thrpt   10   136.000            counts
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.time                             thrpt   10   148.000                ms
    SyncVsFailsafeBenchmark.testOpen_myLibSync                                    thrpt   10     3.964 ±   0.315  ops/us
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.alloc.rate                      thrpt   10  2750.370 ± 217.850  MB/sec
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.alloc.rate.norm                 thrpt   10   728.002 ±   0.001    B/op
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.count                           thrpt   10   138.000            counts
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.time                            thrpt   10   131.000                ms

Lock free version vs Resilience4j (with GC allocation):

    Benchmark                                                                      Mode  Cnt     Score     Error   Units
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention                      thrpt   10     4.518 ±   0.334  ops/us
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:breakerOpener_myLib  thrpt   10     0.579 ±   0.059  ops/us
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:breakerProber_myLib  thrpt   10     3.939 ±   0.284  ops/us
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:gc.alloc.rate        thrpt   10  3174.308 ± 236.505  MB/sec
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:gc.alloc.rate.norm   thrpt   10   737.070 ±   0.881    B/op
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:gc.count             thrpt   10   132.000            counts
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:gc.time              thrpt   10   118.000                ms
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention                       thrpt   10     2.790 ±   0.180  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerOpener_rs4j    thrpt   10     0.350 ±   0.017  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerProber_rs4j    thrpt   10     2.440 ±   0.163  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.alloc.rate         thrpt   10  3820.393 ± 450.208  MB/sec
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.alloc.rate.norm    thrpt   10  1434.228 ±  81.479    B/op
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.count              thrpt   10   159.000            counts
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.time               thrpt   10   157.000                ms
    LockFreeVsResilience4jBenchmark.testClosed_myLib                              thrpt   10     3.071 ±   0.802  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_myLib:gc.alloc.rate                thrpt   10   187.379 ±  48.942  MB/sec
    LockFreeVsResilience4jBenchmark.testClosed_myLib:gc.alloc.rate.norm           thrpt   10    64.001 ±   0.001    B/op
    LockFreeVsResilience4jBenchmark.testClosed_myLib:gc.count                     thrpt   10    13.000            counts
    LockFreeVsResilience4jBenchmark.testClosed_myLib:gc.time                      thrpt   10    18.000                ms
    LockFreeVsResilience4jBenchmark.testClosed_rs4j                               thrpt   10     3.825 ±   0.316  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.alloc.rate                 thrpt   10   262.538 ±  21.750  MB/sec
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.alloc.rate.norm            thrpt   10    72.000 ±   0.001    B/op
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.count                      thrpt   10    18.000            counts
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.time                       thrpt   10    20.000                ms
    LockFreeVsResilience4jBenchmark.testOpen_myLib                                thrpt   10     3.773 ±   0.233  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_myLib:gc.alloc.rate                  thrpt   10  2618.189 ± 160.988  MB/sec
    LockFreeVsResilience4jBenchmark.testOpen_myLib:gc.alloc.rate.norm             thrpt   10   728.002 ±   0.001    B/op
    LockFreeVsResilience4jBenchmark.testOpen_myLib:gc.count                       thrpt   10   130.000            counts
    LockFreeVsResilience4jBenchmark.testOpen_myLib:gc.time                        thrpt   10   132.000                ms
    LockFreeVsResilience4jBenchmark.testOpen_rs4j                                 thrpt   10     2.892 ±   0.132  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.alloc.rate                   thrpt   10  3506.734 ± 160.838  MB/sec
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.alloc.rate.norm              thrpt   10  1272.001 ±   0.001    B/op
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.count                        thrpt   10   147.000            counts
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.time                         thrpt   10   149.000                ms