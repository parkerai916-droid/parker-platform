# Chapter 37 – Task Manager

## Purpose

The Task Manager tracks work that spans more than a single immediate execution.

It provides visibility into ongoing, paused, cancelled, failed and completed work.

## Responsibilities

- Create task records
- Track progress
- Handle retries
- Manage cancellation
- Track dependencies
- Report task status
- Resume recoverable work after restart

## Task States

```text
Created
  ↓
Queued
  ↓
Running
  ↓
Paused
  ↓
Completed
```

Alternative terminal states:

- Cancelled
- Failed
- Expired
- Superseded

## Examples

- Document indexing
- Email review
- Calendar scan
- Home Assistant diagnostics
- Memory consolidation
- Plugin update

## Architectural Rules

- Every long-running task has an owner.
- Every task has a responsible principal.
- Tasks must be auditable.
- Cancelled tasks must not continue in the background.

## Summary

The Task Manager gives Parker operational memory for work in progress.
