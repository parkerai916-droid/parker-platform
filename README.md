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

The Evidence Custodian programme has also reached production integration: governed acceptance, retrieval, Memory Core registration, owner-authorised deletion, and durable deletion audit are all now constructed and wired into the same production composition root, reachable through `ParkerRuntime.submitEvidence`, `retrieveEvidence`, and `deleteEvidenceAsOwner` — see the Evidence Custodian milestone below.

Parker's memory is now durable and governed end-to-end. An explicit owner instruction to remember something is admitted into canonical Memory Core and Knowledge Item storage, survives a full runtime restart, and is retrieved into the real Reasoning Context through a governed path — literal structural matching first, falling back only when necessary to a bounded, fail-closed relevance mechanism that never gains authority over Parker's own canonical records. This has been demonstrated end-to-end, not merely in isolated tests, through Parker's own production owner-facing UI — see "Milestone: Durable Owner Memory and Governed Relevance Retrieval" below.

Parker also now has a real, production-composed owner-facing user interface — a desktop application wired directly into the same production runtime the CLI and every other entry point use, with a supported launcher script for headless/X11-forwarded server operation.

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
- World Model source
- model-backed Reasoning Provider
- ConversationReplyCoordinator

Memory and Knowledge (durable owner memory and governed retrieval):

- Memory Core (canonical assertions, entities, and provenance) with durable, replayable persistence
- Knowledge Item persistence and promotion, backed by the same durability discipline
- explicit owner `REMEMBER` recognition — deterministic, model-independent classification of an explicit persistence directive
- `MemoryAdmissionCoordinator` — governed admission of owner-directed memory into Memory Core and Knowledge Items
- canonical recovery on restart — recovered from durable storage, never resubmitted
- `DefaultReasoningKnowledgeSource` — governed retrieval into the real Reasoning Context, structural (literal) matching first
- Bounded Relevance Computation — a governed, fail-closed fallback used only when structural matching finds nothing, backed by a subordinate, shared relevance mechanism (QMD) that proposes candidate identifiers only; every candidate is independently re-verified and resolved against live canonical state before Parker discloses it
- `DefaultKnowledgeRetrieval` — the analogous governed retrieval surface for direct Knowledge queries, sharing the same relevance mechanism instance

Owner-facing interfaces:

- Local Text Channel / CLI `--interactive` owner console
- Compose Desktop owner UI (`ui-desktop`), wired directly into the real production runtime
- `scripts/run-owner-ui.sh` — the supported launcher for the owner UI, with environment/pre-flight validation and a one-shot Gradle execution model

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

Evidence custody and registration:

- Evidence Custodian (technical custody subsystem, independent of Memory Core)
- Evidence Identity (`EvidenceArtifactId`)
- write-once `EvidenceArtifactStorage`
- governed evidence acceptance (`DefaultEvidenceCustodian.accept`, Permission-Engine-gated)
- governed evidence retrieval (`DefaultEvidenceCustodian.retrieve`, Permission-Engine-gated)
- `EvidenceRegistrationCoordinator` — Runtime-layer orchestration of Evidence Custodian acceptance with Memory Core provenance and document registration, without either subsystem calling the other
- derivative relationship support, verified by behavioural tests against the real Evidence Custodian, Memory Core, and Registration Coordinator implementations
- owner-authorised evidence deletion (`OwnerEvidenceDeletionAuthority` / `DefaultOwnerEvidenceDeletionAuthority`) — a structurally separate capability from `EvidenceCustodian`, never exposed to reasoning providers or ordinary consumers
- durable, append-only deletion audit (`EvidenceDeletionAudit` / `FileSystemEvidenceDeletionAudit`) — an `AUTHORISED` record durably precedes physical deletion; a `COMPLETED` record durably follows it; a caller can never observe a successful deletion without both
- Constitutional Optimisation Safeguard enforcement, verified structurally: no class outside its one authorised holder can obtain a deletion-capable dependency, and no Evidence Custodian type declares a compact/optimise/prune/replace/discard operation
- full production Runtime Integration — `DefaultEvidenceCustodian`, `EvidenceRegistrationCoordinator`, and `DefaultOwnerEvidenceDeletionAuthority` are constructed and wired into `ParkerRuntime`, with their Resources, action-vocabulary entries, and permission rules registered, and reachable through `submitEvidence`, `retrieveEvidence`, and `deleteEvidenceAsOwner`

## Implementation Maturity

| Area | Status |
|---|---|
| Constitutional Architecture | Complete |
| Runtime Foundation | Complete |
| Conversation Pipeline | Complete |
| Reasoning Context | Complete |
| Durable Memory / Knowledge Persistence | Complete (Memory Core, Knowledge Items, explicit owner `REMEMBER`, canonical recovery) |
| Reasoning Context Relevance (Bounded Semantic Fallback / RKS.1–RKS.6) | Complete |
| Reply Delivery | Complete |
| Goal Routing | Complete |
| Planner Integration | Complete |
| Agent Execution | Controlled Submission Complete |
| Evidence Custodian | Complete (Phases 1–10; full native verification passed) |
| Owner-Facing UI | Complete (Desktop, real production runtime) |
| Document Ingestion | Not started (next development focus) |
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

Memory retrieval within this path is now durable and governed end-to-end — recovered from canonical storage after a restart, matched structurally first, and falling back to a bounded, fail-closed relevance mechanism only when structural matching finds nothing. See "Milestone: Durable Owner Memory and Governed Relevance Retrieval" below.

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

## Milestone: Evidence Custodian — Programme Complete

Parker now includes a first-class Evidence Custodian subsystem — a peer of Memory Core, never a component of Memory Core or of any future Evidence Intelligence capability. The Evidence Custodian provides technical custody of preserved original evidence artefacts, governed by the same Trust Framework that governs every other executable action in this platform. All ten phases of the Evidence Custodian Implementation Plan are complete and wired into the production composition root.

Completed:

- **Evidence Identity** — a stable, non-vacuous artefact identity (`EvidenceArtifactId`).
- **Immutable Evidence Storage** — write-once artefact storage; an accepted artefact's content can never be overwritten or replaced.
- **Governed Evidence Acceptance** — an artefact enters custody only following Permission Engine authorisation; nothing is accepted implicitly or as a side effect.
- **Governed Evidence Retrieval** — read access to a custodied artefact is itself an authorised, observational-only proposal.
- **Runtime Evidence Registration Coordinator** — a Runtime-layer coordinator sequencing Evidence Custodian acceptance with Memory Core's own provenance and document registration, so that Evidence Custodian and Memory Core remain fully independent: neither subsystem holds a reference to, or calls, the other.
- **Derivative Relationship Support** — verified by behavioural tests: an original and a derivative artefact always receive distinct identities, distinct Provenance records, and distinct Document records, with traceability preserved solely through Memory Core's existing Provenance mechanism.
- **Owner-Authorised Deletion** — the sole path by which Evidence Custodian custody ends. `OwnerEvidenceDeletionAuthority` is a structurally separate interface from `EvidenceCustodian`, implemented by a separate class (`DefaultOwnerEvidenceDeletionAuthority`) with no dependency on `EvidenceCustodian` or Memory Core; its own production entry point (`ParkerRuntime.deleteEvidenceAsOwner`) takes no caller-supplied principal at all, always acting as the configured owner.
- **Deletion Audit** — a durable, append-only audit record precedes and follows every deletion: an `AUTHORISED` record must be durably confirmed before physical deletion is even attempted, and a `COMPLETED` record must be durably confirmed before a caller can ever observe a successful result.
- **Optimisation Safeguard** — verified structurally, not merely by convention: no class outside its one authorised holder can obtain a reference to a deletion-capable dependency anywhere in the production or composition packages, and no Evidence Custodian type declares a compact, optimise, prune, replace, or discard operation of any kind.
- **Runtime Integration** — `DefaultEvidenceCustodian`, `EvidenceRegistrationCoordinator`, and `DefaultOwnerEvidenceDeletionAuthority` are constructed in `ParkerRuntime`'s production composition root via dependency injection, with their Resources, action-vocabulary entries, and permission rules registered, and reachable through three production entry points: `submitEvidence`, `retrieveEvidence`, and `deleteEvidenceAsOwner`.

```text
Owner
  ↓
EvidenceCustodian.accept()
  ↓
Permission evaluation
  ↓
MemoryCore.createProvenance()
  ↓
Permission evaluation
  ↓
MemoryCore.registerDocument()
  ↓
Coordinated result

Owner
  ↓
OwnerEvidenceDeletionAuthority.deleteAsOwner()
  ↓
Permission evaluation
  ↓
Durable AUTHORISED audit record
  ↓
EvidenceArtifactStorage.delete()
  ↓
Durable COMPLETED audit record
  ↓
Deleted
```

This milestone completes Parker's constitutional evidence foundation. Original evidence is preserved immutably, governed through explicit authorisation, registered independently of Memory Core, deletable only through an owner-only, durably audited path, structurally protected against optimisation-motivated destruction, and fully wired into the production runtime — not merely proven correct in isolated tests.

### Evidence Custodian Programme Status

The Evidence Custodian programme is complete. Parker now provides constitutional evidence identity, immutable evidence storage, governed evidence acceptance, governed evidence retrieval, independent Memory Core registration, owner-authorised evidence deletion, durable append-only deletion audit, structural Optimisation Safeguards, and full production runtime integration — each governed by its own frozen Contract Design, Scope Lock, and Implementation Plan, and each verified against the real, wired production graph rather than isolated tests alone.

This closes the constitutional Evidence Custodian layer. Any future evidence-related work — analysis, interpretation, OCR, comparison, or any other capability consuming custodied evidence — belongs to a higher-level capability such as Evidence Intelligence, governed separately and later, never to the Evidence Custodian itself.

---

## Milestone: Durable Owner Memory and Governed Relevance Retrieval

Parker now provides durable owner memory, recoverable across a full restart, retrieved into real reasoning through a governed path that never lets a subordinate search component gain authority over canonical state.

**Persistence.** An explicit owner instruction to remember something is recognised deterministically (no model call is required to classify it), admitted through `MemoryAdmissionCoordinator` into canonical Memory Core and Knowledge Item storage, and durably written. A full runtime restart recovers this state from durable storage — it is never resubmitted.

**Retrieval.** `DefaultReasoningKnowledgeSource` governs what reaches the real Reasoning Context:

```text
Owner message
      │
      ▼
Structural (literal) match against canonical Memory / Knowledge content
      │
      ├── found  → disclosed
      │
      └── nothing found
             │
             ▼
      Bounded Relevance Computation (fallback only)
             │
             ▼
      QMD — derivative, search-only relevance mechanism
             │
             ▼
      candidate identifiers (never content, never authority)
             │
             ▼
      Parker independently re-verifies and resolves every candidate
      against live canonical Memory Core / Knowledge Item state
             │
             ▼
      Parker governance decides what, if anything, is disclosed
```

Structural matching always runs first and is never bypassed. Bounded Relevance Computation runs only when structural matching genuinely finds nothing, using a relevance mechanism (QMD) shared, unchanged, between Parker's Knowledge Retrieval and Reasoning Context surfaces. That mechanism proposes opaque candidate identifiers only — it cannot create, modify, delete, or redefine Parker memory, and it never sees or returns canonical content. Token resolution is fail-closed (an unknown, duplicate, or excess candidate is rejected, never silently repaired), and every surviving candidate is re-verified against current permission and canonical state immediately before disclosure, not from an earlier snapshot.

**Demonstrated, not merely tested.** This complete path — an explicit `REMEMBER` instruction, durable persistence, a full runtime restart, canonical recovery, and a naturally-phrased recall question answered correctly from the recovered memory — was demonstrated successfully through Parker's real, production owner-facing UI, not only through automated tests or the CLI. A small number of tests that require a live, locally-provisioned relevance component are skipped by default in ordinary verification and run separately.

**Owner-facing UI.** A Compose Desktop owner UI is wired directly into the same production runtime every other entry point uses. `scripts/run-owner-ui.sh` is the supported launcher for running it against the real production stores, with pre-flight checks and a one-shot Gradle execution model chosen deliberately so the launched process's permissions are never inherited from a stale background build daemon. Convenient one-click launching from an owner's own Windows workstation is supported as a local, owner-machine arrangement — a Windows launcher starts an X server, opens an SSH session authenticated by key (never a stored password) with X11 forwarding, and runs `scripts/run-owner-ui.sh` on the server — entirely outside this repository, since it is specific to one owner's own machine.

---

## Current Verified Baseline

- **Architecture milestone:** Architecture v1.0 — Constitutional Foundation
- **Implementation status:** Controlled Agent Run Submission complete; Evidence Custodian programme complete (Phases 1–10); durable owner memory and governed relevance retrieval (RKS.1–RKS.6) complete; owner-facing UI operational
- **Latest repository commit:** `68faf46` — `fix(ui): make run-owner-ui.sh immune to a stale wrong-group Gradle daemon` (a bounded, launcher-only shell-script correction)
- **Latest production implementation milestone:** Reasoning Knowledge Source — Bounded Semantic Relevance (RKS.1–RKS.6), commit `7a1678c` — `feat(reasoning-context): implement RKS.1-RKS.6 bounded semantic relevance fallback`
- **Latest full native regression baseline:** 2,290 tests, 0 failures, 0 errors, 7 skipped — established at commit `7a1678c`. The commits after it touch only the owner-UI launcher shell script, not Kotlin or Gradle build configuration, and were verified individually rather than by re-running the full suite.
- **Build result:** `BUILD SUCCESSFUL`
- **Repository state:** `main` synchronized with `origin/main`; working tree clean

---

# What Is Not Yet Complete

Parker is not yet a finished consumer assistant.

Controlled Agent Run submission is now live, and synchronous execution through the Agent Runtime is now wired into the production path. The remaining boundary is the transition from an accepted, executing Agent Run into real Tool execution against planned Goals, and the broader Task lifecycle behaviour that follows an Agent Run's outcome.

Still under development:

- document ingestion — transforming external documents (PDFs, scans, email, images, and similar) into governed, Parker-consumable Memory/Knowledge content
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

Document ingestion is a distinct capability from what already exists, and the boundary matters: the Evidence Custodian already provides governed custody of artefacts an owner explicitly submits. Memory and Knowledge now provide durable canonical owner memory and governed retrieval for what an owner explicitly tells Parker to remember. Reasoning Context now includes bounded semantic relevance for recalling that memory. None of this means Parker can read an arbitrary document and turn its contents into governed knowledge on its own — that transformation, from external documents into Parker-consumable Memory/Knowledge, is the next planned development programme and does not exist yet.

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
| **Memory** | What Parker has learned — durably, in canonical Memory Core and Knowledge Item storage |
| **World Model** | What Parker currently believes to be true |
| **Reasoning Context** | What matters for the current turn or task |

Production Reasoning Context assembly now draws from Conversation History, Memory, and World Model sources while preserving their distinct ownership and lifecycle boundaries.

### Memory, Knowledge, and governed relevance

Parker's Memory Core and Knowledge Items are the sole canonical, authoritative record of what Parker has been told to remember. A relevance mechanism (currently QMD) may be consulted, but only as a subordinate, derivative, search-only fallback — never as a second store of truth:

```text
Canonical Memory Core / Knowledge Items (authoritative)
      │
      ▼
Structural retrieval (always first)
      │
      └── fallback only, when nothing structural is found
             │
             ▼
      QMD — derivative, search-only relevance mechanism
             │
             ▼
      candidate identifiers (never content, never authority)
             │
             ▼
      Parker re-verifies and resolves each candidate against
      canonical state, then governs disclosure
```

QMD cannot create, modify, delete, promote, or redefine Parker memory, and it never determines what Parker discloses. See "Milestone: Durable Owner Memory and Governed Relevance Retrieval," above, for the full retrieval path.

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
- **Sprint status:** Controlled Agent Run Submission complete; Evidence Custodian programme complete (Phases 1–10); durable owner memory and governed relevance retrieval (RKS.1–RKS.6) complete; owner-facing UI operational
- **Latest repository commit:** `68faf46` — a bounded, launcher-only shell-script correction; see "Current Verified Baseline" for why this is distinguished from the production milestone below
- **Latest production implementation milestone:** Reasoning Knowledge Source — Bounded Semantic Relevance, commit `7a1678c`, verified by behavioural and structural tests against the real `DefaultReasoningKnowledgeSource`, `DefaultKnowledgeRetrieval`, shared relevance mechanism, and production composition root, and by a real owner-facing UI acceptance
- **Latest full native regression baseline:** 2,290 tests, 0 failures, 0 errors, 7 skipped (see "Build Status")
- **Current focus:** document ingestion — transforming external documents into governed, Parker-consumable Memory/Knowledge content; production Tool execution from planned Goals and broader Task lifecycle handling remain separately open

---

## Build Status

Latest full native regression baseline, at commit `7a1678c`:

```text
Native Gradle verification: BUILD SUCCESSFUL
2,290 tests, 0 failures, 0 errors, 7 skipped
```

The commits on `main` after `7a1678c` are a bounded, owner-UI-launcher-only correction (`scripts/run-owner-ui.sh`, a shell script) and did not require, and were not verified by, a full suite re-run.

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

The current verified baseline includes dedicated behavioural verification for the Evidence Registration Coordinator and for Derivative Relationship Support — confirming that an original and a derivative artefact always receive distinct identities, with traceability preserved solely through Memory Core's own Provenance mechanism — together with dedicated verification for owner-authorised deletion (including durable audit ordering under simulated failure), structural verification of the Constitutional Optimisation Safeguard, and end-to-end verification of the wired production composition root, exercised against real file-backed storage and a real permission graph rather than test fakes.

It also includes dedicated verification of durable Memory Core and Knowledge Item persistence and recovery, structural-first governed retrieval, fail-closed candidate-token minting and resolution, and pre-disclosure re-verification against live canonical state — together with a small number of tests, skipped by default, that require a live, locally-provisioned relevance component and are run separately from ordinary verification.

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

ui-desktop/         Compose Desktop owner UI, wired to the real production runtime

agents/
examples/
plugins/
scripts/            Supported operational launchers (e.g. run-owner-ui.sh)
tools/
```

---

## Current Repository Snapshot

| Item | Status |
|---|---|
| Architecture | Constitutional Foundation (Frozen) |
| Implementation | Controlled Agent Run Submission complete; Evidence Custodian programme complete; durable owner memory and governed relevance retrieval (RKS.1–RKS.6) complete; owner-facing UI operational |
| Latest Repository Commit | `68faf46` — `fix(ui): make run-owner-ui.sh immune to a stale wrong-group Gradle daemon` |
| Latest Production Milestone | `7a1678c` — `feat(reasoning-context): implement RKS.1-RKS.6 bounded semantic relevance fallback` |
| Build Status | `BUILD SUCCESSFUL` (2,290 tests, 0 failures, 0 errors, 7 skipped, at `7a1678c`) |
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
9. Evidence Custodian ✅
10. Durable owner memory and governed relevance retrieval (Memory Core, Knowledge Items, RKS.1–RKS.6) ✅
11. Owner-facing UI ✅
12. Document ingestion ← next
13. Workflow Engine
14. Plugins and richer tools
15. Android product integration
16. Multi-device production platform

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

### Evidence Custodian Programme

Developed as a separate, parallel infrastructure programme, governed by its own Contract Design, Scope Lock, and Implementation Plan:

1. Evidence Identity ✅
2. Immutable Evidence Storage ✅
3. Governed Evidence Acceptance ✅
4. Governed Evidence Retrieval ✅
5. Runtime Evidence Registration Coordinator ✅
6. Derivative Relationship Support ✅ (verified by behavioural tests)
7. Deletion workflow ✅ (owner-only `OwnerEvidenceDeletionAuthority`, durably audited)
8. Optimisation Safeguard enforcement ✅ (verified structurally, not by convention)
9. Platform-wide verification ✅ (completed through cumulative native verification across Phases 7–10; full native Gradle suite passed at that time with 1,191 tests, 0 failures, and 0 errors — see "Current Verified Baseline" for the current count)
10. Runtime integration ✅

Evidence Custodian is now fully wired into `ParkerRuntime`'s production composition root: `DefaultEvidenceCustodian`, `EvidenceRegistrationCoordinator`, and `DefaultOwnerEvidenceDeletionAuthority` are constructed via dependency injection, their Resources/action-vocabulary/permission rules are registered, and the subsystem is reachable through `submitEvidence`, `retrieveEvidence`, and `deleteEvidenceAsOwner`. Controlled Tool execution from planned Goals remains this platform's own separate, unresolved boundary — see "What Is Not Yet Complete," above.

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

The Runtime Foundation, production conversation path, Reasoning Context integration, reply delivery, Goal handoff, Plan Candidate generation, Planner Runtime integration, durable owner memory with governed relevance retrieval, and an owner-facing user interface now exist as working, verified parts of the platform.

The next phase is not to loosen Parker's safeguards in the name of capability. It is to extend capability while preserving them.

Parker is no longer solely an architectural vision. It is a functioning, governed runtime platform — constitutional principles enforced by real, verified code, extended one governed engineering unit at a time. Future work will extend what Parker can do without loosening the constitutional guarantees that already govern what it does today.

The future of personal AI should not belong only to the companies that build the models.

**It should belong to the people who use them.**
