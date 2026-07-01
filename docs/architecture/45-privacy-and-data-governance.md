# Chapter 45 – Privacy and Data Governance

## Purpose

Privacy and Data Governance define how Parker controls, retains, exports and deletes user data.

## Responsibilities

- Define data ownership
- Manage consent
- Apply retention policies
- Support export
- Support deletion
- Classify sensitive data
- Protect third-party personal information

## Data Categories

- Conversation data
- Memory
- Documents
- Email
- Calendar
- Contacts
- Home Assistant state
- Audit logs
- Plugin data

## User Rights

The user should be able to:

- inspect stored information
- correct information
- delete information
- export information
- disable memory categories
- revoke permissions

## Retention

Not all data should be retained indefinitely.

Retention depends on:

- sensitivity
- usefulness
- legal obligation
- user preference
- audit requirements

## Architectural Rules

- User data belongs to the user.
- Parker is custodian, not owner.
- Sensitive data requires explicit handling.
- Deletion must be deliberate and auditable.
- Third-party personal data receives heightened protection.

## Summary

Privacy is not a feature. It is a governing constraint across Parker.
