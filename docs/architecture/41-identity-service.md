# Chapter 41 – Identity Service

## Purpose

The Identity Service defines and manages principals within Parker.

A principal is any actor that can request access, publish events or initiate work.

## Responsibilities

- Define principal identities
- Maintain principal metadata
- Support user identity
- Support internal service identities
- Support agent identities
- Support plugin identities
- Support future delegated identities

## Principal Types

- User
- System
- Internal Agent
- Plugin
- Tool
- Scheduled Task
- Developer Session
- Future Remote Device

## Identity Is Not Permission

Identity answers:

> Who is requesting this?

Permission answers:

> Are they allowed?

These must remain separate.

## Architectural Rules

- Every action has a principal.
- Internal services are not anonymous.
- Agents are principals.
- Plugins are principals.
- Identity records are auditable.

## Summary

The Identity Service gives Parker accountability.
