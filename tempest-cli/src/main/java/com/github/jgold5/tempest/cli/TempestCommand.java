package com.github.jgold5.tempest.cli;

import com.github.jgold5.tempest.core.LoadGenerator;
import com.github.jgold5.tempest.core.LoadTestConfig;
import com.github.jgold5.tempest.core.http.HttpRequestConfig;
import com.github.jgold5.tempest.core.metrics.MetricsRecorder;
import com.github.jgold5.tempest.core.metrics.MetricsSnapshot;
import java.time.Duration;
import java.util.concurrent.Callable;
import org.HdrHistogram.Histogram;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Entry point for the Tempest CLI.
 *
 * <p>Accepts a target URL as a positional argument and configures a load test via {@code --rps} and
 * {@code --duration} flags. Delegates to {@link LoadGenerator} to execute the test and prints a
 * latency summary on completion.
 *
 * <p>Intended to be invoked via the distributed fat jar:
 *
 * <pre>{@code
 * java -jar tempest-cli-all.jar <url> --rps 100 --duration 30
 * }</pre>
 */
@Command(name = "tempest", description = "HTTP load testing tool")
public class TempestCommand implements Callable<Integer> {

  @Option(names = "--rps", required = true)
  private int targetRps;

  @Option(
      names = {"--duration", "-d"},
      required = true)
  private int durationSeconds;

  @Option(names = {"--warmup", "-w"})
  private int warmupDuration;

  @Parameters(index = "0", description = "Target URL")
  private String url;

  public Integer call() {
    HttpRequestConfig requestConfig = HttpRequestConfig.get(url, null);
    LoadTestConfig loadTestConfig =
        new LoadTestConfig.Builder()
            .targetRps(targetRps)
            .duration(Duration.ofSeconds(durationSeconds))
            .warmupDuration(Duration.ofSeconds(warmupDuration))
            .build();
    MetricsRecorder metricsRecorder = new MetricsRecorder();
    LoadGenerator loadGenerator = new LoadGenerator(requestConfig, loadTestConfig, metricsRecorder);
    loadGenerator.run();
    MetricsSnapshot snapshot = metricsRecorder.getSnapshot();
    Histogram hist = snapshot.getIntervalHistogram();
    long p50 = hist.getValueAtPercentile(50.0) / 1_000_000;
    long p95 = hist.getValueAtPercentile(95.0) / 1_000_000;
    long p99 = hist.getValueAtPercentile(99.0) / 1_000_000;
    System.out.println("\nResults:");
    System.out.printf("  Total requests   : %d%n", snapshot.getTotalCount());
    System.out.printf("  Total errors     : %d%n", snapshot.getErrorCount());
    System.out.printf("  p50              : %dms%n", p50);
    System.out.printf("  p95              : %dms%n", p95);
    System.out.printf("  p99              : %dms%n", p99);
    return 0;
  }

  public static void main(String[] args) {
    System.exit(new CommandLine(new TempestCommand()).execute(args));
  }
}
