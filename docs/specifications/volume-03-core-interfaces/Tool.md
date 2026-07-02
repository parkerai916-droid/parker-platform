# Tool Interface

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
