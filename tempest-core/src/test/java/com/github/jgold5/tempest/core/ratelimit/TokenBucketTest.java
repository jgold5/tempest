package com.github.jgold5.tempest.core.ratelimit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

public class TokenBucketTest {

  @Test
  void shouldRateLimitAfterBucketExhausted() throws InterruptedException {
    TokenBucket tokenBucket = new TokenBucket(5, 1);
    for (int i = 0; i < 5; i++) {
      long start = System.nanoTime();
      tokenBucket.acquire();
      long end = System.nanoTime();
      assertThat(end - start).isLessThan(Duration.ofMillis(50).toNanos());
    }
    long start = System.nanoTime();
    tokenBucket.acquire();
    long end = System.nanoTime();
    assertThat(end - start).isGreaterThan(Duration.ofMillis(50).toNanos());
  }

  @Test
  void shouldMatchIndicatedRate() throws InterruptedException {
    TokenBucket tokenBucket = new TokenBucket(1, 100);
    long start = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      tokenBucket.acquire();
    }
    long end = System.nanoTime();
    assertThat(end - start)
        .isCloseTo(Duration.ofSeconds(1).toNanos(), Percentage.withPercentage(20));
  }
}
