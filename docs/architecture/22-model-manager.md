# Chapter 22 – Model Manager

## Purpose

The Model Manager provides a unified abstraction over all AI models used within Parker.

## Responsibilities

- Discover models
- Load/unload models
- Route inference
- Manage versions
- Health monitoring
- Fallbacks

## Rule

Services request capabilities rather than specific models.
