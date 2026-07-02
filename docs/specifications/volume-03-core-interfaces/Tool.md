# Tool Interface

## Status
Version: 0.6-alpha3 (content unchanged since the v0.6 Volume 3 release;
stamped during the v0.7 Architecture Completion Phase for consistency
with other Volume 3 documents -- see
docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md §3.6).

## Purpose

A Tool is a deterministic execution unit invoked by the ExecutionPipeline.

## Responsibilities

- Validate inputs
- Execute approved work
- Return structured results
- Avoid hidden state
- Avoid direct coordination with other tools

## Required Operations

```kotlin
interface Tool {
    val descriptor: ToolDescriptor
    suspend fun validate(request: ExecutionRequest): ValidationResult
    suspend fun execute(request: ExecutionRequest): ToolResult
}
```

## Normative Requirements

- Tools MUST NOT decide whether work is authorised.
- Tools MUST validate before execution.
- Tools MUST return structured results.
- Tools MUST NOT communicate directly with unrelated tools.

## Related

- Chapter 12 – Tool Framework
