# ADR 004: HdrHistogram for Latency Measurement

## Status
Accepted

## Context
Tempest records latency for every HTTP request and must produce accurate percentile statistics
(p50, p95, p99) at the end of a test. A naive approach of storing all latency values in a list
would consume unbounded memory under high RPS.

## Decision
Use HdrHistogram (`org.HdrHistorgram`) for latency recording.

## Rationale
- **Memory efficiency** -- HdrHistogram uses fixed-size bucket-based storage regardless of
the number of recorded values. Memory usage is determined by the configured value range and precision,
not request count.
- **Accuracy** -- designed specifically for latency measurement with configurable significant digits
of precision.
- **Concurrency** -- the `Recorder`/flip pattern allows concurrent writes from virtual threads
without contention, minimizing allocations.
- Used by production-grade tools like wrk2 and the JVM itself for internal metrics.

## Consequences
- Values exceeding the configured max (`60s`) are clamped -- acceptable for a load tester where
requests exceeding 60s are effectively failures.
- `Recorder.getIntervalHistogram()` resets the histogram on each call -- callers must store the
result before querying it multiple times.