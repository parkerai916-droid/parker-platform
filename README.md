## Architecture Status

**Current Architecture Milestone:** **Architecture v1.0 – Constitutional Foundation**

Parker's constitutional architecture is now established.

## Engineering Standard

Parker Engineering Standard (PES-001) governs the engineering process for developing the platform. Future Architecture Decisions that alter engineering practice should cite the relevant PES-001 section.

The Constitution defines what Parker is. Architecture Decisions define how Parker is structured. PES-001 defines how Parker is engineered.

This milestone defines the immutable principles that govern the platform and provides the foundation for all future implementation. From this point forward, new features, runtime components, plugins, reasoning providers, and services must conform to Parker's constitutional architecture rather than redefining it.

### Constitutional Principles

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

### Constitutional Documents

The constitutional foundation is defined by the following architecture documents:

- [Architecture History](docs/architecture/ARCHITECTURE_HISTORY.md)
- [Parker Constitution](docs/architecture/parker-constitution.md)
- [User Authorship & Evidence](docs/architecture/user-authorship-and-evidence.md)
- [Reasoning Context](docs/architecture/reasoning-context.md)
- [Trust Framework](docs/architecture/09-trust-framework.md)

### Governance

Future architecture and implementation must comply with these constitutional principles.

Constitutional documents are considered foundational architecture and may only be modified through explicit architectural review. All implementation is expected to preserve these principles while remaining modular, model-agnostic, and replaceable.