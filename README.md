[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/circuit-breaker/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/circuit-breaker)
[![codecov](https://codecov.io/github/dmitriy-iliyov/circuit-breaker/graph/badge.svg?token=8HOK2CVJRH)](https://codecov.io/github/dmitriy-iliyov/circuit-breaker)
[![CI](https://github.com/dmitriy-iliyov/circuit-breaker/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/circuit-breaker/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dmitriy-iliyov/circuit-breaker-starter.svg?label=maven-central&color=blue)](https://central.sonatype.com/artifact/io.github.dmitriy-iliyov/circuit-breaker-starter)
![Release](https://img.shields.io/github/release/dmitriy-iliyov/circuit-breaker)
[![GitHub Release Date](https://img.shields.io/github/release-date/dmitriy-iliyov/circuit-breaker)](https://github.com/dmitriy-iliyov/circuit-breaker/releases/latest)
![GitHub last commit](https://img.shields.io/github/last-commit/dmitriy-iliyov/circuit-breaker)

## Overview
This library provides a lightweight and flexible implementation of the [Circuit Breaker pattern](https://microservices.io/patterns/reliability/circuit-breaker.html) in Java. It is designed to improve the stability and resilience of distributed systems by preventing cascading failures when a remote service is experiencing issues.

The library is built around a state machine where state transitions are managed by highly configurable strategies. This approach allows you to precisely define the conditions for tripping the circuit breaker, the duration it stays open, and the criteria for closing it again.

A key design principle is the separation of concerns, where different observation strategies can be combined to fit specific use cases. For high-throughput, low-latency scenarios, the library provides lock-free implementations of key strategies, ensuring minimal performance overhead while maintaining resilience.

**It is not intended as a replacement for mature libraries like Resilience4j but rather as an exploratory project that is well-suited for small to medium-sized applications where its specific design trade-offs are a good fit.**

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
