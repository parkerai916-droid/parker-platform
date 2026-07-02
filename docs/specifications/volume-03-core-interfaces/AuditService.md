# AuditService Interface

## Status
Version: 0.6-alpha3 (content unchanged since the v0.6 Volume 3 release;
stamped during the v0.7 Architecture Completion Phase for consistency
with other Volume 3 documents -- see
docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md §3.6).

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
