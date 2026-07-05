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

* Your conversations belong in someone else's cloud.
* Your memories belong in someone else's database.
* Your automation depends on someone else's service.
* Your intelligence is tied to someone else's model.

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

* Qwen
* Gemma
* Claude
* GPT
* Llama

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

## What Works Today

Parker is currently in foundational implementation.

The constitutional architecture is complete, and core runtime components are being implemented through structured engineering sprints.

Completed runtime work includes:

* Identity-aware Permission Engine
* Permission Policy model
* Execution Pipeline integration
* Tool Registry
* Tool Invocation Binding
* Task Manager Agent Event Subscription
* Task lifecycle transitions
* Resource Registry foundation

Current verified baseline:

* **Implementation Phase:** Sprint 2
* **Track A:** Complete
* **Track B:** In Progress
* **Latest verification:** Android Studio — **269 / 269 tests passing**

---

## Not Yet

Parker is not yet a finished consumer assistant.

The following areas are still under development:

* Full Android runtime
* Complete agent framework
* Production-ready plugin ecosystem
* End-user installation flow
* Long-term Memory implementation
* Complete World Model implementation
* Multi-device deployment
* Public developer SDK
* Production security hardening
* User-facing release builds

The project is being built deliberately from the constitutional foundation upward.

---

## Architecture Overview

Parker separates intelligence into three distinct responsibilities:

```text
                +----------------------+
                |      Reasoning       |
                |  "What should happen?"|
                +----------+-----------+
                           |
                           v
                +----------------------+
                |     Trust Engine     |
                |  "Is this permitted?"|
                +----------+-----------+
                           |
                           v
                +----------------------+
                | Execution Pipeline   |
                |   "Make it happen."  |
                +----------+-----------+
                           |
                           v
                Tools • Plugins • Devices • Services
```

The core execution principle is:

> **Cognition proposes. Trust authorises. Runtime executes.**

---

## Knowledge Architecture

Parker organises knowledge into three distinct layers:

| Layer                 | Purpose                                   |
| --------------------- | ----------------------------------------- |
| **Memory**            | What Parker has learned                   |
| **World Model**       | What Parker currently believes to be true |
| **Reasoning Context** | What matters for the current task         |

Each layer has a different lifecycle and responsibility.

---

## Project Goals

Parker aims to become a complete personal intelligence platform capable of:

* Personal assistance
* Home automation
* Workflow automation
* Knowledge management
* Long-term memory
* Local AI reasoning
* Secure tool execution
* Multi-device operation
* Plugin extensibility
* Local and hybrid deployment

---

## Design Principles

Parker is built around non-negotiable principles:

* Owner authority
* Trust before execution
* Local-first operation
* Model independence
* Plugin extensibility
* Explicit permissions
* Explainable decisions
* Constitutional governance
* Auditable runtime behaviour
* Replaceable reasoning providers

---

## Current Architecture Status

**Current Architecture Milestone:** Architecture v1.0 – Constitutional Foundation

Parker's constitutional architecture is complete.

The platform has entered **Implementation Phase – Sprint 2**, where the constitutional architecture is being translated into verified runtime components through incremental engineering units governed by the Parker Engineering Standard (**PES-001**).

The constitutional architecture is no longer evolving during normal implementation.

New runtime components are expected to conform to the Constitution, Architecture Decisions, and Engineering Standard rather than redefining them.

---

## Project Governance

Parker is governed by four complementary constitutional documents:

| Document                                  | Purpose                                                                                     |
| ----------------------------------------- | ------------------------------------------------------------------------------------------- |
| **Parker Constitution**                   | Defines what Parker is.                                                                     |
| **Architecture Decisions**                | Define how Parker is architected and why major decisions were made.                         |
| **Parker Engineering Standard (PES-001)** | Defines how Parker is engineered, verified, reviewed, and evolved.                          |
| **Project Governance**                    | Defines the relationship between the governing documents and the platform governance model. |

Future Architecture Decisions that alter engineering practice must explicitly reference the relevant PES-001 section.

---

## Engineering Standard

Parker Engineering Standard (**PES-001**) governs the engineering lifecycle used to develop the platform.

The standard establishes:

* Architecture before implementation.
* Evidence before opinion.
* Verification before acceptance.
* Documentation as a first-class engineering artefact.
* Explicit implementation gap management.
* Engineering reviews and retrospectives as part of development.

Implementation is performed as incremental, independently verified engineering units.

Every completed unit must satisfy the Definition of Complete defined by PES-001 before acceptance.

---

## Constitutional Principles

The Constitutional Foundation establishes that:

* **Parker owns authority. Modules provide capability.**
* **Cognition proposes. Trust authorises. Runtime executes.**
* **The owner remains in control.**
* **Trust is earned through architecture, not marketing.**
* **Local-first and trust-first operation are the default.**
* **User rights are protected as constitutional principles.**
* **Knowledge is organised into three layers:** Memory, World Model, and Reasoning Context.
* **Reasoning providers are model-agnostic and interchangeable.**
* **No module may grant itself authority or bypass the Trust Framework.**

---

## Constitutional Documents

The constitutional foundation is defined by the following documents:

* [Architecture History](docs/architecture/ARCHITECTURE_HISTORY.md)
* [Parker Constitution](docs/architecture/parker-constitution.md)
* [Architecture Decisions](docs/architecture/ARCHITECTURE_DECISIONS.md)
* [Parker Engineering Standard (PES-001)](docs/architecture/PARKER_ENGINEERING_STANDARD.md)
* [Project Governance](docs/architecture/PROJECT_GOVERNANCE.md)
* [User Authorship & Evidence](docs/architecture/user-authorship-and-evidence.md)
* [Reasoning Context](docs/architecture/reasoning-context.md)
* [Trust Framework](docs/architecture/09-trust-framework.md)

---

## For Developers

### Current Sprint

Parker is currently in:

* **Implementation Phase:** Sprint 2
* **Track A:** Complete
* **Track B:** In Progress

### Build Status

Current verified baseline:

```text
Android Studio verification: 269 / 269 tests passing
```

### Test Command

The current verified test baseline is from Android Studio.

Until a command-line test command is formally documented in the repository, use Android Studio to run the full project test suite and verify that all tests pass before accepting implementation work.

### Where to Start Reading

Recommended reading order:

1. [Parker Constitution](docs/architecture/parker-constitution.md)
2. [Project Governance](docs/architecture/PROJECT_GOVERNANCE.md)
3. [Parker Engineering Standard (PES-001)](docs/architecture/PARKER_ENGINEERING_STANDARD.md)
4. [Architecture Decisions](docs/architecture/ARCHITECTURE_DECISIONS.md)
5. [Trust Framework](docs/architecture/09-trust-framework.md)
6. [Reasoning Context](docs/architecture/reasoning-context.md)
7. Implementation plans, gaps, history, reviews, checkpoints, and retrospectives

---

## Repository Structure

```text
docs/
    architecture/
    specifications/
    decisions/

runtime/

plugins/

tools/

tests/
```

---

## Roadmap

Parker is being developed in deliberate stages:

1. Architecture
2. Runtime
3. Trust
4. Execution
5. Resources
6. World Model
7. Memory
8. Plugins
9. Agents
10. Android
11. Production

Every stage builds on the guarantees established by the previous one.

---

## Contributing

Parker is still in its foundational phase.

Contributors interested in architecture, runtime systems, Android, AI, security, distributed systems, developer tooling, local-first systems, and trustworthy automation are welcome.

Before submitting significant changes, read the constitutional documents and engineering standard.

Contributions should preserve Parker's core guarantees:

* Owner authority
* Trust-first execution
* Model independence
* Local-first operation
* Auditable runtime behaviour
* Architecture-led implementation

---

## Vision

Parker is not another chatbot.

It is an attempt to build a trustworthy personal intelligence platform where the owner remains in control, AI remains replaceable, and trust is enforced by architecture rather than promised through policy.

The future of personal AI should not belong only to the companies that build the models.

It should belong to the people who use them.
