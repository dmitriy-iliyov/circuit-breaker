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
- **Slow request detector** - detects slow requests and treats them as failures.
- **Lock-Free Implementations** - each strategy has a corresponding lock-free version.
- **Gradual Half-Open State** - extra state that implements a gradually increasing load in accordance with the multiplier.

## Quick Start

1. Add dependencies
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>circuit-breaker-starter</artifactId>
      <version>1.0.1</version>
  </dependency>

  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-aop</artifactId>
  </dependency>
```
`spring-boot-starter-aop` is required: the `@CircuitBreaker` annotation is handled by an AspectJ aspect, and the starter does not bring AspectJ transitively. Without it the application fails on startup with `NoClassDefFoundError: org/aspectj/lang/ProceedingJoinPoint`. 

2. Enable circuit breaker support
```java
@SpringBootApplication
@EnableCircuitBreaker
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

3. Create circuit breaker as Bean
```java
@Bean
public CircuitBreaker circuitBreaker(CircuitBreakerFactory circuitBreakerFactory) {
  CircuitBreakerConfiguration configuration = CircuitBreakerConfiguration.builder()
          .name("exampleCircuitBreaker")
          .observableExceptions(Set.of(SpecificBusinessException.class))
          .ignorableExceptions(Set.of(IllegalArgumentException.class))
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
  return circuitBreakerFactory.create(configuration);
}
```

4. Use in service
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

Environment: GitHub Actions `ubuntu-latest` runner (4 vCPU), Java 21 (Temurin).
Benchmarks run with `-t 4`; half-open contention groups use `-tg 1,3`: one thread keeps tripping the breaker
while three threads probe it. Resilience4j is configured with `writableStackTraceEnabled(false)`, so none of
the compared rejections except Failsafe's capture a stack trace.

Summary:
- **Closed state**: the sync version is 2–2.3x faster than Failsafe and allocates 80 vs 456 B/op. The lock-free version is 3–3.6x slower than
  Resilience4j with the same allocation.
- **Open state**: 55–60x faster than Failsafe and 11–15x faster than Resilience4j.
- **Half-open contention**: throughput is dominated by rejections, so the picture follows the open state:
  ~50x Failsafe and ~15x Resilience4j. The lock-free result is unstable in the GC-profiled run.

Sync version vs Failsafe:

    Benchmark                                                                     (loopLimit)    Mode  Cnt   Score   Error   Units
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention                                   N/A   thrpt   20   1.563 ± 0.004  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerOpener_failsafe            N/A   thrpt   20   0.386 ± 0.002  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerProber_failsafe            N/A   thrpt   20   1.176 ± 0.003  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention                                  N/A   thrpt   20  81.769 ± 0.276  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerOpener_myLibSync          N/A   thrpt   20  20.425 ± 0.086  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerProber_myLibSync          N/A   thrpt   20  61.345 ± 0.229  ops/us
    SyncVsFailsafeBenchmark.testClosed_failsafe                                           100   thrpt   20   4.314 ± 0.156  ops/us
    SyncVsFailsafeBenchmark.testClosed_myLibSync                                          100   thrpt   20   9.993 ± 0.218  ops/us
    SyncVsFailsafeBenchmark.testOpen_failsafe                                             N/A   thrpt   20   1.456 ± 0.021  ops/us
    SyncVsFailsafeBenchmark.testOpen_myLibSync                                            N/A   thrpt   20  86.790 ± 0.337  ops/us

Sync version vs Failsafe (with GC allocation):

    Benchmark                                                                     (loopLimit)    Mode  Cnt     Score    Error   Units
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention                                   N/A   thrpt   20     1.593 ±  0.056  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerOpener_failsafe            N/A   thrpt   20     0.391 ±  0.013  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:breakerProber_failsafe            N/A   thrpt   20     1.203 ±  0.043  ops/us
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.alloc.rate                     N/A   thrpt   20  1675.957 ± 58.152  MB/sec
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.alloc.rate.norm                N/A   thrpt   20  1103.224 ±  2.975    B/op
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.count                          N/A   thrpt   20  1035.000           counts
    SyncVsFailsafeBenchmark.failsafe_halfOpenContention:gc.time                           N/A   thrpt   20   694.000               ms
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention                                  N/A   thrpt   20    80.698 ±  0.453  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerOpener_myLibSync          N/A   thrpt   20    20.270 ±  0.061  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:breakerProber_myLibSync          N/A   thrpt   20    60.428 ±  0.403  ops/us
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.alloc.rate                    N/A   thrpt   20  4305.153 ± 25.050  MB/sec
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.alloc.rate.norm               N/A   thrpt   20    55.970 ±  0.009    B/op
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.count                         N/A   thrpt   20  1653.000           counts
    SyncVsFailsafeBenchmark.myLibSync_halfOpenContention:gc.time                          N/A   thrpt   20  1156.000               ms
    SyncVsFailsafeBenchmark.testClosed_failsafe                                           100   thrpt   20     4.846 ±  0.064  ops/us
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.alloc.rate                             100   thrpt   20  2106.572 ± 27.997  MB/sec
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.alloc.rate.norm                        100   thrpt   20   456.000 ±  0.001    B/op
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.count                                  100   thrpt   20  1177.000           counts
    SyncVsFailsafeBenchmark.testClosed_failsafe:gc.time                                   100   thrpt   20   799.000               ms
    SyncVsFailsafeBenchmark.testClosed_myLibSync                                          100   thrpt   20     9.673 ±  1.254  ops/us
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.alloc.rate                            100   thrpt   20   740.245 ± 95.530  MB/sec
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.alloc.rate.norm                       100   thrpt   20    80.268 ±  0.046    B/op
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.count                                 100   thrpt   20   504.000           counts
    SyncVsFailsafeBenchmark.testClosed_myLibSync:gc.time                                  100   thrpt   20   318.000               ms
    SyncVsFailsafeBenchmark.testOpen_failsafe                                             N/A   thrpt   20     1.531 ±  0.002  ops/us
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.alloc.rate                               N/A   thrpt   20  1576.197 ±  2.324  MB/sec
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.alloc.rate.norm                          N/A   thrpt   20  1080.001 ±  0.001    B/op
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.count                                    N/A   thrpt   20  1074.000           counts
    SyncVsFailsafeBenchmark.testOpen_failsafe:gc.time                                     N/A   thrpt   20   720.000               ms
    SyncVsFailsafeBenchmark.testOpen_myLibSync                                            N/A   thrpt   20    82.550 ±  0.338  ops/us
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.alloc.rate                              N/A   thrpt   20  3147.554 ± 12.961  MB/sec
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.alloc.rate.norm                         N/A   thrpt   20    40.000 ±  0.001    B/op
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.count                                   N/A   thrpt   20  1462.000           counts
    SyncVsFailsafeBenchmark.testOpen_myLibSync:gc.time                                    N/A   thrpt   20   943.000               ms

Lock-free version vs Resilience4j:

    Benchmark                                                                                     (loopLimit)    Mode  Cnt   Score   Error   Units
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention                                      N/A   thrpt   20  97.453 ± 0.446  ops/us
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention:breakerOpener_myLibLockFree          N/A   thrpt   20  24.326 ± 0.098  ops/us
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention:breakerProber_myLibLockFree          N/A   thrpt   20  73.127 ± 0.356  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention                                               N/A   thrpt   20   6.479 ± 0.038  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerOpener_rs4j                            N/A   thrpt   20   1.595 ± 0.013  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerProber_rs4j                            N/A   thrpt   20   4.884 ± 0.030  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_myLibLockFree                                              100   thrpt   20   2.406 ± 0.019  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_rs4j                                                       100   thrpt   20   7.433 ± 0.029  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_myLibLockFree                                                N/A   thrpt   20  99.322 ± 0.589  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_rs4j                                                         N/A   thrpt   20   8.722 ± 0.286  ops/us

Lock-free version vs Resilience4j (with GC allocation):

    Benchmark                                                                                     (loopLimit)    Mode  Cnt     Score      Error   Units
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention                                      N/A   thrpt   20    50.803 ±   27.160  ops/us
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention:breakerOpener_myLibLockFree          N/A   thrpt   20    12.626 ±    6.847  ops/us
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention:breakerProber_myLibLockFree          N/A   thrpt   20    38.177 ±   20.313  ops/us
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention:gc.alloc.rate                        N/A   thrpt   20  2707.758 ± 1450.058  MB/sec
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention:gc.alloc.rate.norm                   N/A   thrpt   20    55.876 ±    0.102    B/op
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention:gc.count                             N/A   thrpt   20  1200.000             counts
    LockFreeVsResilience4jBenchmark.myLibLockFree_halfOpenContention:gc.time                              N/A   thrpt   20   759.000                 ms
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention                                               N/A   thrpt   20     5.580 ±    0.013  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerOpener_rs4j                            N/A   thrpt   20     1.376 ±    0.004  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:breakerProber_rs4j                            N/A   thrpt   20     4.205 ±    0.010  ops/us
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.alloc.rate                                 N/A   thrpt   20  4265.077 ±  231.067  MB/sec
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.alloc.rate.norm                            N/A   thrpt   20   801.711 ±   42.368    B/op
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.count                                      N/A   thrpt   20  1636.000             counts
    LockFreeVsResilience4jBenchmark.rs4j_halfOpenContention:gc.time                                       N/A   thrpt   20  1043.000                 ms
    LockFreeVsResilience4jBenchmark.testClosed_myLibLockFree                                              100   thrpt   20     1.803 ±    0.005  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_myLibLockFree:gc.alloc.rate                                100   thrpt   20   137.447 ±    0.407  MB/sec
    LockFreeVsResilience4jBenchmark.testClosed_myLibLockFree:gc.alloc.rate.norm                           100   thrpt   20    79.993 ±    0.006    B/op
    LockFreeVsResilience4jBenchmark.testClosed_myLibLockFree:gc.count                                     100   thrpt   20    94.000             counts
    LockFreeVsResilience4jBenchmark.testClosed_myLibLockFree:gc.time                                      100   thrpt   20    58.000                 ms
    LockFreeVsResilience4jBenchmark.testClosed_rs4j                                                       100   thrpt   20     6.465 ±    0.120  ops/us
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.alloc.rate                                         100   thrpt   20   466.894 ±   57.296  MB/sec
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.alloc.rate.norm                                    100   thrpt   20    75.996 ±   10.688    B/op
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.count                                              100   thrpt   20   317.000             counts
    LockFreeVsResilience4jBenchmark.testClosed_rs4j:gc.time                                               100   thrpt   20   185.000                 ms
    LockFreeVsResilience4jBenchmark.testOpen_myLibLockFree                                                N/A   thrpt   20    82.722 ±    0.243  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_myLibLockFree:gc.alloc.rate                                  N/A   thrpt   20  3154.033 ±    9.600  MB/sec
    LockFreeVsResilience4jBenchmark.testOpen_myLibLockFree:gc.alloc.rate.norm                             N/A   thrpt   20    40.000 ±    0.001    B/op
    LockFreeVsResilience4jBenchmark.testOpen_myLibLockFree:gc.count                                       N/A   thrpt   20  1321.000             counts
    LockFreeVsResilience4jBenchmark.testOpen_myLibLockFree:gc.time                                        N/A   thrpt   20   746.000                 ms
    LockFreeVsResilience4jBenchmark.testOpen_rs4j                                                         N/A   thrpt   20     5.432 ±    0.503  ops/us
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.alloc.rate                                           N/A   thrpt   20  3339.853 ±  528.243  MB/sec
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.alloc.rate.norm                                      N/A   thrpt   20   639.989 ±   42.754    B/op
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.count                                                N/A   thrpt   20  1461.000             counts
    LockFreeVsResilience4jBenchmark.testOpen_rs4j:gc.time                                                 N/A   thrpt   20   902.000                 ms
