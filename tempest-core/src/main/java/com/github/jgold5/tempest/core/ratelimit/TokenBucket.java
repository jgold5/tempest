package com.github.jgold5.tempest.core.ratelimit;

/**
 * A thread-safe token bucket rate limiter.
 *
 * <p>Tokens are added continuously at a rate derived from {@code targetRps}, and each call to
 * {@link #acquire()} consumes one token. If no token is available, the caller blocks until refill
 * brings the bucket to at least one token.
 *
 * <p>Refill is computed lazily on each {@link #acquire()} call using nanosecond-precision elapsed
 * time, avoiding a background thread.
 *
 * <p>This implementation uses {@code synchronized}/{@code wait} for mutual exclusion. All state
 * mutations occur within the monitor, so instances may be shared across virtual threads without
 * additional synchronization.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * TokenBucket bucket = new TokenBucket(10.0, 100.0); // burst of 10, 100 req/s
 * bucket.acquire(); // blocks if rate limit is exceeded
 * }</pre>
 */
public class TokenBucket {

  private final double maxTokens;
  private double currentTokens;
  private final double refillRatePerNano;
  private long lastRefillTime;

  /**
   * Creates a new {@code TokenBucket}.
   *
   * @param maxTokens the bucket capacity; also the maximum burst size. Must be positive.
   * @param targetRps the target throughput in requests per second. Must be positive
   */
  public TokenBucket(double maxTokens, double targetRps) {
    this.maxTokens = maxTokens;
    this.currentTokens = maxTokens;
    this.refillRatePerNano = targetRps / 1_000_000_000;
    this.lastRefillTime = System.nanoTime();
  }

  /**
   * Acquires a single token, blocking until one becomes available.
   *
   * <p>On each invocation the bucket is refilled based on elapsed wall-clock time since the last
   * refill. If the bucket still holds fewer than one token after refill, the thread waits up to 1
   * ms and retries until a token is available.
   *
   * <p>The bucket is started full, so the first {@code maxTokens} calls return immediately.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  public void acquire() throws InterruptedException {
    synchronized (this) {
      long now = System.nanoTime();
      long elapsed = now - lastRefillTime;
      double newTokens = elapsed * refillRatePerNano;
      currentTokens = Math.min(maxTokens, newTokens + currentTokens);
      lastRefillTime = now;
      while (currentTokens < 1.0) {
        wait(1);
        now = System.nanoTime();
        elapsed = now - lastRefillTime;
        newTokens = elapsed * refillRatePerNano;
        currentTokens = Math.min(maxTokens, newTokens + currentTokens);
        lastRefillTime = now;
      }
      currentTokens -= 1;
    }
  }
}
