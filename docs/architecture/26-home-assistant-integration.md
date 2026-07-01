# Chapter 26 – Home Assistant Integration

## Purpose

The Home Assistant Integration Service provides Parker with a secure abstraction over Home Assistant so the rest of the platform never communicates directly with smart-home devices.

## Responsibilities

- Discover entities
- Maintain authenticated sessions
- Execute approved service calls
- Monitor entity state
- Publish events
- Validate responses

## Resource Mapping

Every Home Assistant entity becomes a Parker Resource.

## Security

- LAN-first operation
- Tokens stored securely
- All commands pass through the Execution Pipeline
- No direct model-to-device communication

## Summary

Home Assistant is Parker's smart-home gateway.
