# ADR-016 – Core Contracts

## Status
Accepted

## Decision
Parker's runtime model is defined by five core contracts:

- Principal
- Resource
- Permission
- ExecutionRequest
- ExecutionResult

## Reason
A small set of stable contracts reduces ambiguity and allows all runtime systems to speak the same language.

## Consequences
Voice, text, plugin, agent, scheduled, Android and Home Assistant requests all normalise into the same execution model.
