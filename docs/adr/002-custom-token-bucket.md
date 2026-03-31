# ADR 002: Custom Token Bucket for Rate Limiting

## Status
Accepted

## Context
Tempest needs to pace HTTP request dispatch at a configuratble target RPS.
Without rate limiting, requests fire as fast as the system allows, making
it impossible to simulate realistic load profiles.

## Decision
Implement a custom token bucket rate limiter rather than using Guava's `RateLimiter`

## Rationale
- A custom implementation allows future optimization, such as swapping `synchronized`/`wait` for
`ReentrantLock` to eliminate virtual thread pinning.
- Guava's `RateLimiter` is a black box that offers no path to that opimization.

## Consequences
- The implementation must be carefully tested to ensure accuracy at the target RPS.
- A future optimization task exists to replace `synchronized`/`wait` with `ReentrantLock`.
