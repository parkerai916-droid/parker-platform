# Chapter 38 – Workflow Engine

## Purpose

The Workflow Engine executes approved multi-step workflows.

A workflow is a structured sequence of actions that may include conditions, dependencies, retries and rollback behaviour.

## Responsibilities

- Execute workflow definitions
- Track step status
- Support conditional branching
- Support retries
- Support cancellation
- Support rollback where practical
- Emit workflow events

## Workflow Examples

- Leaving home routine
- Bedtime routine
- Document intake process
- Weekly maintenance
- Home Assistant recovery
- Email triage

## Workflow Structure

```text
Trigger
  ↓
Preconditions
  ↓
Steps
  ↓
Validation
  ↓
Completion
  ↓
Reflection
```

## Architectural Rules

- Workflows do not grant permissions.
- Every workflow step enters the Execution Pipeline.
- Workflow state is auditable.
- Rollback must never perform unauthorised actions.

## Summary

The Workflow Engine allows Parker to coordinate complex behaviour without hiding execution.
