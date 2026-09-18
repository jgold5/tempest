# ADR 006: Fat Jar over GraalVM Native Image

## Status
Accepted

## Context
Tempest ships as a command-line tool. A GraalVM Native Image binary would start in milliseconds and
would not require a JVM on the machine running the test, which is attractive for a tool intended to
be dropped onto a test box.

## Decision
Distribute a single fat jar built by the Shadow plugin. Do not pursue Native Image.

## Rationale
- Picocli resolves `@Command`, `@Option` and `@Parameters` reflectively at runtime. Native Image
compiles under a closed-world assumption: every reflective access must be registered in reflection
metadata at build time, or the compiled binary fails when it reaches that code. Keeping a native
build working therefore means generating that metadata (Picocli ships `picocli-codegen` for this)
and keeping it in step with every option the CLI grows.
- The benefit does not apply to this workload. Native Image buys startup time, and a Tempest run is
dominated by `--duration` -- seconds to minutes. Saving JVM startup on a 30 second test is not worth
maintaining a second build toolchain.
- The fat jar already delivers the part that matters: one artifact, no install step.
- The remaining requirement, a Java runtime on the box, is not an additional cost. Java 21 is
already a hard requirement under ADR 001.

## Consequences
- Running Tempest requires `java -jar` and a Java 21 runtime on the target machine.
- If Tempest grows a short-lived mode where process startup is a meaningful fraction of total
runtime, this decision should be revisited -- and the reflection configuration budgeted for.
- Any future move to Native Image should start from `picocli-codegen` rather than hand-maintained
reflection config.
