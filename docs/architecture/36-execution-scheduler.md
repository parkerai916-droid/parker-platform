# Chapter 36 – Execution Scheduler

## Purpose

The Execution Scheduler determines when approved work should run.

It sits between the Execution Pipeline and long-running operational work, ensuring that urgent work receives priority while background work does not degrade responsiveness.

## Responsibilities

- Prioritise execution requests
- Schedule deferred tasks
- Respect urgency and deadlines
- Coordinate with Resource Manager
- Prevent duplicate scheduled actions
- Support cancellation and rescheduling
- Publish scheduling events

## Scheduling Inputs

The scheduler considers:

- request priority
- user context
- system load
- battery state
- network availability
- required model availability
- deadline
- policy restrictions

## Priority Classes

- Immediate
- High
- Normal
- Background
- Deferred
- Maintenance

## Architectural Rules

- Scheduling never bypasses permissions.
- Scheduled work still enters the Execution Pipeline.
- Expired scheduled tasks must not execute silently.
- Failed scheduled work must produce diagnostic events.

## Summary

The Execution Scheduler ensures Parker acts at the right time without compromising responsiveness or trust.
