# Parker Platform

## A Trust-First Personal Intelligence Platform

> **Parker is to personal AI what Linux is to operating systems: an open, model-independent platform where the owner, not the vendor, is ultimately in control.**

**Own your AI. Own your data. Own your decisions.**

Parker is an open-source platform for building trustworthy personal AI systems.

Unlike conventional AI assistants that revolve around cloud services, proprietary models, and vendor-controlled ecosystems, Parker is designed around a simple principle:

> **The owner remains in control.**

Everything else follows from that.

---

## Why Parker Exists

Today's AI assistants are powerful, but they commonly assume that:

- conversations belong in someone else's cloud;
- memories belong in someone else's database;
- automation depends on someone else's service;
- intelligence is tied to someone else's model.

Parker was created to reverse those assumptions.

Instead of asking owners to trust a company, Parker is designed so that trust is enforced by architecture.

---

## What Makes Parker Different

### Trust-First Architecture

Reasoning models do not execute actions directly.

> **Cognition proposes. Trust authorises. Runtime executes.**

Every executable action remains subject to Parker's identity, permission, and runtime controls.

### Owner Authority

Parker treats the owner as the constitutional authority.

No plugin, agent, language model, or external service may grant itself authority or bypass the Trust Framework.

### Model Independent

Parker is not built around any single AI model.

Reasoning engines are replaceable components. They may include local or remote models such as:

- Qwen
- Gemma
- Claude
- GPT
- Llama

Parker remains the governing platform regardless of which model provides reasoning.

### Local-First

Parker is designed to operate locally wherever practical.

Privacy is the default. Cloud services are optional capabilities rather than mandatory infrastructure.

### Plugin-Based

Capabilities are added through contracts, tools, and plugins rather than being hardcoded into the core platform.

### Constitutional Architecture

Parker is governed by published architectural principles.

> **Architecture drives implementation, not the other way around.**

---

# What Works Today

Parker's constitutional foundation is complete and frozen. The project has moved well beyond isolated runtime components and now includes a production-composed pipeline covering intake, context assembly, reasoning, reply delivery, Goal handoff, Plan Candidate generation, Planner Runtime invocation, and — as of the Controlled Agent Run Submission milestone — controlled Agent Run submission and synchronous execution.

This represents a meaningful architectural transition, not merely an additive one. Parker now has a production composition root: a single, real assembly point where the runtime components built and verified across previous units are wired together into the platform's canonical runtime, rather than remaining a set of independently verified, isolated pieces. The conversation, reasoning, reply, planning, and execution pipeline described below exists today as a production-composed system — constructed once, in one place, and exercised by real production code — not merely as a design proven correct only inside isolated tests.

An accepted Task Proposal no longer stops at queueing. The Task Manager Runtime now submits a `START` command for every accepted proposal, gated by an explicit run-initiation permission evaluation before any Agent Run record exists. Acceptance and execution are deliberately separate phases: an accepted Agent Run stops at `READY`, and `AgentRunExecutionTrigger.execute(agentRunId)` — invoked only after the Task Manager Runtime has released its own lock — owns the `READY → RUNNING` transition, the `agent.started` event, and execution through the existing Agent Runtime. Reasoning, planning, trust, and execution remain architecturally separate, independently governed components throughout.

This is controlled submission and execution wired into the production path, not a general-purpose autonomous agent or a finished tool-execution surface — see "What Is Not Yet Complete" below.

The current implementation has been developed through governance-first units under the Parker Engineering Standard (**PES-001**).

## Engineering Workflow

The lifecycle below has become one of Parker's defining engineering characteristics. Every implementation unit progresses through this governed sequence:

```text
Governance Review
        ↓
Contract Design
        ↓
Scope Lock
        ↓
Implementation
        ↓
Native Verification
        ↓
Commit
        ↓
Push
```

Architecture is reviewed and locked before code is written, and native verification is completed before a unit is accepted into the canonical repository. No unit skips a stage.

## Current Platform Capabilities

Trust and execution substrate:

- Identity Service
- Identity-aware Permission Engine
- Permission Policy model
- Execution Pipeline
- Tool Registry
- Tool Invocation Binding
- Resource Registry
- Event Bus and runtime event coordination
- Task Manager Runtime
- controlled Agent Run submission from the Task Manager Runtime
- explicit `START` run-initiation permission evaluation
- two-phase Agent Run acceptance and execution
- `AgentRunExecutionTrigger`
- deterministic production `AgentStepSource`
- Multi-step Agent Runtime
- Agent Run lifecycle ordering through `READY`, `RUNNING`, and terminal Agent events
- suspend, resume, and cancel semantics
- auditable runtime outcomes

Communication, conversation, and reasoning:

- Local Text Channel
- Communication Intake
- CommunicationConversationCoordinator
- Conversation Engine
- ConversationTurnReasoningCoordinator
- production `ReasoningContext` assembly
- Conversation History source
- Memory source
- World Model source
- model-backed Reasoning Provider
- ConversationReplyCoordinator

Reply delivery:

- ResponseComposer
- ReplyDeliveryCoordinator
- ResponseDelivery
- permission-gated delivery through the existing runtime path

Goal and planning path:

- Reasoning-to-Planning handoff
- `PlanningRequest`
- `PlanCandidateGenerator`
- deterministic `DefaultPlanCandidateGenerator`
- production `PlannerRuntime` invocation
- `PlanningSessionResult` propagation through conversation and Parker runtime outcomes
- production composition of the concrete Planner and Task Manager runtimes
- system identity registration for Planner Runtime and Task Manager Runtime

## Implementation Maturity

| Area | Status |
|---|---|
| Constitutional Architecture | Complete |
| Runtime Foundation | Complete |
| Conversation Pipeline | Complete |
| Reasoning Context | Complete |
| Reply Delivery | Complete |
| Goal Routing | Complete |
| Planner Integration | Complete |
| Agent Execution | Controlled Submission Complete |
| Workflow Engine | Planned |
| Android Product | Planned |

---

Parker's runtime is assembled by a single production composition root, with each stage of the pipeline implemented as a thin, single-responsibility coordinator rather than a monolithic handler. Reasoning, planning, trust, and execution remain architecturally separated components — each independently testable and replaceable — while working together as one governed runtime.

## Current Production Conversation Path

```text
Owner Message
      │
      ▼
Communication Intake
      │
      ▼
Conversation Engine
      │
      ▼
Reasoning Context
      │
      ├── Conversation History
      ├── Memory
      └── World Model
      │
      ▼
Reasoning Provider
      │
      ├── Reply
      ├── Goal
      └── NoAction
      │
      ▼
ConversationReplyCoordinator
      │
      ├── Reply → ResponseComposer → ResponseDelivery
      │
      ├── Goal → GoalPlanningHandoffCoordinator
      │              │
      │              ▼
      │       PlanCandidateGenerator
      │              │
      │              ▼
      │         PlannerRuntime
      │              │
      │              ▼
      │      TaskManagerRuntime
      │              │
      │              ▼
      │    AgentRunCommandChannel
      │              │
      │              ▼
      │    AgentRunExecutionTrigger
      │              │
      │              ▼
      │         AgentRuntime
      │              │
      │              ▼
      │      ExecutionPipeline
      │
      └── NoAction → NotAccepted
      │
      ▼
ConversationOutcome
      │
      ▼
ParkerRuntimeOutcome
```

This path is now assembled in the production composition root rather than existing only in isolated tests. The Goal branch now reaches controlled Agent Run submission and synchronous execution through the Agent Runtime, not merely `PlanningSessionResult` — production Tool execution from planned Goals remains incomplete.

---

## Milestone: Production Planning Integration

Parker can now carry an accepted owner message through:

```text
Message
  ↓
Reasoning Context
  ↓
Reasoning
  ↓
Goal
  ↓
PlanningRequest
  ↓
Plan Candidate generation
  ↓
Planner Runtime
  ↓
PlanningSessionResult
```

The planning result is preserved through:

```text
GoalPlanningHandoffOutcome.Planned
        ↓
ConversationOutcome.Planned
        ↓
ParkerRuntimeOutcome.Planned
```

`PlanningSessionResult.Completed`, `Rejected`, and `Failed` are all treated as completed planning attempts and are carried through the `Planned` outcome path. A genuine thrown exception remains a top-level runtime failure.

---

## Milestone: Controlled Agent Run Submission

Parker can now carry an accepted Task Proposal through controlled Agent Run submission and synchronous execution:

```text
TaskProposal
    ↓
TaskManagerRuntime
    ↓
START permission evaluation
    ↓
AgentRunCommandChannel.submit()
    ↓
AgentRun READY
    ↓
task.agent_run_started
    ↓
Task QUEUED → RUNNING
    ↓
AgentRunExecutionTrigger.execute()
    ↓
AgentRun RUNNING
    ↓
ExecutionPipeline
```

This path preserves Parker's constitutional separation:

> **Cognition proposes. Trust authorises. Runtime executes.**

A Planner Runtime may propose a Task Proposal, but nothing may submit or execute an Agent Run except through the same Trust Framework that already governs every other executable action in this platform.

Submission and execution are deliberately two separate phases, not one combined call, in order to preserve:

- truthful lifecycle semantics — an Agent Run only reaches `RUNNING` once execution has actually begun;
- correct event ordering — `task.agent_run_started` is always published before execution begins;
- mutex safety — the Task Manager Runtime releases its own lock before execution runs; and
- explicit Trust control — acceptance and execution remain independently governed steps.

An earlier single-phase design was found, during native verification, to risk a runtime deadlock under this ordering; the two-phase design above is the corrected, adopted result.

---

## Current Verified Baseline

- **Architecture milestone:** Architecture v1.0 — Constitutional Foundation
- **Implementation status:** Controlled Agent Run Submission complete
- **Latest completed unit:** Controlled Agent Run Submission
- **Latest commit:** `991fca3` — `feat(runtime): implement approved two-phase agent run submission`
- **Verification:** full native Gradle test suite passed
- **Build result:** `BUILD SUCCESSFUL`
- **Repository state:** `main` synchronized with `origin/main`; working tree clean

---

# What Is Not Yet Complete

Parker is not yet a finished consumer assistant.

Controlled Agent Run submission is now live, and synchronous execution through the Agent Runtime is now wired into the production path. The remaining boundary is the transition from an accepted, executing Agent Run into real Tool execution against planned Goals, and the broader Task lifecycle behaviour that follows an Agent Run's outcome.

Still under development:

- production tool execution initiated from planned Goals
- broader Task lifecycle handling after Agent failure
- richer production completion and recovery semantics
- scheduling and workflow orchestration
- Workflow Engine
- live validation of `LocalHttpModelInferenceClient` against the intended local model server
- plugin ecosystem
- Android runtime and user experience
- multi-device deployment
- public SDK
- security hardening
- release packaging

The production `AgentStepSource` used today, `DeterministicAgentStepSource`, is a deliberate, deterministic stand-in for a future Planner-backed step source, not tool execution driven by real planned Goals. A configured Agent Run may still terminate in `agent.failed` where no executable action mapping exists for its proposed action — an expected outcome for an unmapped action today, not a defect this milestone resolves.

Therefore:

- planning is live;
- task proposal and queueing infrastructure exists;
- controlled Agent Run submission is live;
- synchronous execution through the Agent Runtime is wired;
- a configured Agent Run may still terminate in `agent.failed` where no executable action mapping exists;
- planned Goal to real Tool execution remains incomplete.

This boundary is intentional and remains subject to future governance.

---

# Architecture Overview

Parker separates intelligence into three responsibilities:

```text
                +----------------------+
                |      Reasoning       |
                | "What should happen?"|
                +----------+-----------+
                           |
                           v
                +----------------------+
                |     Trust Engine     |
                | "Is this permitted?" |
                +----------+-----------+
                           |
                           v
                +----------------------+
                | Execution Pipeline   |
                |  "Make it happen."   |
                +----------+-----------+
                           |
                           v
             Tools • Plugins • Devices • Services
```

The core principle remains:

> **Cognition proposes. Trust authorises. Runtime executes.**

---

## Architecture at a Glance

Before the detailed runtime and conversation diagrams below, here is Parker's architecture at its simplest — the conceptual path every owner interaction follows:

```text
Owner
    │
    ▼
Communication
    │
    ▼
Reasoning
    │
    ▼
Planning
    │
    ▼
Trust
    │
    ▼
Execution
    │
    ▼
Tools • Devices • Plugins
```

Each of these stages is a real, separate component in the production runtime described in the sections that follow — this diagram is a conceptual orientation, not a replacement for them.

---

## Runtime Architecture

### Trust-governed execution substrate

```text
                 Owner
                   │
                   ▼
        Parker Constitution
                   │
                   ▼
           Trust Framework
                   │
                   ▼
        Permission Engine
                   │
                   ▼
        Execution Pipeline
                   │
                   ▼
       Multi-Step Agent Runtime
                   │
                   ▼
     Tool Registry / Resources
```

### Production-composed conversation, reasoning, reply, and planning path

```text
CommunicationIntake
        │
        ▼
CommunicationConversationCoordinator
        │
        ▼
ConversationTurnReasoningCoordinator
        │
        ▼
ConversationEngine
        │
        ▼
ReasoningContext
        │
        ▼
ReasoningProvider
        │
        ▼
ConversationReplyCoordinator
        │
        ├── Reply → ReplyDeliveryCoordinator
        │             ├── ResponseComposer
        │             └── ResponseDelivery
        │
        ├── Goal → GoalPlanningHandoffCoordinator
        │            │
        │            ▼
        │       PlanCandidateGenerator
        │            │
        │            ▼
        │         PlannerRuntime
        │            │
        │            ▼
        │      TaskManagerRuntime
        │            │
        │            ▼
        │    AgentRunCommandChannel
        │            │
        │            ▼
        │    AgentRunExecutionTrigger
        │            │
        │            ▼
        │         AgentRuntime
        │            │
        │            ▼
        │      ExecutionPipeline
        │
        └── NoAction → NotAccepted
        │
        ▼
ConversationOutcome
        │
        ▼
ParkerRuntimeOutcome
```

The conversation path is now constructed by the production composition root. Reply delivery remains permission-gated. Goal planning reaches the real Planner Runtime, and an accepted Task Proposal now reaches controlled Agent Run submission and synchronous execution through the Agent Runtime. Every branch converges on `ConversationOutcome`, then `ParkerRuntimeOutcome`. The next unresolved boundary is production tool execution from planned Goals and broader Task lifecycle completion.

---

## Knowledge Architecture

Parker organises knowledge into three layers:

| Layer | Purpose |
|---|---|
| **Memory** | What Parker has learned |
| **World Model** | What Parker currently believes to be true |
| **Reasoning Context** | What matters for the current turn or task |

Production Reasoning Context assembly now draws from Conversation History, Memory, and World Model sources while preserving their distinct ownership and lifecycle boundaries.

---

## Project Goals

Parker aims to become a complete personal intelligence platform capable of:

- personal assistance;
- home automation;
- workflow automation;
- knowledge management;
- long-term memory;
- local AI reasoning;
- secure tool execution;
- multi-device operation;
- plugin extensibility;
- local and hybrid deployment.

---

## Design Principles

Parker is built around non-negotiable principles:

- Owner authority
- Trust before execution
- Local-first operation
- Model independence
- Plugin extensibility
- Explicit permissions
- Explainable decisions
- Constitutional governance
- Auditable runtime behaviour
- Replaceable reasoning providers

---

## Current Architecture Status

**Current Architecture Milestone:** **Architecture v1.0 — Constitutional Foundation**

The constitutional architecture is complete and frozen.

The implementation has progressed through Sprint 11 and subsequent planning integration units. The runtime now contains both:

1. a trust-governed execution substrate; and
2. a production-composed conversational pipeline reaching real reasoning, reply delivery, and planning.

Production Reasoning Context ownership, Memory integration, World Model integration, Goal routing, Plan Candidate generation, and Planner Runtime invocation are no longer open architectural placeholders.

Future implementation should extend the existing platform through controlled execution, workflow, plugin, device, and user-facing capabilities rather than redesigning the constitutional foundation.

---

## Project Governance

Parker is governed by four complementary documents:

| Document | Purpose |
|---|---|
| **Parker Constitution** | Defines what Parker is. |
| **Architecture Decisions** | Define how Parker is architected and why major decisions were made. |
| **Parker Engineering Standard (PES-001)** | Defines how Parker is engineered, verified, reviewed, and evolved. |
| **Project Governance** | Defines the relationship between the governing documents and the platform governance model. |

Future Architecture Decisions that alter engineering practice must explicitly reference the relevant PES-001 section.

---

## Engineering Standard

Parker Engineering Standard (**PES-001**) governs the engineering lifecycle used to develop the platform.

The standard establishes:

- Architecture before implementation
- Evidence before opinion
- Verification before acceptance
- Documentation as a first-class engineering artefact
- Explicit implementation-gap management
- Engineering reviews and retrospectives as part of development

Implementation is performed as incremental, independently verified engineering units, each progressing through the Engineering Workflow described above.

Every completed unit must satisfy the Definition of Complete defined by PES-001 before acceptance.

---

## Constitutional Principles

The Constitutional Foundation establishes that:

- **Parker owns authority. Modules provide capability.**
- **Cognition proposes. Trust authorises. Runtime executes.**
- **The owner remains in control.**
- **Trust is earned through architecture, not marketing.**
- **Local-first and trust-first operation are the defaults.**
- **User rights are protected as constitutional principles.**
- **Knowledge is organised into Memory, World Model, and Reasoning Context.**
- **Reasoning providers are model-agnostic and interchangeable.**
- **No module may grant itself authority or bypass the Trust Framework.**

---

## Constitutional Documents

The constitutional foundation is defined by:

- Architecture History
- Parker Constitution
- Architecture Decisions
- Parker Engineering Standard (PES-001)
- Project Governance
- User Authorship & Evidence
- Reasoning Context
- Trust Framework

---

# For Developers

## Current Status

- **Architecture:** Constitutional Foundation complete and frozen
- **Runtime Foundation:** Complete
- **Sprint status:** Controlled Agent Run Submission complete
- **Latest completed unit:** Controlled Agent Run Submission, implemented per the approved two-phase acceptance/execution design
- **Latest production commit:** `991fca3`
- **Current focus:** production Tool execution from planned Goals, and broader Task lifecycle handling

---

## Build Status

Current verified baseline:

```text
Native Gradle verification: BUILD SUCCESSFUL
```

The complete test suite must pass before an implementation unit is accepted, committed, and pushed.

---

## Test Verification

The current verified baseline is produced through the native Gradle wrapper:

```powershell
.\gradlew.bat test
```

Implementation units are accepted only after:

1. targeted implementation review;
2. full native Gradle verification;
3. clean scope inspection;
4. commit;
5. push;
6. clean working-tree confirmation.

---

## Recommended Reading Order

1. Parker Constitution
2. Project Governance
3. Parker Engineering Standard (PES-001)
4. Architecture Decisions
5. Trust Framework
6. Reasoning Context
7. Runtime specifications
8. Governance Reviews
9. Contract Designs
10. Scope Locks
11. Implementation history and implementation gaps

---

## Repository Structure

```text
docs/
    adr/              Architecture Decision Records
    architecture/      Governance Reviews, Contract Designs, architecture decisions
    development/
    diagrams/
    engineering/
    glossary/
    governance/
    implementation/    Scope Locks, implementation history, implementation gaps
    interfaces/
    process/
    reference/
    release/
    reviews/
    roadmap/
    schemas/
    security/
    specifications/

src/
    composition/       Production composition root
    contracts/         Frozen, governed data and interface contracts
    interfaces/
    runtime/           Concrete runtime implementations and coordinators

tests/
    composition/
    contracts/
    runtime/

agents/
examples/
plugins/
tools/
```

---

## Current Repository Snapshot

| Item | Status |
|---|---|
| Architecture | Constitutional Foundation (Frozen) |
| Implementation | Controlled Agent Run Submission complete |
| Latest Commit | `991fca3` — `feat(runtime): implement approved two-phase agent run submission` |
| Build Status | `BUILD SUCCESSFUL` |
| Branch | `main` |
| Repository | Clean • Synced with origin |

---

## Roadmap

Parker is being developed in deliberate stages:

1. Constitutional Architecture ✅
2. Runtime Foundation ✅
3. Reasoning Context integration ✅
4. Production conversation composition ✅
5. Reply composition and delivery ✅
6. Goal handoff and Plan Candidate generation ✅
7. Planner Runtime production integration ✅
8. Controlled Agent Run submission ✅
9. Workflow Engine
10. Plugins and richer tools
11. Android product integration
12. Multi-device production platform

The controlled transition from an authorised Task Proposal into Agent Run submission and synchronous execution is now implemented:

```text
PlanningSessionResult
        ↓
TaskProposal
        ↓
AgentRunCommand
        ↓
Authorised execution
```

This does not make Agent Execution complete overall. The next unresolved boundary is:

- production Tool execution from planned Goals;
- broader Task lifecycle completion and failure handling;
- workflow orchestration.

That transition must continue to preserve the constitutional separation between cognition, trust, and execution.

---

## Contributing

Parker remains in active foundational development.

Contributors interested in:

- runtime systems;
- Android;
- AI;
- security;
- distributed systems;
- local-first computing;
- developer tooling;
- personal intelligence platforms

are welcome.

Before submitting significant changes, read the constitutional documents and Parker Engineering Standard.

Contributions must preserve Parker's core guarantees:

- Owner authority
- Trust-first execution
- Model independence
- Local-first operation
- Auditable runtime behaviour
- Architecture-led implementation

---

# Vision

Parker is not another chatbot. It is a governed runtime platform for trustworthy personal intelligence.

It is an attempt to build one where the owner remains in control, AI remains replaceable, and trust is enforced by architecture rather than promised through policy.

The Runtime Foundation, production conversation path, Reasoning Context integration, reply delivery, Goal handoff, Plan Candidate generation, and Planner Runtime integration now exist as working, verified parts of the platform.

The next phase is not to loosen Parker's safeguards in the name of capability. It is to extend capability while preserving them.

Parker is no longer solely an architectural vision. It is a functioning, governed runtime platform — constitutional principles enforced by real, verified code, extended one governed engineering unit at a time. Future work will extend what Parker can do without loosening the constitutional guarantees that already govern what it does today.

The future of personal AI should not belong only to the companies that build the models.

**It should belong to the people who use them.**
