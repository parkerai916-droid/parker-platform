# Principal Contract

## Status
Version: 0.6-alpha  
Status: Draft Specification

## Purpose
A Principal is any actor that can request access, initiate work, publish events, receive permissions, or appear in audit records.

## Principal Types
- User
- System
- Internal Agent
- Plugin
- Tool
- Scheduled Task
- Developer Session
- Future Remote Device

## Required Fields
- principalId
- principalType
- displayName
- owner
- status
- createdAt
- lastSeenAt
- metadata

## Lifecycle
Created → Active → Suspended → Revoked → Archived

## Normative Requirements
- Parker MUST NOT execute any request without an identified Principal.
- Principal identity MUST NOT imply permission.
- Internal agents MUST be represented as Principals.
- Plugins MUST be represented as Principals.
- Principal activity MUST be auditable.
- Revoked Principals MUST NOT initiate new ExecutionRequests.

## Related Documents
- Chapter 41 – Identity Service
- Chapter 42 – Authentication Framework
- ADR-013 – Agents and Services Use Principal Identities
- docs/diagrams/principal-lifecycle-state-machine.mmd (literal transcription of the Lifecycle line above; see IMPLEMENTATION_GAPS.md #5 for what it deliberately does not cover)
