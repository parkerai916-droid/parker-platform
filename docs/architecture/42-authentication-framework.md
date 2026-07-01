# Chapter 42 – Authentication Framework

## Purpose

The Authentication Framework verifies that a principal is who it claims to be.

It supports both user authentication and service authentication.

## Responsibilities

- Authenticate users
- Authenticate services
- Support biometrics
- Support device unlock state
- Support tokens
- Support trusted devices
- Support re-authentication for sensitive actions

## Authentication Methods

- Android device unlock
- Biometrics
- Passcode
- Token validation
- Local service credentials
- Future passkeys

## Authentication vs Authorisation

Authentication proves identity.

Authorisation grants action.

Parker must never confuse the two, because apparently even software needs boundaries.

## Sensitive Actions

Sensitive actions may require step-up authentication.

Examples:

- exporting data
- unlocking doors
- disabling alarms
- changing trust settings
- viewing credentials

## Architectural Rules

- Authentication state expires.
- Sensitive operations may require fresh authentication.
- Authentication never bypasses permissions.
- Authentication failures are auditable.

## Summary

Authentication establishes identity. Permission still determines authority.
