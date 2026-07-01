# Core Contract Relationships

The five Volume 1 core contracts form Parker's runtime grammar.

```text
Principal
    creates
ExecutionRequest
    targets
Resource
    evaluated by
Permission
    produces
ExecutionResult
```

## Relationship Rules
- A Principal MUST exist before an ExecutionRequest can be valid.
- A Resource MUST exist before Permission can be evaluated.
- Permission MUST be evaluated before execution.
- ExecutionResult MUST be produced for every executed request.
- Audit SHOULD link all five contracts together where practical.
