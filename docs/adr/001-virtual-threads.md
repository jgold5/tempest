# ADR 001: Virtual Threads for Concurrency

## Status
Accepted

## Context
Tempest needs to dispatch many concurrent HTTP requests while maintaining a target RPS. A
traditional fixed thread pool would require careful sizing -- too small and requests queue up,
too large and memory/context switching overhead becomes a problem.

## Decision
Use Java 21 virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`, spawning one
virtual thread per request.

## Rationale
- Virtual threads are cheap enough to create per-request without pooling -- each one uses a
small initial stack that grows on demand rather than reserving ~1MB upfront.
- When a virtual thread blocks on I/O (waiting for an HTTP response), the JVM parks it and
schedules another, keeping platform threads fully utilized.
- Eliminates thread pool sizing as a configuration concern -- the JVM handles scheduling
automatically.

## Consequences
- Requires Java 21 minimum.
- Code using `synchronized` blocks can cause virtual thread pinning -- a known limitation to
be addressed in future optimizations.