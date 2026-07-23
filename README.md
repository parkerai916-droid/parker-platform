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

Today's AI assistants are powerful, but they all share the same assumptions:

- Your conversations belong in someone else's cloud.
- Your memories belong in someone else's database.
- Your automation depends on someone else's service.
- Your intelligence is tied to someone else's model.

Parker was created to reverse those assumptions.

Instead of asking users to trust a company, Parker is designed so trust is enforced by architecture.

---

## What Makes Parker Different

### Trust-First Architecture

Every action is evaluated through a dedicated Trust and Permission system.

Reasoning models never execute actions directly.

> **AI can propose. Parker must authorise. Runtime executes.**

### Owner Authority

Parker treats the owner as the constitutional authority.

No plugin, agent, language model, or external service can bypass that authority.

### Model Independent

Parker is not built around any single AI model.

Reasoning engines are replaceable components. Today that might be:

- Qwen
- Gemma
- Claude
- GPT
- Llama

Tomorrow it may be something that does not exist yet.

Parker remains the platform.

### Local-First

Parker is designed to operate locally whenever possible.

Privacy is the default.

Cloud services are treated as optional capabilities, not mandatory infrastructure.

### Plugin-Based

Capabilities are added through plugins rather than hardcoded into the core platform.

This allows Parker to evolve without compromising its architectural guarantees.

### Constitutional Architecture

Parker is governed by published architectural principles.

Core guarantees cannot be casually bypassed by implementation shortcuts.

> **Architecture drives implementation, not the other way around.**

---

# What Works Today

Parker has completed its constitutional foundation and has substantially progressed through the core runtime, implemented through disciplined engineering sprints governed by the Parker Engineering Standard (**PES-001**). Implementation is now well into **Sprint 10**.

Completed runtime work includes:

Trust and execution substrate:
- Identity-aware Permission Engine
- Permission Policy model
- Execution Pipeline
- Tool Registry
- Tool Invocation Binding
- Resource Registry
- Task Runtime (Task Manager Runtime)
- Multi-step Agent Runtime
- Event-driven runtime coordination

Communication and conversation orchestration (Sprints 7-10):
- Local Text Channel (deliver Tool, registered and end-to-end verified)
- Communication Intake
- Conversation Engine
- CommunicationConversationCoordinator (Communication → Conversation)
- ConversationTurnReasoningCoordinator (Turn → Reasoning)
- Model-backed Reasoning Provider
- ResponseComposer
- ReplyDeliveryCoordinator
- ConversationReplyCoordinator

Together, these implement a tested, orchestration-only path from an accepted inbound message, through conversational reasoning, to a composed and delivered reply -- each unit verified individually and through real-stack, end-to-end tests. This is not yet a live, production entry point -- see Not Yet, below.

## Milestone: Conversation-to-Reply Orchestration

The Parker runtime now supports, building on the identity-aware, permission-gated Runtime Foundation established earlier:

- Identity-aware, permission-gated execution
- Multi-step agent runs, with suspend / resume / cancel semantics
- Event-driven runtime coordination
- An accepted inbound message carried, through a chain of thin, Scope-Locked coordinators, to a reasoned, composed, and delivered reply

This does not mean the runtime is production complete. No composition root, Goal/Planner routing, or production `ReasoningContext` assembly exists yet.

Current verified baseline:

- **Implementation Phase:** Sprint 10
- **Latest Completed Unit:** ConversationReplyCoordinator
- **Latest verification:** Android Studio — **612 / 612 tests passing**, BUILD SUCCESSFUL

---

## Not Yet

Parker is not yet a finished consumer assistant, and its runtime orchestration is not yet wired into anything that runs in production.

The following areas are still under development:

- Production composition root (nothing in this repository instantiates or wires the runtime chain at real startup)
- Goal routing from reasoning output to Planner Runtime
- Production `ReasoningContext` ownership and assembly
- Live validation of `LocalHttpModelInferenceClient` against a real model server
- Long-term Memory integration into reasoning (an isolated in-memory store exists; not yet connected to Conversation Engine or Reasoning Provider)
- World Model integration into reasoning (an isolated in-memory store exists; not yet connected to Conversation Engine or Reasoning Provider)
- Planner Runtime integration into the conversation path (an isolated in-memory decision mechanism exists; nothing routes a `Goal` to it)
- Workflow Engine
- Complete Android runtime
- Production-ready plugin ecosystem
- Multi-device deployment
- Public developer SDK
- Production security hardening
- End-user release builds

The platform is being built deliberately from the constitutional foundation upward.

---

# Architecture Overview

Parker separates intelligence into three distinct responsibilities:

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

The core execution principle is:

> **Cognition proposes. Trust authorises. Runtime executes.**

---

## Runtime Architecture

The runtime now provides a trust-governed execution substrate, and a separately-verified, tested chain of orchestration coordinators carrying an inbound message through to a delivered reply. The two are architecturally connected but not yet wired together at a real, running entry point.

**Trust-governed execution substrate:**

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

**Conversation-to-reply orchestration (implemented and tested; not yet production-wired):**

```text
   CommunicationIntake
           │  accept / reject
           ▼
   CommunicationConversationCoordinator ──► ConversationTurnReasoningCoordinator
           │  submitAndReason               ──► ConversationEngine
           │                                ──► Reasoning Provider (model-backed)
           ▼
   ConversationReplyCoordinator
           │  submitAndDeliver
           ▼
   ReplyDeliveryCoordinator
           │  composeAndDeliver
           ▼
   ResponseComposer ──► ResponseDelivery ──► Execution Pipeline ──► Tools (Local Text Channel)
```

No production composition root yet constructs this chain or calls it from a real, running conversation flow -- every coordinator above is exercised only by its own tests today, including full real-stack, end-to-end tests. Every call this chain reaches still passes through the Execution Pipeline and Permission Engine shown above; nothing in the conversation path bypasses trust authorisation.

Every execution path remains subject to constitutional authority.

Reasoning proposes.

Trust authorises.

Runtime executes.

---

## Knowledge Architecture

Parker organises knowledge into three distinct layers:

| Layer | Purpose |
|-------|---------|
| **Memory** | What Parker has learned |
| **World Model** | What Parker currently believes to be true |
| **Reasoning Context** | What matters for the current task |

Each layer has a distinct lifecycle and architectural responsibility.

---

## Project Goals

Parker aims to become a complete personal intelligence platform capable of:

- Personal assistance
- Home automation
- Workflow automation
- Knowledge management
- Long-term memory
- Local AI reasoning
- Secure tool execution
- Multi-device operation
- Plugin extensibility
- Local and hybrid deployment

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

**Current Architecture Milestone:** **Architecture v1.0 – Constitutional Foundation**

Parker's constitutional architecture is complete and frozen.

The platform is well into **Implementation Phase – Sprint 10**.

Runtime orchestration has substantially progressed. The trust-governed execution substrate (Permission Engine, Execution Pipeline, Tool Registry, Resource Registry, Task and Agent Runtimes) established through Sprint 3, and a tested, coordinator-chained path carrying an accepted inbound message through conversational reasoning to a composed and delivered reply, established across Sprints 7-10, both now exist and are verified. Production composition-root wiring, Goal/Planner Runtime routing, and production `ReasoningContext` ownership remain open.

Future implementation extends platform capability -- production wiring, Planner and Goal-routing integration, Memory and World Model integration, workflows, and additional agent capabilities -- rather than redesigning the architecture.

The constitutional architecture is no longer evolving during normal implementation.

New runtime components are expected to conform to the Constitution, Architecture Decisions, and the Parker Engineering Standard rather than redefining them.

---

## Project Governance

Parker is governed by four complementary constitutional documents:

| Document | Purpose |
|----------|---------|
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
- Explicit implementation gap management
- Engineering reviews and retrospectives as part of development

Implementation is performed as incremental, independently verified engineering units.

Every completed unit must satisfy the Definition of Complete defined by PES-001 before acceptance.

---

## Constitutional Principles

The Constitutional Foundation establishes that:

- **Parker owns authority. Modules provide capability.**
- **Cognition proposes. Trust authorises. Runtime executes.**
- **The owner remains in control.**
- **Trust is earned through architecture, not marketing.**
- **Local-first and trust-first operation are the default.**
- **User rights are protected as constitutional principles.**
- **Knowledge is organised into three layers:** Memory, World Model, and Reasoning Context.
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

- **Implementation Phase:** Sprint 10
- **Runtime Foundation:** Complete
- **Latest Completed Unit:** ConversationReplyCoordinator
- **Next Focus:** Remaining `IMPLEMENTATION_GAPS.md` #53 items -- production composition-root wiring, Goal/Planner Runtime routing, production `ReasoningContext` ownership, and live `LocalHttpModelInferenceClient` validation

---

## Build Status

Current verified baseline:

```text
Android Studio verification: 612 / 612 tests passing, BUILD SUCCESSFUL
```

---

## Test Verification

The current verified baseline is produced using Android Studio.

Until command-line verification becomes part of the documented engineering workflow, all implementation units must be verified by running the complete Android Studio test suite before acceptance.

---

## Recommended Reading Order

1. Parker Constitution
2. Project Governance
3. Parker Engineering Standard (PES-001)
4. Architecture Decisions
5. Trust Framework
6. Reasoning Context
7. Runtime specifications
8. Implementation plans, reviews, checkpoints, retrospectives, history, and implementation gaps

---

## Repository Structure

```text
docs/
    architecture/
    specifications/
    decisions/
    reviews/

runtime/

plugins/

tools/

tests/
```

---

## Roadmap

Parker is being developed in deliberate stages:

1. Constitutional Architecture
2. Runtime Foundation ✅
3. Planner
4. World Model
5. Memory
6. Workflow Engine
7. Plugins
8. Agents
9. Android Integration
10. Production Platform

Each stage builds upon guarantees established by the previous stage.

Within Runtime Foundation, both the trust-governed execution substrate (Permission Engine, Execution Pipeline, Tool Registry, Resource Registry, Task and Agent Runtimes) and a tested conversation-to-reply orchestration chain (Communication Intake through Conversation Engine, a model-backed Reasoning Provider, Response Composition, and Response Delivery) are implemented and verified as of Sprint 10. Isolated, not-yet-integrated primitives also exist ahead of schedule for Planner, World Model, and Memory (stages 3-5). Production composition-root wiring -- connecting this orchestration to a real, running entry point -- remains open.

---

## Contributing

Parker remains in its foundational engineering phase.

Contributors interested in:

- Runtime systems
- Android
- AI
- Security
- Distributed systems
- Local-first computing
- Developer tooling
- Personal intelligence platforms

are welcome.

Before submitting significant changes, read the constitutional documents and Parker Engineering Standard.

Contributions should preserve Parker's core guarantees:

- Owner authority
- Trust-first execution
- Model independence
- Local-first operation
- Auditable runtime behaviour
- Architecture-led implementation

---

# Vision

Parker is not another chatbot.

It is an attempt to build a trustworthy personal intelligence platform where the owner remains in control, AI remains replaceable, and trust is enforced by architecture rather than promised through policy.

The completion of the **Runtime Foundation** marks the transition from architectural vision to a functioning execution platform.

Future work builds on this foundation to add planning, memory, world understanding, workflows, and richer personal intelligence while preserving the constitutional principles established at the beginning of the project.

The future of personal AI should not belong only to the companies that build the models.

**It should belong to the people who use them.**
