# Chapter 48 – Safe Mode and Recovery

## Purpose

Safe Mode allows Parker to recover from faults while preserving user control and system safety.

## Safe Mode Behaviour

When Safe Mode is active:

- Plugins are disabled
- Background agents are paused
- External execution is restricted
- Core diagnostics remain available
- Trust settings remain inspectable
- Recovery tools are enabled

## Triggers

Safe Mode may be triggered by:

- repeated startup failure
- plugin crash loop
- corrupted configuration
- permission system failure
- model runtime failure
- unsafe execution detection

## Recovery Actions

- Disable plugins
- Restore previous configuration
- Rebuild indexes
- Clear temporary state
- Re-authenticate services
- Run diagnostics

## Architectural Rules

- Safe Mode must not depend on optional plugins.
- Safe Mode must preserve access to recovery controls.
- Unsafe services remain disabled until explicitly restored.
- Recovery actions are audited.

## Summary

Safe Mode is Parker's seatbelt. Ideally boring. Very useful when everything else becomes dramatic.
