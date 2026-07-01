# Chapter 49 – Testing and Verification

## Purpose

Testing and Verification ensure Parker behaves according to architecture.

Testing is not limited to code correctness.

It also verifies architectural compliance.

## Test Categories

- Unit tests
- Integration tests
- End-to-end tests
- Simulation tests
- Security tests
- Permission tests
- Plugin sandbox tests
- Regression tests
- Architecture tests

## Required Architecture Tests

- Models cannot execute tools.
- Tools require permission decisions.
- Plugins cannot access undeclared resources.
- Agents have explicit principals.
- Event spoofing is rejected.
- Level 4 actions require confirmation.
- Memory promotion respects policy.
- Revoked permissions take effect immediately.

## Verification Environments

- Local Android device
- Emulator
- Simulated Home Assistant
- Mock calendar
- Mock email
- Synthetic document set

## Architectural Rules

- Architecture violations are release blockers.
- Tests must cover failure modes.
- Security tests must run before public release.
- Regression tests must protect invariants.

## Summary

Testing protects Parker from slowly becoming something Parker was never meant to be.
