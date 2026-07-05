# Reasoning Context

**Status:** Constitutional — subordinate to `parker-constitution.md`
**Applies to:** Parker's knowledge architecture — Memory, the World Model, and reasoning providers

---

## Purpose

This document completes Parker's knowledge architecture by defining the third and final knowledge layer: Reasoning Context. Together with Memory and the World Model, Reasoning Context forms the full picture of what Parker knows, believes, and is currently working with. This document exists to state, precisely, what each layer is for, why they must remain separate, and how information is allowed to move between them.

Parker already establishes Memory as long-term knowledge and the World Model as live state. What has not been made explicit is the temporary, task-scoped layer that sits between them and the reasoning provider actually doing the work at any given moment. That layer is Reasoning Context, and it is defined here in relation to, not in replacement of, Memory and the World Model.

## Scope

This document governs the three knowledge layers used in reasoning — Memory, the World Model, and Reasoning Context — and the rules by which information flows between them and into a reasoning provider. It does not govern how any individual reasoning provider is implemented, nor does it govern authorization or execution, which remain the domain of the Permission Engine and the Execution Pipeline as established in the Parker Constitution. This document is about what a reasoning provider is given to work with, not about what it is allowed to do with the resulting proposal.

## Core Principles

Parker's knowledge is organized into three layers, each with a distinct purpose, lifespan, and role.

### Memory — "What Parker has learned."

Memory is Parker's long-term knowledge store. It holds durable facts, preferences, prior context, and history that remain relevant beyond a single task or session. Memory is intentionally slow to change: information enters it deliberately, not automatically, and it persists across tasks, sessions, and time.

### World Model — "What Parker believes is true right now."

The World Model is Parker's live, current understanding of the state of the world relevant to the user — device state, environment, ongoing tasks, and other facts that change over time and must be kept current. Unlike Memory, the World Model is expected to change frequently, driven by sensors and other live inputs. It represents Parker's best current belief, not a permanent record.

### Reasoning Context — "What matters for the current task."

Reasoning Context is the temporary, task-scoped working set that a reasoning provider actually reasons over. It is assembled specifically for the task at hand, drawing on whatever subset of Memory and the World Model is relevant, plus any information supplied directly as part of the current request. Reasoning Context may also include user-selected documents, files, emails, images, recordings, or other resources explicitly supplied for the current task. These remain part of the Reasoning Context only unless deliberately promoted under Parker's memory policy. It exists only for the duration of the task and is discarded once the task concludes.

### Why the three layers must remain separate

Each layer answers a different question — what has been learned, what is currently true, and what matters right now — and conflating them degrades all three. If Reasoning Context were allowed to blend indiscriminately into Memory, Parker's long-term knowledge would accumulate noise from every transient task it ever performed. If the World Model were treated as permanent, it would ossify around momentary conditions that stop being true. If Memory and the World Model were not filtered down before reaching a reasoning provider, every task would be reasoned over with irrelevant, excess context that dilutes the quality of reasoning and increases the chance of an incorrect or overconfident proposal. Separation is what keeps each layer fit for the purpose it exists to serve.

### Information flow

Information moves through Parker's knowledge layers in one direction, task by task:

```
Sensors              -> World Model
Memory + World Model -> Reasoning Context
Reasoning Context    -> Reasoning Provider
Reasoning Provider   -> Trust Engine
Trust Engine         -> Execution Pipeline
```

Sensors and other live inputs update the World Model continuously. When a task begins, the relevant portions of Memory and the World Model are assembled into a fresh Reasoning Context scoped to that task. The reasoning provider — whichever model or engine is configured — reasons over that context and produces a proposal. That proposal is then evaluated for authorization by the trust-authorising stage of Parker's architecture (carried out by the Permission Engine, consistent with the Parker Constitution's principle that trust authorises), and only an authorized proposal reaches the Execution Pipeline. Nothing in this flow allows a reasoning provider to write back into Memory or the World Model directly; any such change is a separate, governed act, not a side effect of reasoning.

### Reasoning Context is ephemeral

Reasoning Context exists only for as long as the task it was assembled for. Once the task completes — successfully, unsuccessfully, or abandoned — its Reasoning Context is discarded. It is not retained, cached for reuse in unrelated future tasks, or treated as a record of what Parker now knows.

### Promotion into Memory is never automatic

Nothing in Reasoning Context is written into Memory simply because it was used, discussed, or reasoned over during a task. Moving information from a transient task into Parker's durable knowledge is a deliberate act governed by Parker's memory policy, which decides what is worth retaining, in what form, and for how long. Reasoning Context influences the task it was built for and then disappears; it does not silently become Parker's permanent understanding of the user or the world.

## Design Goals

- Give every reasoning provider a clean, task-scoped working set rather than unfiltered access to everything Parker has ever stored or currently believes.
- Prevent long-term knowledge from being polluted by information that was only ever relevant to a single, transient task.
- Keep the World Model responsive to live conditions without requiring it to also serve as a permanent record.
- Make the boundary between "used in reasoning" and "retained in Memory" an explicit, governed decision rather than an implicit side effect.
- Keep this layered structure independent of any specific reasoning provider, so that reasoning providers remain interchangeable services rather than becoming entangled with how Parker stores or manages knowledge.

## Architectural Responsibilities

- **Memory** is responsible for durable storage and retrieval of long-term knowledge, and for exposing only the portions relevant to a given task when Reasoning Context is assembled.
- **The World Model** is responsible for maintaining an accurate, current picture of relevant state, kept up to date by sensors and other live inputs, and for exposing the current-state slice relevant to a given task.
- **Reasoning Context assembly** is responsible for combining the relevant portions of Memory and the World Model with the specifics of the current task into a single, bounded working set, and for discarding that working set once the task concludes.
- **Reasoning providers** are responsible for reasoning over the Reasoning Context they are given and producing a proposal. They have no standing access to Memory or the World Model outside what has been assembled into their Reasoning Context, and no ability to authorize their own proposals or write directly back into Memory or the World Model.
- **Parker's memory policy** is responsible for deciding what, if anything, from a completed task is promoted into Memory. This decision is separate from, and subsequent to, the reasoning that occurred during the task.

## Relationship to Existing Parker Components

This document specializes the cognition stage of the Parker Constitution — "Cognition proposes" — by defining precisely what cognition is given to reason over. It does not change the roles of the Permission Engine, the Execution Pipeline, the Tool Registry, the Identity Service, or the Resource Registry. The trust-authorising stage referenced in the information flow above is the same trust-authorising function established in the Parker Constitution and carried out by the Permission Engine; it is referred to here as the point at which a proposal is authorised, consistent with existing architecture, and introduces no separate or competing authority. Reasoning providers remain interchangeable, model-agnostic services under this document exactly as they are elsewhere in Parker's architecture: Reasoning Context is defined independently of any specific provider, so that swapping providers never requires renegotiating how knowledge is structured or governed. This is a direct consequence of the constitutional principle that Parker owns authority and modules provide capability: a reasoning provider is a module supplying the capability to reason over a Reasoning Context, nothing more, and Reasoning Context is deliberately structured so that no provider ever needs standing authority to do its job.

## Future Considerations

As Parker's Memory and World Model grow richer and reasoning tasks become more complex, the discipline of assembling a bounded, task-scoped Reasoning Context becomes more important, not less. Future work may refine how relevance is determined when selecting what enters a given Reasoning Context, and may develop more sophisticated memory policies for deciding what is promoted from a completed task. Neither development should be permitted to erode the separation between the three layers, allow reasoning providers standing access beyond their assembled context, or make promotion into Memory implicit rather than deliberate.

## Summary

Parker's knowledge architecture rests on three distinct layers: Memory as what Parker has learned, the World Model as what Parker currently believes, and Reasoning Context as what matters for the task in front of it right now. Information flows one way — from sensors into the World Model, from Memory and the World Model into a fresh Reasoning Context, from Reasoning Context into a reasoning provider, and from an authorized proposal into execution. Reasoning Context is assembled per task and discarded when the task ends; nothing is promoted into Memory automatically, only through Parker's deliberate memory policy. Keeping these layers separate is what keeps Parker's reasoning uncluttered, its long-term knowledge trustworthy, and its reasoning providers fully interchangeable.
