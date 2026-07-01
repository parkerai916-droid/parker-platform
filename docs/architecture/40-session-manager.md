# Chapter 40 – Session Manager

## Purpose

The Session Manager maintains secure user sessions across voice, text and future interaction channels.

It supports conversational continuity while ensuring sessions expire safely.

## Responsibilities

- Create sessions
- Track session identity
- Maintain session context
- Expire inactive sessions
- Support voice-to-text continuity
- Support secure re-authentication
- Associate actions with session IDs

## Session Types

- Voice session
- Text session
- Background task session
- Developer session
- Plugin session
- Future remote session

## Expiry Conditions

A session may expire because of:

- inactivity
- confidence loss
- user command
- authentication timeout
- device lock
- policy restrictions

## Architectural Rules

- Sessions do not grant permanent authority.
- Sensitive actions may require re-authentication.
- Session context is not Memory.
- Every execution links to a session where available.

## Summary

The Session Manager allows Parker to feel conversational without becoming careless.
