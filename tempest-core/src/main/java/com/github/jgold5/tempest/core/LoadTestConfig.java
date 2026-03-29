package com.github.jgold5.tempest.core;

import java.time.Duration;

/**
 * Configuration for a load test run.
 *
 * <p>Use the {@link Builder} to construct instances:
 *
 * <pre>{@code
 * LoadTestConfig config = new LoadTestConfig.Builder()
 *     .targetRps(100)
 *     .duration(Duration.ofSeconds(30))
 *     .warmupDuration(Duration.ofSeconds(5))
 *     .rampUpDuration(Duration.ofSeconds(10))
 *     .build();
 * }</pre>
 *
 * <p>Only {@code targetRps} and {@code duration} are required. {@code warmupDuration} and {@code
 * rampupDuration} default to {@link Duration#ZERO} if not set.
 */
public class LoadTestConfig {
  private final double targetRps;
  private final Duration duration;
  private final Duration warmupDuration;
  private final Duration rampupDuration;

  private LoadTestConfig(Builder builder) {
    this.targetRps = builder.targetRps;
    this.duration = builder.duration;
    this.warmupDuration = builder.warmupDuration;
    this.rampupDuration = builder.rampupDuration;
  }

  /**
   * Builder for {@link LoadTestConfig}.
   *
   * <p>{@code targetRps} and {@code duration} are required. All other fields are optional.
   */
  public static class Builder {
    private double targetRps;
    private Duration duration;
    private Duration warmupDuration = Duration.ZERO;
    private Duration rampupDuration = Duration.ZERO;

    /**
     * Sets the target request rate.
     *
     * @param targetRps requests per second to sustain during the test. Must be positive.
     * @return this builder
     */
    public Builder targetRps(double targetRps) {
      this.targetRps = targetRps;
      return this;
    }

    /**
     * Sets the total test duration, excluding warmup and rampup.
     *
     * @param duration how long to run the test at full load. Must be positive.
     * @return this builder
     */
    public Builder duration(Duration duration) {
      this.duration = duration;
      return this;
    }

    /**
     * Sets the warmup duration.
     *
     * <p>During warmup, requests are sent at {@code targetRps} but results are excluded from
     * metrics. Useful for allowing the target server to reach a steady state before measurement
     * begins. Defaults to {@link Duration#ZERO}.
     *
     * @param duration warmup period length. Must not be negative.
     * @return this builder
     */
    public Builder warmupDuration(Duration duration) {
      this.warmupDuration = duration;
      return this;
    }

    /**
     * Sets the rampup duration.
     *
     * <p>During rampup, the request rate increases linearly from zero to {@code targetRps}. Useful
     * for avoiding an initial spike that could skew results or overwhelm the target. Defaults to
     * {@link Duration#ZERO}.
     *
     * @param duration rampup period length. Must not be negative.
     * @return this builder
     */
    public Builder rampupDuration(Duration duration) {
      this.rampupDuration = duration;
      return this;
    }

    /**
     * Builds a validated {@link LoadTestConfig}.
     *
     * @return a new {@code LoadTestConfig}
     * @throws IllegalArgumentException if {@code targetRps} is not positive, or {@code duration} is
     *     null, zero, or negative
     */
    public LoadTestConfig build() {
      if (targetRps <= 0) throw new IllegalArgumentException("targetRps must be > 0");
      if (duration == null || duration.isZero() || duration.isNegative())
        throw new IllegalArgumentException("duration must be > 0");
      return new LoadTestConfig(this);
    }
  }

  public double getTargetRps() {
    return targetRps;
  }

  public Duration getDuration() {
    return duration;
  }

  public Duration getWarmupDuration() {
    return warmupDuration;
  }

  public Duration getRampupDuration() {
    return rampupDuration;
  }
}
