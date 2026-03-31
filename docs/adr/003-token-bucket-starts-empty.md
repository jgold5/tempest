# ADR 003: Token Bucket Starts Empty

## Status
Accepted

## Context
The token bucket implementation must decide its initial token count -- either full (using `maxTokens`)
or empty (zero tokens).

## Decision
Initialize `currentTokens` to `0` rather than `maxTokens`.

## Rationale
Starting full allows an immediate burst of up to `maxTokens` requests before rate limitiing
kicks in. For a load tester, this produces inaccurate results -- the measured RPS in the first
second is artificially inflated and not representative of the configured target.

Starting empty ensures requests are paced from the first acquisition, giving consistent throughput from
the start of the test.

## Consequences
- The first request will always block briefly until the first token is available.
- Burst behavior at startup is intentionally eliminated.