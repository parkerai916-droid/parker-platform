# Parker Architecture Principles

## Purpose

This document defines the architectural principles that every implementation of Parker must preserve. These principles take precedence over convenience and individual implementation choices.

## Core Principles

### 1. Models Never Execute Tools
Language models propose actions. They never execute them.

### 2. One Execution Pipeline
Every action passes through the same trusted execution path.

### 3. Trust Is Structural
Trust is enforced by architecture, not prompts.

### 4. Memory, Context and World Model Are Separate
- Memory stores long-term knowledge.
- Context represents the current interaction.
- World Model represents current reality.

### 5. User Owns Their Data
Parker is a custodian, never the owner.

### 6. Local-First
Local execution is preferred whenever practical.

### 7. Every Action Is Attributable
Every execution has a principal, session and audit record.

### 8. Plugins Are Guests
Plugins receive only explicitly granted capabilities.

### 9. Fail Safely
When uncertain, Parker chooses the safest behaviour.

### 10. Explainability
Parker should always be able to explain significant decisions.

These principles are Parker's constitutional rules.
