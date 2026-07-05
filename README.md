[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/circuit-breaker/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/circuit-breaker)
[![codecov](https://codecov.io/github/dmitriy-iliyov/circuit-breaker/graph/badge.svg?token=8HOK2CVJRH)](https://codecov.io/github/dmitriy-iliyov/circuit-breaker)
[![CI](https://github.com/dmitriy-iliyov/circuit-breaker/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/circuit-breaker/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dmitriy-iliyov/circuit-breaker-starter.svg?label=maven-central&color=blue)](https://central.sonatype.com/artifact/io.github.dmitriy-iliyov/circuit-breaker-starter)
[![javadoc](https://javadoc.io/badge2/io.github.dmitriy-iliyov/circuit-breaker-core/javadoc.svg)](https://javadoc.io/doc/io.github.dmitriy-iliyov/circuit-breaker-core)
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

    Benchmark                                                                     (loopLimit)   Mode  Cnt  Score   Error   Units
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention                                   N/A  thrpt   20  3.121 ± 0.120  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerOpener_failsafe            N/A  thrpt   20  0.394 ± 0.014  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerProber_failsafe            N/A  thrpt   20  2.727 ± 0.106  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention                                  N/A  thrpt   20  4.044 ± 0.057  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerOpener_myLibSync          N/A  thrpt   20  0.510 ± 0.008  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerProber_myLibSync          N/A  thrpt   20  3.533 ± 0.050  ops/us
    SyncVsFailsafeBenchmark.testClosed_failsafe                                           100  thrpt   20  2.930 ± 0.055  ops/us
    SyncVsFailsafeBenchmark.testClosed_myLibSync                                          100  thrpt   20  6.560 ± 0.043  ops/us
    SyncVsFailsafeBenchmark.testOpen_failsafe                                             N/A  thrpt   20  3.058 ± 0.205  ops/us
    SyncVsFailsafeBenchmark.testOpen_myLibSync                                            N/A  thrpt   20  3.731 ± 0.057  ops/us


Lock free version vs Resilience4j:

    Benchmark                                                                     (loopLimit)   Mode  Cnt  Score   Error   Units
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention                                   N/A  thrpt   20  3.121 ± 0.120  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerOpener_failsafe            N/A  thrpt   20  0.394 ± 0.014  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerProber_failsafe            N/A  thrpt   20  2.727 ± 0.106  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention                                  N/A  thrpt   20  4.044 ± 0.057  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerOpener_myLibSync          N/A  thrpt   20  0.510 ± 0.008  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerProber_myLibSync          N/A  thrpt   20  3.533 ± 0.050  ops/us
    SyncVsFailsafeBenchmark.testClosed_failsafe                                           100  thrpt   20  2.930 ± 0.055  ops/us
    SyncVsFailsafeBenchmark.testClosed_myLibSync                                          100  thrpt   20  6.560 ± 0.043  ops/us
    SyncVsFailsafeBenchmark.testOpen_failsafe                                             N/A  thrpt   20  3.058 ± 0.205  ops/us
    SyncVsFailsafeBenchmark.testOpen_myLibSync                                            N/A  thrpt   20  3.731 ± 0.057  ops/us

Sync version vs Failsafe (with GC allocation):

    Benchmark                                                                     (loopLimit)   Mode  Cnt     Score     Error   Units
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention                                   N/A  thrpt   20     3.145 ±   0.214  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerOpener_failsafe            N/A  thrpt   20     0.392 ±   0.027  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerProber_failsafe            N/A  thrpt   20     2.753 ±   0.187  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.alloc.rate                     N/A  thrpt   20  3329.139 ± 228.556  MB/sec
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.alloc.rate.norm                N/A  thrpt   20  1110.075 ±   0.770    B/op
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.count                          N/A  thrpt   20   682.000            counts
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.time                           N/A  thrpt   20   722.000                ms
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention                                  N/A  thrpt   20     3.873 ±   0.067  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerOpener_myLibSync          N/A  thrpt   20     0.490 ±   0.009  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerProber_myLibSync          N/A  thrpt   20     3.383 ±   0.058  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.alloc.rate                    N/A  thrpt   20  2669.874 ±  48.897  MB/sec
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.alloc.rate.norm               N/A  thrpt   20   723.026 ±   1.052    B/op
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.count                         N/A  thrpt   20   578.000            counts
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.time                          N/A  thrpt   20   637.000                ms
    SyncVsFailsafeBenchmark.testClosed_failsafe                                           100  thrpt   20     2.802 ±   0.049  ops/us
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.alloc.rate                             100  thrpt   20  1218.528 ±  21.131  MB/sec
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.alloc.rate.norm                        100  thrpt   20   456.001 ±   0.001    B/op
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.count                                  100  thrpt   20   409.000            counts
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.time                                   100  thrpt   20   248.000                ms
    SyncVsFailsafeBenchmark.testClosed_myLibSync                                          100  thrpt   20     6.659 ±   0.226  ops/us
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.alloc.rate                            100  thrpt   20   514.758 ±  17.440  MB/sec
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.alloc.rate.norm                       100  thrpt   20    81.071 ±   0.008    B/op
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.count                                 100  thrpt   20   174.000            counts
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.time                                  100  thrpt   20   123.000                ms
    SyncVsFailsafeBenchmark.testOpen_failsafe                                             N/A  thrpt   20     2.961 ±   0.180  ops/us
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.alloc.rate                               N/A  thrpt   20  3049.509 ± 185.463  MB/sec
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.alloc.rate.norm                          N/A  thrpt   20  1080.001 ±   0.001    B/op
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.count                                    N/A  thrpt   20   699.000            counts
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.time                                     N/A  thrpt   20   743.000                ms
    SyncVsFailsafeBenchmark.testOpen_myLibSync                                            N/A  thrpt   20     3.849 ±   0.059  ops/us
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.alloc.rate                              N/A  thrpt   20  2671.566 ±  40.757  MB/sec
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.alloc.rate.norm                         N/A  thrpt   20   728.001 ±   0.001    B/op
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.count                                   N/A  thrpt   20   681.000            counts
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.time                                    N/A  thrpt   20   718.000                ms

Lock free version vs Resilience4j (with GC allocation):

    Benchmark                                                                     (loopLimit)   Mode  Cnt     Score     Error   Units
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention                              N/A  thrpt   20     4.476 ±   0.182  ops/us
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:breakerOpener_myLib          N/A  thrpt   20     0.553 ±   0.023  ops/us
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:breakerProber_myLib          N/A  thrpt   20     3.923 ±   0.159  ops/us
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:gc.alloc.rate                N/A  thrpt   20  3144.232 ± 130.305  MB/sec
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:gc.alloc.rate.norm           N/A  thrpt   20   736.637 ±   0.650    B/op
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:gc.count                     N/A  thrpt   20   657.000            counts
    LockFreeVsResilience4jBenchmark.myLib_halfOpenContention:gc.time                      N/A  thrpt   20   652.000                ms
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention                               N/A  thrpt   20     2.534 ±   0.110  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerOpener_rs4j            N/A  thrpt   20     0.317 ±   0.012  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerProber_rs4j            N/A  thrpt   20     2.216 ±   0.098  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.alloc.rate                 N/A  thrpt   20  3392.048 ± 152.417  MB/sec
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.alloc.rate.norm            N/A  thrpt   20  1403.959 ±  11.894    B/op
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.count                      N/A  thrpt   20   778.000            counts
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.time                       N/A  thrpt   20   808.000                ms
    LockFreeVsResilience4jBenchmark.testClosed_myLib                                      100  thrpt   20     2.927 ±   0.250  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_myLib:gc.alloc.rate                        100  thrpt   20   223.301 ±  19.088  MB/sec
    LockFreeVsResilience4jBenchmark.testClosed_myLib:gc.alloc.rate.norm                   100  thrpt   20    80.000 ±   0.001    B/op
    LockFreeVsResilience4jBenchmark.testClosed_myLib:gc.count                             100  thrpt   20    76.000            counts
    LockFreeVsResilience4jBenchmark.testClosed_myLib:gc.time                              100  thrpt   20    63.000                ms
    LockFreeVsResilience4jBenchmark.testClosed_rs4j                                       100  thrpt   20     3.223 ±   0.333  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.alloc.rate                         100  thrpt   20   270.414 ±  27.982  MB/sec
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.alloc.rate.norm                    100  thrpt   20    88.000 ±   0.001    B/op
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.count                              100  thrpt   20    90.000            counts
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.time                               100  thrpt   20    54.000                ms
    LockFreeVsResilience4jBenchmark.testOpen_myLib                                        N/A  thrpt   20     4.148 ±   0.099  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_myLib:gc.alloc.rate                          N/A  thrpt   20  2879.229 ±  68.987  MB/sec
    LockFreeVsResilience4jBenchmark.testOpen_myLib:gc.alloc.rate.norm                     N/A  thrpt   20   728.001 ±   0.001    B/op
    LockFreeVsResilience4jBenchmark.testOpen_myLib:gc.count                               N/A  thrpt   20   651.000            counts
    LockFreeVsResilience4jBenchmark.testOpen_myLib:gc.time                                N/A  thrpt   20   668.000                ms
    LockFreeVsResilience4jBenchmark.testOpen_rs4j                                         N/A  thrpt   20     2.813 ±   0.041  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.alloc.rate                           N/A  thrpt   20  3411.410 ±  49.410  MB/sec
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.alloc.rate.norm                      N/A  thrpt   20  1272.000 ±   0.001    B/op
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.count                                N/A  thrpt   20   714.000            counts
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.time                                 N/A  thrpt   20   728.000                ms