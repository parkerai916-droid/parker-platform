# Permission Contract

## Status
Version: 0.6-alpha  
Status: Draft Specification

## Purpose
A Permission determines whether a Principal may perform an Action against a Resource under a specific Context.

## Core Question
May this Principal perform this Action on this Resource under these circumstances?

## Decision Outcomes
- Approved
- ApprovedWithConfirmation
- Deferred
- Denied

## Permission Levels
1. Automatic
2. User Aware
3. Confirmation Required
4. High Assurance
5. Administrative

## Normative Requirements
- Permission evaluation MUST occur before execution.
- Level 4 and Level 5 actions MUST require explicit confirmation or high-assurance authentication.
- Permission decisions MUST be auditable.
- Permission grants MUST be revocable.
- Revoked permission MUST take effect immediately.
- Cumulative risk MUST be evaluated for multi-step plans.

## Related Documents
- Chapter 10 – Permission Engine
- Chapter 11 – Execution Pipeline
