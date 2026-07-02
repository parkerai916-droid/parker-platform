# AuditService Interface

## Purpose

The AuditService records significant decisions, actions and outcomes.

## Responsibilities

- Create audit records
- Link related events
- Redact sensitive data
- Support explainability
- Protect audit integrity

## Required Operations

```kotlin
interface AuditService {
    suspend fun record(record: AuditRecord): AuditRecordId
    suspend fun query(query: AuditQuery): List<AuditRecord>
}
```

## Normative Requirements

- Audit records MUST be append-only.
- Significant actions MUST be audited.
- Audit logs are protected Resources.
- Sensitive fields SHOULD be redacted.

## Related

- Chapter 43 – Audit and Observability
- ADR-014
