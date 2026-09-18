# ADR 005: Virtual Threads over a Reactive Framework

## Status
Accepted

## Context
ADR 001 recorded the choice of virtual threads over a traditional fixed thread pool. A reactive
framework -- Project Reactor, RxJava, or an async HTTP client driven by callbacks -- is the other
established way to keep many requests in flight without dedicating a platform thread to each one,
and it warrants its own record.

## Decision
Use virtual threads with ordinary blocking calls rather than a reactive or callback-driven
pipeline.

## Rationale
- Both approaches solve the same problem: do not park a platform thread per in-flight request.
Virtual threads solve it in the runtime; reactive solves it by restructuring the program into
operators and callbacks.
- Tempest's measurement is inherently sequential per request -- read the clock, execute, read the
clock, record. Blocking code keeps the two `System.nanoTime()` calls adjacent to the call they
measure. In a reactive pipeline that timing has to be carried through operators, which is exactly
where latency measurement bugs hide.
- Reactive's headline benefit is end-to-end backpressure. Tempest deliberately does not want
backpressure on dispatch: the token bucket is the only thing permitted to govern send rate. A
generator that slows down because the target slowed down produces coordinated omission, hiding the
worst latencies it exists to measure.
- A virtual thread stack trace names the request path. A reactive stack trace names the scheduler.
- No framework dependency -- the concurrency comes from the platform.

## Consequences
- Blocking client libraries are a first-class choice, which is why the HTTP layer uses a
synchronous client rather than an async one.
- `synchronized` becomes a liability rather than a neutral choice, since a blocked monitor can pin
a carrier thread. See ADR 001 and ADR 002.
- The ceiling on concurrency becomes carrier threads and sockets rather than the programming model.
If a single box ever needs to multiplex far more connections than it can today, revisit.
