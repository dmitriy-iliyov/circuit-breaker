[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/circuit-breaker/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/circuit-breaker)
[![codecov](https://codecov.io/github/dmitriy-iliyov/circuit-breaker/graph/badge.svg?token=8HOK2CVJRH)](https://codecov.io/github/dmitriy-iliyov/circuit-breaker)
[![CI](https://github.com/dmitriy-iliyov/circuit-breaker/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/circuit-breaker/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dmitriy-iliyov/circuit-breaker-starter.svg?label=maven-central&color=blue)](https://central.sonatype.com/artifact/io.github.dmitriy-iliyov/circuit-breaker-starter)
![Release](https://img.shields.io/github/release/dmitriy-iliyov/circuit-breaker)
[![GitHub Release Date](https://img.shields.io/github/release-date/dmitriy-iliyov/circuit-breaker)](https://github.com/dmitriy-iliyov/circuit-breaker/releases/latest)
![GitHub last commit](https://img.shields.io/github/last-commit/dmitriy-iliyov/circuit-breaker)

## Overview
This library is an exploratory implementation of the [Circuit Breaker Pattern](https://microservices.io/patterns/reliability/circuit-breaker.html) in Java, designed to improve system resilience by preventing cascading failures. It is not intended as a replacement for mature libraries like Resilience4j but serves as a research project that may be suitable for small to medium-sized applications where its specific design trade-offs are a good fit.

The core of the library is a state machine (`CLOSE`, `OPEN`, `HALF_OPEN`) where transitions are managed by highly configurable and pluggable observation strategies. This design allows for precise control over the circuit's behavior and enables high-performance, low-latency scenarios through lock-free strategy implementations, ensuring minimal overhead while maintaining resilience.

## Key Features
- **State-driven Architecture**: Follows a clear state machine (`CLOSE`, `OPEN`, `HALF_OPEN`) for predictable behavior.
- **Centralized Management**: A `CircuitBreakerRegistry` to manage and access multiple circuit breaker instances in your application.
- **Modular Observation Strategies**: Customize the behavior of each state with pluggable strategies.
    - **Sliding Window Close State Strategy**: Monitors recent requests to decide when to trip the circuit based on failure rate or count.
    - **Time-based Open State Strategy**: Keeps the circuit open for a configurable duration, allowing the downstream service time to recover.
    - **Count-based Half-Open State Strategy**: Allows a limited number of trial requests to pass through to test if the downstream service has recovered.
- **High-Performance Lock-Free Implementations**: Each core strategy has a corresponding lock-free version designed for high-concurrency environments, minimizing contention and maximizing throughput.
- **Rich Configuration**: A fluent builder API for creating configurations, allowing you to tune every aspect of the circuit breaker's behavior.

## Quick Start

## Benchmarks
