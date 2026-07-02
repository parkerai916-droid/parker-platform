# Resource Contract

## Status
Version: 0.6-alpha  
Status: Draft Specification

## Purpose
A Resource is anything Parker can read, create, modify, delete, execute, export, observe, or protect.

## Resource Categories
- Memory
- World Model
- Document
- Email
- Calendar
- Contact
- Home Assistant Entity
- Android Capability
- Tool
- Plugin
- Agent
- Secret
- Configuration
- Audit Log

## Required Fields
- resourceId
- resourceType
- displayName
- ownerPrincipalId
- sensitivity
- lifecycleState
- createdAt
- updatedAt
- source
- metadata

## Lifecycle
Created → Registered → Available → Updated → Archived → Deleted

## Normative Requirements
- Parker MUST NOT access undeclared Resources.
- Every Resource MUST have an owner.
- Every Resource MUST have a sensitivity classification.
- Sensitive Resources MUST pass through Permission evaluation before access.
- Resource lifecycle changes MUST be auditable.
- Plugins MUST NOT create hidden Resources.

## Related Documents
- Chapter 8 – Resource Registry
- Chapter 45 – Privacy and Data Governance
- docs/diagrams/resource-lifecycle-state-machine.mmd (literal transcription of the Lifecycle line above; see IMPLEMENTATION_GAPS.md #5 for what it deliberately does not cover)
