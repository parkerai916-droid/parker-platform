# Chapter 43 – Audit and Observability

## Purpose

Audit and Observability provide Parker with explainable operational history.

Audit explains what happened.

Observability explains how the system behaved.

## Responsibilities

- Record permission decisions
- Record execution outcomes
- Record sensitive data access
- Record plugin actions
- Record agent actions
- Track failures
- Track performance
- Support diagnostics

## Audit Records

Audit entries should include:

- timestamp
- principal
- resource
- action
- decision
- policy result
- confirmation result
- tool used
- outcome
- correlation ID

## Observability Data

Observability may include:

- latency
- model load times
- task duration
- failure rates
- resource pressure
- event throughput

## Privacy

Logs must not become uncontrolled surveillance.

Sensitive data should be redacted unless explicitly required for debugging.

## Architectural Rules

- Significant actions are audited automatically.
- Audit records are append-only.
- Audit logs are protected resources.
- Observability must respect privacy policy.

## Summary

Audit allows Parker to explain itself. Observability allows Parker to be maintained.
