# Chapter 44 – Secrets and Credential Management

## Purpose

The Secrets and Credential Management Service protects tokens, keys, passwords and other sensitive credentials.

## Responsibilities

- Store secrets securely
- Retrieve secrets for authorised services
- Rotate secrets
- Revoke secrets
- Protect Home Assistant tokens
- Protect API keys
- Integrate with Android Keystore
- Prevent accidental logging

## Secret Types

- API tokens
- OAuth tokens
- Home Assistant tokens
- Encryption keys
- Plugin credentials
- Cloud service credentials

## Storage

Secrets should be stored using platform-backed secure storage wherever practical.

On Android this should use Android Keystore or equivalent secure mechanisms.

## Architectural Rules

- Secrets are never stored in plain text.
- Secrets are never logged.
- Plugins cannot read secrets directly unless explicitly granted.
- Secret access is auditable.
- Revoked secrets must fail immediately.

## Summary

Secrets are among Parker's highest-risk resources and require strict handling.
