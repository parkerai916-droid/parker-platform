# Chapter 39 – Resource Manager

## Purpose

The Resource Manager protects Parker's runtime stability by managing computational resources.

This is especially important for local-first operation on mobile hardware.

## Responsibilities

- Monitor CPU usage
- Monitor memory pressure
- Monitor battery level
- Monitor thermal state
- Monitor storage
- Coordinate model loading
- Throttle background work
- Prevent resource starvation

## Managed Resources

- CPU
- RAM
- Storage
- Battery
- Network
- GPU / NPU where available
- Model runtime capacity

## Policy Examples

- Avoid loading large models on low battery.
- Delay background indexing during thermal stress.
- Prefer lightweight models when responsiveness matters.
- Pause maintenance work during active conversation.

## Architectural Rules

- Resource constraints may delay execution.
- Resource constraints may not bypass Trust.
- Critical safety tasks may override normal throttling.
- Resource decisions must be explainable.

## Summary

The Resource Manager keeps Parker useful without turning the phone into a pocket-sized hand warmer.
