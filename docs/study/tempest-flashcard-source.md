---
deck: "Tempest — Java HTTP Load Testing Tool"
purpose: "Interview prep for the Tempest project bullet on Justin Gold's resume"
source_repo: "github.com/jgold5/tempest"
source_ref: "v0.1.0"
tags: [java-21, virtual-threads, concurrency, rate-limiting, hdrhistogram, load-testing, gradle, picocli, adr]
suggested_card_count: 60
card_format: "question -> answer, one idea per card"
---

# Tempest Flashcard Source

Source material for generating a study deck about Tempest, a Java 21 HTTP load
testing tool. The deck's job is to make the resume bullet defensible in an
interview: every claim on the resume should map to a card, and every card
should be answerable in 30-60 seconds out loud.

## How to build the deck from this doc

- Each `**Q.**` / `**A.**` pair below is one card. Front = the Q line, back = the A block.
- Keep answers as written where possible — they are checked against the code at
  `v0.1.0`, not paraphrased from general knowledge.
- Preserve the section name as a tag on each card (e.g. `virtual-threads`,
  `token-bucket`, `hdrhistogram`).
- Cards marked `[gap]` are known weaknesses or unimplemented pieces. Keep them
  in the deck — they are the questions most likely to get asked second.
- Cards marked `[depth]` are follow-up depth for a strong interviewer; they can
  be split into a second deck if the main deck is meant to stay short.

## The resume bullet, and what it commits to

> Java 21 tool built on virtual-thread execution, with a custom token-bucket rate
> limiter, HdrHistogram-backed latency metrics reporting p50/p95/p99, and a
> Picocli CLI distributed as a fat jar across a multi-module Gradle build.
> Tagged v0.1.0.
>
> Made and documented the architectural decisions in four ADRs: virtual threads
> over reactive frameworks for throughput-heavy workloads, a custom token bucket
> over Guava's RateLimiter, and HdrHistogram selected for its allocation
> behavior and the resulting GC pressure. Dropped GraalVM Native Image after
> Picocli's annotation reflection broke AOT compilation.

Every noun in that bullet gets at least one card: virtual threads, token bucket,
HdrHistogram, p50/p95/p99, Picocli, fat jar, multi-module Gradle, ADRs, GraalVM.

---

## Section: overview

**Q.** What is Tempest, in one sentence?

**A.** A command-line HTTP load testing tool in Java 21 that fires requests at a
configured target RPS for a configured duration, and reports p50/p95/p99 latency
plus request and error counts.

---

**Q.** What does a Tempest invocation look like?

**A.** `java -jar tempest-cli-all.jar http://localhost:8080 --rps 100 --duration 30 --warmup 5`
— positional target URL, then target RPS, test duration in seconds, and an
optional warmup in seconds.

---

**Q.** What are the modules in the Tempest build, and what does each own?

**A.** Two Gradle modules. `tempest-core` owns the load generation engine — the
load generator, HTTP executor, token bucket, and metrics. `tempest-cli` owns
argument parsing and output formatting, depends on core, and is the module that
produces the distributable fat jar.

---

**Q.** Why split core from CLI at all for a tool this size?

**A.** The CLI is one of several possible front ends. Keeping the engine free of
Picocli annotations and `System.out` means the same engine can later be driven by
a config file, a library caller, or a distributed runner without touching the
load generation code. It also keeps the test surface honest — core is tested
without going through argument parsing.

---

**Q.** Walk through the request path for a single Tempest request.

**A.** `TempestCommand` parses the flags and builds an `HttpRequestConfig` and a
`LoadTestConfig`, then hands both to `LoadGenerator`. `LoadGenerator` loops until
the duration elapses; each iteration calls `TokenBucket.acquire()` to pace, then
submits a task to a virtual-thread-per-task executor. That task calls
`HttpRequestExecutor.execute()`, which runs the OkHttp call and times it, and the
resulting `HttpRequestResult` is written to `MetricsRecorder` — unless the test
is still inside the warmup window. At the end the CLI pulls a `MetricsSnapshot`
and prints percentiles off the histogram.

---

**Q.** What are the core types in `tempest-core` and their roles?

**A.** `LoadGenerator` (the dispatch loop), `LoadTestConfig` (builder-validated
RPS/duration/warmup/rampup), `TokenBucket` (pacing), `HttpRequestConfig` /
`HttpRequestExecutor` / `HttpRequestResult` (the HTTP layer and its per-request
outcome), and `MetricsRecorder` / `MetricsSnapshot` (recording and the immutable
read side).

---

## Section: virtual-threads

**Q.** How does Tempest get its concurrency?

**A.** `Executors.newVirtualThreadPerTaskExecutor()` — one Java 21 virtual thread
per request, no pooling. The dispatch loop submits a task per token acquired and
never waits for the response.

---

**Q.** Why virtual threads instead of a fixed platform thread pool?

**A.** ADR 001: pool sizing becomes a tuning problem you cannot get right for all
targets. Too small and requests queue behind the pool instead of going out at the
target rate; too large and you pay memory and context-switching overhead. Virtual
threads make thread count a non-decision — the JVM schedules them onto a small
carrier pool.

---

**Q.** Why is a virtual thread cheap enough to create per request?

**A.** It does not reserve a ~1MB stack up front the way a platform thread does.
Its stack lives on the heap and grows on demand, so creating one per request is
closer in cost to allocating an object than to creating an OS thread.

---

**Q.** What happens when a Tempest request blocks waiting on the HTTP response?

**A.** The JVM parks (unmounts) that virtual thread from its carrier thread and
schedules another one, so the platform threads stay busy doing real work instead
of sitting in a blocked read. That is what lets a handful of carrier threads
carry thousands of in-flight requests.

---

**Q.** Why virtual threads over a reactive framework for this workload?

**A.** Load generation is throughput-heavy, blocking-I/O-bound code. Virtual
threads get the same scheduling benefit while the code stays straight-line and
imperative — you keep `long start = nanoTime(); execute(); long end = nanoTime();`
instead of threading latency measurement through a callback or operator chain.
Reactive buys backpressure plumbing this tool does not need, at the cost of
debuggability and a stack-trace story.

---

**Q.** What is virtual thread pinning, and why does it appear in the Tempest ADRs?

**A.** A virtual thread is pinned when it cannot unmount from its carrier thread
— classically while inside a `synchronized` block that blocks. A pinned carrier
thread is unavailable to other virtual threads, so enough pinning collapses the
concurrency win back to the carrier pool size. ADR 001 records it as a known
limitation, and ADR 002 keeps the token bucket custom specifically so the
`synchronized`/`wait` inside it can be swapped for a `ReentrantLock` later.
`[depth]`

---

**Q.** Where does Tempest currently call the `synchronized` token bucket from, and
why does that soften the pinning concern today?

**A.** `TokenBucket.acquire()` is called from the dispatch loop in
`LoadGenerator.run()`, not from the per-request virtual threads. The blocking
`synchronized`/`wait` therefore sits on the calling thread rather than on the
virtual threads doing the I/O. The pinning risk is real but latent — it arrives
the moment the bucket is shared across the request threads. `[depth]`

---

**Q.** What is the minimum Java version for Tempest and why?

**A.** Java 21 — virtual threads are a final feature there
(`Executors.newVirtualThreadPerTaskExecutor()`), and that is the floor ADR 001
accepts as the cost of the design. CI builds on Temurin 21.

---

## Section: token-bucket

**Q.** What does the token bucket do in Tempest?

**A.** It paces dispatch to the configured target RPS. Tokens accrue over time at
`targetRps` per second; every request must `acquire()` one token before it is
submitted, so the send rate cannot exceed the configured rate.

---

**Q.** How does Tempest's token bucket refill, and what does it deliberately not do?

**A.** Refill is computed lazily inside `acquire()` from elapsed `System.nanoTime()`
since the last refill — `elapsedNanos * refillRatePerNano`, capped at `maxTokens`.
There is deliberately no background refill thread; nothing runs between calls.

---

**Q.** Why compute refill lazily instead of running a scheduled refill thread?

**A.** A refill thread costs a thread plus timer granularity and adds a second
source of truth for the clock. Computing from elapsed nanos on demand makes the
bucket exact to the nanosecond at the moment it matters — the acquisition — and
leaves nothing running when the tool is idle.

---

**Q.** Why did Tempest implement its own token bucket instead of using Guava's `RateLimiter`?

**A.** ADR 002: Guava's `RateLimiter` is a black box with no path to the
optimization the project knows it wants — replacing `synchronized`/`wait` with
`ReentrantLock` to avoid pinning virtual threads. Owning the implementation keeps
that door open. The accepted cost is that correctness at the target rate is now
the project's own burden, which is why the bucket has a rate-accuracy test.

---

**Q.** Does the Tempest token bucket start full or empty, and why?

**A.** Empty — `currentTokens` is initialized to `0`. ADR 003. Starting full would
let up to `maxTokens` requests fire instantly before rate limiting engages,
inflating measured RPS in the first second and making the first slice of the test
unrepresentative of the configured target.

---

**Q.** What is the cost of starting the bucket empty?

**A.** The very first request always blocks briefly waiting for the first token,
and burst behavior at startup is gone by design. For a load tester that is the
right trade: you want the configured rate, not a spike. `[depth]`

---

**Q.** What does `acquire()` do when no token is available?

**A.** It loops: `wait(1)` — a one millisecond timed wait — then recomputes the
refill and re-checks. It only decrements and returns once at least one full token
is available. `InterruptedException` propagates to the caller.

---

**Q.** What capacity does `LoadGenerator` give the token bucket, and what does that imply?

**A.** It constructs `new TokenBucket(targetRps, targetRps)` — capacity equals the
target rate, so the bucket can bank at most one second of idle time. If the
dispatch loop stalls for longer than a second, the missed requests are not made
up. That is a deliberate smoothness-over-catch-up choice worth being able to
defend. `[depth]`

---

**Q.** How is token bucket accuracy tested?

**A.** Two tests: one asserts `acquire()` blocks until a token is available, and
one asserts the observed rate matches the configured rate. Since ADR 002 takes on
the burden of a hand-rolled limiter, rate accuracy is the test that justifies the
decision.

---

## Section: hdrhistogram

**Q.** Why HdrHistogram instead of collecting latencies into a list?

**A.** ADR 004: a list of every latency is unbounded memory — at high RPS over a
long test that is millions of boxed longs and the GC pressure that comes with
them. HdrHistogram's storage is fixed-size bucketed counts sized by the
configured value range and precision, not by the number of recorded values.

---

**Q.** In one line, how does HdrHistogram represent latencies?

**A.** As counts in logarithmically-spaced buckets subdivided linearly, sized for
a configured maximum value and a configured number of significant digits — so
memory is a function of range and precision, and recording a value is an index
computation plus an increment. `[depth]`

---

**Q.** How is the recorder configured in Tempest?

**A.** `new Recorder(TimeUnit.SECONDS.toNanos(60), 3)` — a maximum trackable value
of 60 seconds, in nanoseconds, with 3 significant digits of precision.

---

**Q.** What happens to a request slower than 60 seconds?

**A.** Its value is clamped to the configured maximum. ADR 004 accepts this: in a
load test, a request taking over 60 seconds is effectively a failure, and it is
already counted as such if it errored out.

---

**Q.** What is the `Recorder`/flip pattern and why does Tempest use it?

**A.** `Recorder` holds two internal histograms and atomically swaps them when you
call `getIntervalHistogram()`. Writers keep recording into the active one with no
lock contention while the caller reads the flipped-out one. That is what lets many
virtual threads record concurrently without synchronizing, and without allocating
per-read.

---

**Q.** What is the footgun in `Recorder.getIntervalHistogram()`?

**A.** It resets as it returns — each call hands back only the interval since the
previous call. Calling it twice and expecting the same data gives you an empty
second result. Tempest handles this by capturing it once into an immutable
`MetricsSnapshot` and querying the stored histogram for all three percentiles.

---

**Q.** What is `MetricsSnapshot` and why does it exist?

**A.** An immutable capture of one reporting interval — timestamp, total count,
error count, and the flipped-out interval histogram. It exists precisely because
the recorder is destructive on read: the snapshot is the safe, re-queryable copy.

---

**Q.** What are the three latency percentiles Tempest reports, and why percentiles
rather than an average?

**A.** p50, p95, p99. An average hides the tail — a mean of 5ms is compatible with
1% of users waiting two seconds. Percentiles are what SLOs are written against,
and the tail is where queueing, GC pauses, and connection limits actually show up.

---

**Q.** Tempest records latency in nanoseconds but prints milliseconds. Where does
that conversion happen?

**A.** In the CLI — `getValueAtPercentile(...) / 1_000_000`. The engine keeps full
nanosecond resolution end to end; only the presentation layer rounds. `[depth]`

---

**Q.** What else does `MetricsRecorder` track besides latency?

**A.** Two `AtomicLong` counters — total requests recorded and errors — incremented
per result. A result counts as an error when it is not a 2xx, including transport
failures that never got a response.

---

**Q.** `[gap]` What is inconsistent about `MetricsRecorder.getSnapshot()` today?

**A.** The histogram is per-interval (it resets on every call) but `totalCount` and
`errorCount` are cumulative and never reset. On a single end-of-run snapshot the
two agree; the moment live interval reporting is added, the counts and the
histogram would describe different windows. Fixing it means either resetting the
counters on flip or tracking interval deltas.

---

## Section: http-layer

**Q.** What HTTP client does Tempest use, and how is it configured?

**A.** OkHttp, with a single shared `OkHttpClient` built once per
`HttpRequestExecutor` — 10 second connect, read, and write timeouts by default,
all three overridable through a constructor.

---

**Q.** Why is the `OkHttpClient` created once rather than per request?

**A.** It carries the connection pool and dispatcher. Creating one per request
would throw away connection reuse and make the tool measure TCP and TLS setup on
every call instead of the endpoint's actual response time. `[depth]`

---

**Q.** How does Tempest time a request?

**A.** `System.nanoTime()` immediately before `call.execute()` and again once the
response comes back (or once the `IOException` is caught), with the difference
stored on the `HttpRequestResult`. `nanoTime` is used rather than
`currentTimeMillis` because it is monotonic and not subject to clock adjustments.

---

**Q.** What error types does Tempest distinguish, and why bother?

**A.** `NONE`, `HTTP_ERROR`, `CONNECTION_REFUSED`, `CONNECTION_TIMEOUT`,
`READ_TIMEOUT`, `UNKNOWN`. They mean different things about the target: a 500 is
the service failing under load, connection refused means nothing is listening at
all, and a read timeout means the server accepted the connection but could not
answer in time — which is the classic saturation signature. Collapsing them into
one "error" number would throw away the diagnosis.

---

**Q.** How does Tempest classify a transport failure?

**A.** By exception type: `ConnectException` maps to `CONNECTION_REFUSED`;
`SocketTimeoutException` maps to `CONNECTION_TIMEOUT` if the message mentions
connect, otherwise `READ_TIMEOUT`; anything else falls through to `UNKNOWN`, which
is the one case that carries the exception message along with it.

---

**Q.** `[gap]` What is fragile about that classification?

**A.** The connect-vs-read timeout split keys off the text of the exception
message. That is a string match against a library's wording — it works today and
would break silently on an OkHttp change. The honest answer in an interview is
that it should key off which phase the call was in, tracked explicitly.

---

**Q.** What HTTP methods can `HttpRequestConfig` express, and how is one built?

**A.** GET, POST, PUT, PATCH, DELETE, through static factories —
`HttpRequestConfig.get(url, headers)`, `.post(url, headers, body)`, and so on.
The constructor is private, so a config cannot be built in a state its factory
does not allow.

---

**Q.** `[gap]` What can the CLI actually drive today versus what core supports?

**A.** Core supports all five methods with custom headers and bodies; the CLI
hardcodes `HttpRequestConfig.get(url, null)`. Method, header, and body flags are
unbuilt — a deliberate v0.1.0 scope cut, not an oversight to be surprised by.

---

**Q.** How is the HTTP layer tested without a live server?

**A.** WireMock — stubbed endpoints cover the OK response, a 500 mapping to
`HTTP_ERROR`, connection refused with nothing listening, and an unresponsive
server producing a read timeout. AssertJ for assertions, JUnit 5 as the platform.

---

## Section: load-generator

**Q.** How does the dispatch loop decide when to stop?

**A.** It compares elapsed `System.nanoTime()` against the configured duration in
nanos at the top of each iteration, so the loop stops issuing new requests once
the window closes.

---

**Q.** Why does `LoadGenerator` submit the request instead of executing it inline?

**A.** Because an inline call would make the dispatch rate a function of the
target's response time — a slow server would automatically reduce the offered
load, which is exactly the bias you are trying to avoid in a load test.
Submitting to the virtual-thread executor keeps the send rate pinned to the token
bucket regardless of how slowly responses come back. `[depth]`

---

**Q.** What is coordinated omission, and how does Tempest's design relate to it?

**A.** Coordinated omission is the measurement bug where a load generator stops
sending while the target is slow, so the worst latencies are never recorded and
the results flatter the system. Tempest's open-loop dispatch — pace on the token
bucket, hand each request to its own virtual thread, never wait — is the
structural defense against it: a stalled target does not throttle the generator.
`[depth]`

---

**Q.** How does warmup work?

**A.** Requests are sent at full rate during the warmup window, but their results
are not recorded — `LoadGenerator` checks elapsed time against
`warmupDuration` before calling `recordResult`. The point is to let the target
reach steady state (JIT compiled, caches warm, pools filled) before measurement
starts.

---

**Q.** `[gap]` Is warmup time added to the test duration or taken out of it?

**A.** Taken out of it. The loop runs for `duration` total and warmup is a prefix
of that, so `--duration 30 --warmup 5` measures 25 seconds, not 30. Defensible,
but worth being precise about rather than being caught by it.

---

**Q.** `[gap]` What does `rampupDuration` do?

**A.** Nothing yet. `LoadTestConfig` accepts and validates it, and its Javadoc
describes the intent — increase the rate linearly from zero to `targetRps` to
avoid an initial spike — but `LoadGenerator` never reads it, and the CLI has no
flag for it. It is a declared seam for the next iteration, and calling it done
would be overclaiming.

---

**Q.** How does Tempest handle being interrupted mid-test?

**A.** The `InterruptedException` from `acquire()` is caught, the executor is shut
down with `shutdownNow()`, the thread's interrupt flag is restored via
`Thread.currentThread().interrupt()`, and the interruption is logged. Restoring
the flag matters — swallowing it hides the cancellation from every caller up the
stack.

---

**Q.** Why is the executor created in a try-with-resources block?

**A.** `ExecutorService` is `AutoCloseable` as of Java 19, and closing it blocks
until submitted tasks finish. That is what makes in-flight requests at the end of
the window get recorded rather than dropped when the loop exits. `[depth]`

---

**Q.** How is `LoadTestConfig` validated?

**A.** In `Builder.build()` — `targetRps` must be positive, `duration` must be
non-null and strictly positive, and both throw `IllegalArgumentException`
otherwise. `warmupDuration` and `rampupDuration` default to `Duration.ZERO`. An
invalid config cannot be constructed.

---

**Q.** What does `LoadGeneratorTest` cover?

**A.** That the expected number of requests is submitted for a given rate and
duration, that results during warmup are excluded from metrics, and that
interruption is handled cleanly.

---

## Section: cli-and-build

**Q.** What does Picocli give the CLI?

**A.** Annotation-driven argument parsing — `@Command` on the class, `@Option` for
`--rps`, `--duration`/`-d` and `--warmup`/`-w`, `@Parameters` for the positional
target URL — plus required-flag enforcement and generated help. `TempestCommand`
implements `Callable<Integer>` so the return value becomes the process exit code.

---

**Q.** How is Tempest distributed?

**A.** As a single fat (shadow) jar, built by the GradleUp Shadow plugin in
`tempest-cli`, with `com.github.jgold5.tempest.cli.TempestCommand` as the main
class. One `java -jar` and no install step — appropriate for a tool you want to
drop onto a test box.

---

**Q.** Why was GraalVM Native Image dropped?

**A.** Picocli resolves its `@Command`/`@Option` annotations reflectively at
runtime, and that reflection broke ahead-of-time compilation. Making it work
meant maintaining reflection configuration for the native build. For a tool whose
startup time is irrelevant next to a 30 second test run, a native binary was not
worth that carrying cost, so the fat jar stayed.

---

**Q.** `[depth]` What would it have taken to keep Native Image?

**A.** Picocli's `annotation-processor` generates the GraalVM reflection metadata
at compile time, which is the supported path. The decision was scope, not
impossibility — the payoff (faster startup, no JVM on the box) did not justify the
added build complexity at v0.1.0.

---

**Q.** What runs in CI?

**A.** GitHub Actions on push and PR to `main`: Temurin 21, cached Gradle, then
`./gradlew build sonar` — compile, tests, Spotless format check, JaCoCo coverage
verification, and a SonarCloud analysis. A second workflow lints PR titles for
Conventional Commits.

---

**Q.** How is code style and coverage enforced?

**A.** Spotless with google-java-format for style, applied to every subproject;
JaCoCo for coverage, with `jacocoTestCoverageVerification` wired into `check` and
scoped to the four classes that carry the logic — `HttpRequestExecutor`,
`MetricsRecorder`, `TokenBucket` and `LoadGenerator`. Coverage is measured on the
code that can break, not inflated by counting getters.

---

**Q.** What is in the dependency set, and why is it small?

**A.** OkHttp, HdrHistogram, Logback and Picocli at runtime; JUnit 5, AssertJ and
WireMock for tests — pinned in a Gradle version catalog, with Dependabot watching
weekly. The engine is deliberately close to the JDK: the concurrency comes from
the platform, so there is no framework to take on.

---

## Section: adrs-and-process

**Q.** What is an ADR, and which four does Tempest have?

**A.** An Architecture Decision Record — a short document capturing context,
decision, rationale and consequences at the moment a choice is made, so the
reasoning survives the commit. Tempest has four: 001 virtual threads, 002 custom
token bucket, 003 token bucket starts empty, 004 HdrHistogram.

---

**Q.** Why write ADRs on a solo project?

**A.** Because the expensive thing to lose is not the code, it is why the code is
shaped that way. Six months later "why is this bucket empty at construction?" is a
question you would otherwise re-derive — or worse, silently reverse. The
consequences section is also where you record the debt you took on, which is what
turns a shortcut into a tracked decision.

---

**Q.** Which Tempest ADR names its own future rework, and what is it?

**A.** ADR 002 — it records that the `synchronized`/`wait` inside the token bucket
should become a `ReentrantLock` to eliminate virtual thread pinning, and that
keeping the implementation in-house rather than using Guava is what makes that
change possible.

---

## Section: gaps-and-next

**Q.** `[gap]` If you shipped one more feature in Tempest, what would it be and why?

**A.** Wiring up rampup — the config field, validation and Javadoc already exist,
so it is a contained change to the dispatch loop, and it removes the sharpest
edge in the tool: hitting a cold service at full rate from the first second.

---

**Q.** `[gap]` What is missing from Tempest's output today?

**A.** Live interval reporting. The plumbing is there — `MetricsRecorder` produces
interval snapshots with timestamps and the recorder already flips per call — but
nothing calls it mid-run, so a 30 second test is silent until it finishes. That is
also the change that would force fixing the cumulative-vs-interval counter
mismatch.

---

**Q.** `[gap]` What would it take for Tempest to test something other than a single GET?

**A.** Core is ready — five methods, headers and bodies are all expressible on
`HttpRequestConfig`. The work is at the edges: CLI flags for method, headers and
body, or a scenario file so a run can drive a sequence of requests rather than
one repeated call.

---

**Q.** What is Tempest honestly good for today, and what is it not?

**A.** Good for: pacing a steady, accurate request rate at a single endpoint from
one machine and getting trustworthy tail latency out of it. Not for: multi-step
scenarios, distributed load generation, or anything needing request bodies from
the CLI. Knowing the boundary is the point of tagging it v0.1.0.
