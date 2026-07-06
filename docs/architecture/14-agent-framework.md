# Chapter 14 - Agent Framework

Agents are specialised internal workers. They operate continuously but do not receive implicit trust.

Every agent is a Principal and must receive explicit permissions.

**Terminology note (Sprint 5).** This chapter names two related but
distinct concepts, disambiguated in full in their own governing
documents: a long-lived, daemon-style **Background Agent**
(`docs/specifications/volume-03-core-interfaces/Agent.md`, specified
but not yet implemented), and the bounded, per-Task **Agent Runtime**
that executes Agent Runs (`docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`,
implemented in Sprint 3, Track C). Agent Runtime does not instantiate
or depend on the Background Agent interface.
