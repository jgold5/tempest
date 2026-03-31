# Tempest

A command-line HTTP load testing tool built with Java 21 virtual threads.

## Features

- Configurable target RPS with token bucket rate limiting
- Accurate latency percentiles via HdrHistogram (p50, p95, p99)
- Warmup period support to exclude startup noise from results
- Single distributable fat jar -- no installation required

## Requirements

- Java 21+

## Usage
```bash
java -jar tempest-cli-all.jar --rps --duration [--warmup ]
```

### Example
```bash
java -jar tempest-cli-all.jar http://localhost:8080 --rps 100 --duration 30 --warmup 5
```
```
Starting load test: 100.0 RPS for 30s
Load test complete

Results:
  Total requests : 2500
  Total errors   : 0
  p50            : 3ms
  p95            : 8ms
  p99            : 14ms
```