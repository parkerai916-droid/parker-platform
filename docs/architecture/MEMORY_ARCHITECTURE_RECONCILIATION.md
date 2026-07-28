# Memory Architecture Reconciliation

## Status

Programme: **Programme 2 — Memory Architecture Reconciliation.**
Phase: **Governance only.** No Kotlin is implemented, proposed as a diff,
or changed by this document. Neither `src/` nor `tests/` is touched.
Nothing is staged, committed, or pushed.

This document does not repeat repository discovery. It is built entirely
from `docs/architecture/MEMORY_CORE_GOVERNANCE_REVIEW.md`'s own accepted
findings, treated here as given fact, plus the additional, already-approved
World Model architecture documents cited where they bear directly on a
question this reconciliation must answer (`WORLD_MODEL_RUNTIME_ARCHITECTURE.md`
§6's source list in particular — cited, not re-derived, and load-bearing
for Section 10). Its purpose is synthesis and decision, not further
inspection: **NOT READY FOR CONTRACT DESIGN** is accepted as this
Programme's current state; this document exists to resolve the
architectural relationships that finding left open, so that Contract
Design, when it begins, starts from one coherent architecture rather than
several unreconciled proposals.

---

## 1. Executive Summary

The Governance Review found a name collision, not a capability overlap:
Parker already has a subsystem called "Memory" (`MemoryStore`), and this
Programme is building a different, broader thing that happens to share
the word "Memory Core." This document resolves that collision by
establishing that **they are not competitors for the same name and were
never meant to be** — they are two different layers of one architecture,
and the fix is structural (a layering decision) plus terminological (a
rename), not a redesign of anything already built.

**The recommended architecture, in one line:** Memory Core is a
foundational identity/provenance/retrieval substrate; today's
`MemoryStore` is renamed **Knowledge Memory** and becomes the evaluated,
promoted layer that sits on top of Memory Core; Conversation History
remains its own, already-correct, already-production-composed subsystem
that Memory Core references but never owns; the World Model remains
fully independent of both, exactly as its own already-approved
architecture already requires; and Reasoning Context continues to read
three narrow, sibling projections — Conversation, Knowledge, World
Model — never Memory Core directly.

This reconciliation also corrects one assumption implicit in this task's
own example diagram: a strict linear chain ending
`Knowledge Memory → World Model` is not supported by repository evidence.
`WorldObservation`'s own already-approved source list
(`WORLD_MODEL_RUNTIME_ARCHITECTURE.md` §6) explicitly excludes both the
Planner and Memory as World Model inputs. World Model is not downstream
of Memory in any sense that exists today, and this document does not
recommend making it so. This is stated plainly, early, because it changes
the shape of every diagram later in this document from a single pipeline
into two parallel families feeding one shared assembly layer.

**Final classification: READY FOR MEMORY CORE CONTRACT DESIGN.** Both
blocking questions the Governance Review left open — the Memory Core /
Memory relationship, and whether the sensitive-record permission gap
must be closed before or during a first unit — are resolved by this
document's own decisions (Sections 8 and 12), not deferred further.

---

## 2. Existing Architecture

Restated from the Governance Review's own accepted findings, without
re-inspecting the repository:

- **`MemoryStore`** (`src/interfaces/MemoryStore.kt`,
  `src/runtime/InMemoryMemoryStore.kt`): a flat, generic long-term
  knowledge/preference store. `CandidateMemory` → `MemoryPromotionPolicy`
  evaluation → `MemoryRecord`. One payload string, one closed
  five-value category enum, a handful of scattered provenance-shaped
  fields, `remember`/`retrieve`/`forget`. Real, tested, but its write
  path (`remember`/`forget`) has zero production callers — the store is
  structurally guaranteed empty in the running system today.
- **`MemorySource`** (`src/interfaces/MemorySource.kt`): the narrower,
  read-only projection of the same instance, genuinely production-wired
  into `DefaultReasoningContextAssembler`.
- **Conversation History** (`ConversationEngine` /
  `ConversationHistorySource`): a separate, already mature, already
  production-composed, already tested subsystem — raw `Turn` records
  (message, sender, timestamp), keyed by `ConversationId`, one-sided
  (owner messages only), no summarisation, no persistence beyond process
  lifetime. Not owned by Memory in any sense today.
- **World Model** (`WorldModel` / `WorldModelSource` /
  `InMemoryWorldModel`): a third, separate, production-composed
  subsystem holding transient, sourced, confidence-scored current
  belief, structurally parallel to Memory, not a consumer of it.
- **No Entity, Document, Evidence, or Provenance model** exists
  anywhere in the repository.
- **No permission boundary** exists on any Memory operation today —
  `InMemoryMemoryStore` holds no `PermissionEngine` reference, and
  `DefaultReasoningContextAssembler`'s read of `MemorySource.recall`
  performs no permission check before rendering a memory into every
  Reasoning Context it assembles.

---

## 3. Architectural Problem

Two separate problems, easy to conflate, kept explicitly distinct
throughout this document:

**Problem A — a naming collision.** "Memory Core" and "Memory"
(`MemoryStore`) sound like the same concept at two different maturity
levels. They are not: `MemoryStore` already has a decided, working
promotion/evaluation lifecycle that has nothing to do with entities,
documents, or provenance, and Memory Core's brief explicitly disclaims
being "the complete future Memory system." Left unresolved, any Contract
Design that follows risks either accidentally redesigning `MemoryStore`
under a different name, or building Memory Core with no relationship to
it at all, leaving two permanently disconnected "memory" concepts in one
repository.

**Problem B — a genuine layering gap.** Even once the naming collision
is resolved, there remains a real architectural question this Programme
must answer once, not per-Contract-Design-unit: what is the dependency
order between Conversation, Memory Core, Knowledge (the renamed
`MemoryStore`), and World Model, and what should Reasoning Context read?
Answering this per-unit, incrementally, risks exactly the kind of
undocumented, retrofitted dependency this repository's own governance
discipline exists to prevent.

This document treats Problem A as resolved by Section 13's rename
recommendation, and Problem B as resolved by Section 6's layering
recommendation — deliberately as two separate resolutions, since a name
change alone would not fix the layering question, and a layering
decision alone would not fix the vocabulary confusion a reader
encounters before ever reaching an architecture diagram.

---

## 4. Candidate Architectures

Four candidates were weighed for the Memory Core / Knowledge relationship
(Question 1's own required assessment):

**A. Memory Core beneath Knowledge Memory** (Memory Core → Knowledge
Memory, matching this task's own example diagram). Knowledge Memory's
evaluation and promotion decisions operate over records — Entities,
Documents, Evidence, Provenance — that Memory Core already knows how to
identify and store. **Recommended** — justified constitutionally in
Section 5.

**B. Memory Core parallel to Knowledge Memory** (siblings, no
dependency). Rejected: Knowledge Memory's own promotion factors
(`33-memory-consolidation.md`: repetition, user importance, goal
relevance, frequency, confidence, explicit request) are exactly the kind
of judgment that should be evaluated against real Entity/Document/
Evidence/Provenance context. Keeping them siblings would force Knowledge
Memory to either duplicate a provenance model of its own (repeating
`MemoryRecord`'s already-flagged flat-field weakness) or evaluate
promotion decisions with no grounding in what the candidate is actually
about.

**C. Knowledge Memory beneath Memory Core** (the reverse dependency).
Rejected: this would mean promotion happens first, and Memory Core would
have to reconstruct entities and provenance retroactively from
already-flattened, already-promoted knowledge — discarding exactly the
structure (source, creator, creation time, relationships) that must be
captured at ingestion time to ever be recoverable (Governance Review
Section 7's non-contemporaneity finding). Provenance capture cannot be
retrofitted after the fact with any integrity.

**D. Merge Knowledge Memory into Memory Core** (no distinction at all).
Rejected: this conflates a decision-making seam (`MemoryPromotionPolicy`
— evaluating whether something is worth durable retention) with an
identity/provenance/retrieval substrate (Memory Core — recording what
something is and where it came from, making no retention judgment at
all). This is the same separation of concerns already established
elsewhere in this repository — `PlanDecision` is a seam distinct from
the Task Manager it operates alongside, `AgentStepSource` is a seam
distinct from the Agent Runtime state it operates on — and Memory Core
must not collapse an analogous distinction merely because both halves
involve "storage."

**Determination: Candidate A.**

---

## 5. Constitutional Analysis

Candidate A is justified directly by the Constitution's own governing
discipline, restated by `MEMORY_RUNTIME_ARCHITECTURE.md` §1 as: **"Memory
stores knowledge. Memory never decides. Memory never acts."** Read
literally, this sentence already implies two separate responsibilities
living at two separate points in one pipeline — *storing* (identifying,
recording, preserving what something is and where it came from) is not
the same act as *deciding* (evaluating whether it is worth durable
retention as knowledge). Memory Core is the storing half; Knowledge
Memory (today's `MemoryStore`, renamed) is the deciding half, exercised
through its own bounded seam, `MemoryPromotionPolicy` (recommended
renamed alongside it — Section 13).

This also satisfies "Cognition proposes. Trust authorises. Runtime
executes," applied one level more precisely than it has been applied to
Memory before: a caller proposing an Entity or Document to Memory Core is
cognition proposing (submission carries no authority); Memory Core's own
identity/provenance recording is a Runtime act, not a decision; Knowledge
Memory's promotion evaluation is a further, separate decision, made only
once Memory Core's own record already exists to be evaluated; and Trust
(Section 12) authorises every write and every sensitive read at the one
point — Memory Core's own boundary — where enforcement is cheapest to
guarantee for every downstream consumer at once.

Candidate C and D each violate this reading in a different way: C makes
Memory Core's own recording act depend on a decision (promotion) that
should come after it, not before; D erases the "decide" step entirely by
fusing it into "store." Candidate A is the only one of the four that
keeps storing and deciding as two distinct, ordered, non-overlapping
acts — exactly what the Constitution's own sentence already requires,
applied consistently rather than newly invented here.

---

## 6. Recommended Layering

This task's own example layering diagram places World Model after
Knowledge Memory in one linear chain. Repository evidence does not
support this: `WorldObservation`'s own approved architecture
(`WORLD_MODEL_RUNTIME_ARCHITECTURE.md` §6, cited directly in
`src/interfaces/WorldModel.kt`'s own KDoc) names exactly seven
legitimate World Model input sources — Sensors, Plugins, Agents, Runtime,
User — and **explicitly excludes both the Planner and Memory** from that
list. World Model is not, and per this already-accepted architecture
must not become, downstream of Memory in any form. `ADR-002` says the
same thing independently: "Context stores immediate conversation state.
World Model stores current reality. Memory stores long-term knowledge" —
three separate stores, not a pipeline.

**Recommended layering, corrected accordingly:**

```
Owner
  |
  v
Conversation                         World Model
  |  (referenced, never owned)       (independently sourced --
  v                                   Sensors, Plugins, Agents,
Memory Core                           Runtime, User only --
  |                                   never Memory, never Planner)
  v
Knowledge Memory
  |
  v
  \                                  /
   \________________________________/
                    |
                    v
            Reasoning Context
```

Conversation, Memory Core, and Knowledge Memory form one dependency
chain (the "Memory family"). World Model is a second, fully independent
family with its own sources. Both families terminate at Reasoning
Context, which reads from three sibling projections — Conversation,
Knowledge Memory, World Model — never from Memory Core directly (Section
9). This is not a redesign of anything already built: it is the exact
shape `ADR-002` and the existing, unmodified `DefaultReasoningContextAssembler`
already implement today, minus the one incorrect assumption (a
World-Model-follows-Memory pipeline) this task's own example diagram
introduced.

---

## 7. Conversation Relationship

**Determination: alongside Memory Core, not inside it, and not above
it.** Conversation records already exist, are already production-composed,
and are already tested (Governance Review Section 3.3/12). Placing
Conversation *inside* Memory Core would mean duplicating ownership of
state `ConversationEngine` already owns — exactly the duplicate-ownership
outcome this task's own instruction warns against. Placing Conversation
*above* Memory Core (implying Memory Core depends on it) is not
supported either: nothing about identifying an Entity, Document, or
Evidence Item requires a Conversation to exist first — a Document could
enter Memory Core from a source with no associated conversation at all
(an imported file, a plugin observation).

The correct relationship is referential: Memory Core's own Provenance
records (Section 11) may cite a `ConversationId`/`TurnId` as a source,
exactly the same loose-coupling-by-identifier pattern `CandidateMemory.correlationId`
already uses today toward tasks and sessions, never a structural
ownership or containment relationship. `ConversationEngine` remains the
sole owner of Conversation state; Memory Core never constructs a `Turn`,
never mutates a `Conversation`, and never duplicates Turn content into
its own records.

---

## 8. Memory Relationship

This section resolves the Governance Review's single most consequential
open question (its Section 20, Question 1) directly.

**Today's `MemoryStore` is not a competing or redundant concept next to
Memory Core — it is what Memory Core's own output layer already looks
like, minus the foundation Memory Core is meant to supply underneath
it.** Concretely: `CandidateMemory.knowledgePayload` is what a
Memory-Core-aware Entity, Document, or Evidence reference would otherwise
have been; `CandidateMemory.sourceSubsystem`/`correlationId`/
`originatingPrincipalId` are a flattened, ad hoc approximation of what a
real Memory Core Provenance record should be; `MemoryPromotionPolicy` is
already, precisely, the "evaluate and decide what becomes durable
knowledge" seam Knowledge Memory needs to keep. Nothing about
`MemoryStore`'s actual behaviour needs to be thrown away — it needs a
foundation placed beneath it, and a name that no longer collides with
that foundation's own name.

**Recommendation:** today's `MemoryStore` **becomes Knowledge Memory**
(Section 13's terminology), not "User Memory" (too narrow — nothing
in its current design scopes it to one user; `originatingPrincipalId`
is nullable and Principal-agnostic records are explicitly supported) and
not "Long-term Memory" (redundant with, and less precise than, "Knowledge
Memory," which names *what* it holds — evaluated knowledge — rather than
merely *how long* it is held). It is not "another bounded context"
unrelated to Memory Core; Section 5 established exactly why it is Memory
Core's direct dependent, not a peer.

**Survives unchanged, renamed, split, becomes an adapter, or deprecated?**
None of the five, applied singly and immediately — this is a staged
answer, not a one-word one, and is the substance of Section 14's
migration strategy: renamed at the documentation/vocabulary level now
(no code change, since this is governance only); its Kotlin identifiers
renamed in a later, explicitly-approved implementation unit; and only
then, once Memory Core itself exists, adapted so that Knowledge Memory's
submission path constructs or references real Memory Core Provenance/
Entity records instead of carrying its own flat, duplicate fields. It is
not deprecated — its promotion/evaluation/retrieval behaviour remains
genuinely necessary and is not being replaced by anything Memory Core
itself does. It is not split — nothing in this review identifies two
independently-varying responsibilities inside it that would justify
separating it into two types.

---

## 9. Knowledge Relationship

Knowledge Memory (renamed `MemoryStore`) sits directly beneath Reasoning
Context and directly above Memory Core, per Sections 6 and 8. Its own
internal responsibility is unchanged from what `MemoryPromotionPolicy`
already does today: evaluate a submission against named promotion
factors and decide whether it becomes durable, retrievable knowledge.
What changes, over time, and only once Memory Core exists to make it
possible, is *what a submission is allowed to reference* — a real
Entity, Document, or Evidence Item with real Memory-Core-owned
Provenance, rather than an untyped payload string with a handful of flat
fields describing its own origin. This document does not design that
field-level change; it only fixes the direction: Knowledge Memory
depends on Memory Core, never the reverse, so this evolution is additive
to Knowledge Memory's own contract, not a redesign of Memory Core to
accommodate it.

---

## 10. World Model Relationship

**Determination: the World Model consumes none of Memory Core, Knowledge
Memory, or Conversation — directly or indirectly — as an input to what
it believes.** This is not a new policy invented by this reconciliation;
it is the already-approved, already-implemented exclusion
`WORLD_MODEL_RUNTIME_ARCHITECTURE.md` §6 already states, confirmed
directly against `WorldObservation`'s own KDoc in
`src/interfaces/WorldModel.kt`, which names its seven legitimate sources
and explicitly calls out "Planner and Memory deliberately excluded."
`WorldModelUpdatePolicy.evaluate(observation, existing)` — the one seam
that decides what the World Model believes — takes only the incoming
observation and any existing belief for the same subject; it has no
parameter through which Memory Core, Knowledge Memory, or Conversation
content could enter even if a future implementation wanted it to.

This reconciliation recommends **preserving this exclusion exactly as it
stands**, for a reason grounded in this task's own constitutional
material, not merely because it is already the rule: World Model
answers "what is true right now" from live, sourced signals; Memory
answers "what has been learned, deliberately, over time." Letting World
Model beliefs be informed by Knowledge Memory would blur exactly the line
`ADR-002` and `WorldModel.md`'s own normative requirement ("World Model
MUST NOT become Memory") already draw, in the specific direction that
requirement warns against being crossed.

**Recommended information flow:** World Model remains a fully
independent, parallel family (Section 6's diagram). The only place its
output and the Memory family's output ever meet is downstream, at
Reasoning Context — as two sibling read projections into one assembly,
never as one feeding the other.

---

## 11. Provenance Position

**Determination: Provenance is its own record type, mandatorily
referenced by every other Memory Core record — not embedded ad hoc per
type, and not merely "layered beneath" as an unrealised metaphor.**

The Governance Review (Section 20, Question 5) left this open between
three shapes: embedded on every record; a separate record; or a vague
"layered beneath everything" framing with no concrete shape. This
document resolves it in favour of the second, made concrete rather than
left abstract, for a reason the Governance Review's own Section 7 already
surfaced but did not carry to a conclusion: **document handling is
progressive, not atomic.** A Document's acquisition time is known the
moment Parker's boundary encounters it; its claimed creation time may
only become known (or disputed) after later analysis; its extraction
confidence is only known once a future Document Handling programme
actually parses it; whether it is original, extracted, summarised, or
inferred may not be resolvable until multiple processing steps have run.
An embedded-per-record provenance field, fixed at record-creation time,
cannot accept this kind of staged, later-arriving information without
mutating the record itself — which Section 8's own inherited
correction-not-destruction principle (Governance Review Section 10)
already rules out for original records. A **separate, independently
addressable, independently amendable Provenance record** — referenced,
never embedded, and itself corrected only through the same linked-
amendment model as everything else — is the only one of the three
candidate shapes that can accept later-arriving provenance information
without ever mutating the record it describes.

Every Memory Core record (Entity, Document, Evidence Item) and, by
extension through Section 9's dependency direction, every Knowledge
Memory record, carries a mandatory, non-nullable reference to a
Provenance record. No Memory Core record may exist without one — this is
the one universal invariant this document fixes now, deliberately,
rather than leaving to a later Contract Design's discretion, since
allowing an optional or absent provenance reference would reopen exactly
the "storage does not create truth, but untracked storage cannot even be
checked for truth" gap this whole Programme exists to close.

---

## 12. Trust Boundary

**Determination: one Trust boundary, enforced at Memory Core's own
read/write surface — not one boundary per consuming subsystem.**

Because Section 9 establishes that Knowledge Memory (and any future
Evidence-aware consumer) is structurally downstream of Memory Core,
enforcing `PermissionEngine.evaluate` once, at the point where a Memory
Core record is created, amended, disputed, deleted, or read, covers
every downstream consumer automatically — mirroring the existing
precedent that `ExecutionPipeline` is the one place a Tool's permission
check happens, never re-implemented per Tool. Scattering separate
permission logic across Memory Core and Knowledge Memory independently
would risk exactly the kind of silent, per-subsystem gap the Governance
Review already found once (Section 5 of that document: no permission
check exists anywhere on the current Memory path).

Answered specifically, per this task's own six sub-questions, all
resolving to the same single boundary:

- **Who may create Memory Core records?** Any caller whose proposed
  action resolves, via the existing `ActionVocabularyEntry`/`ActionMapper`
  mechanism, to `PermissionAction.WRITE` on `ResourceType.MEMORY` (or
  `DOCUMENT`), and is `APPROVED` by `PermissionEngine.evaluate` — never
  unconditionally, and never merely because the caller is a registered
  system Principal.
- **Who may amend them?** The same `WRITE` check, evaluated against the
  new, linked amendment record being created (Section 10 of the
  Governance Review: amendment is itself a new record, never an in-place
  mutation) — not a separate "amend" action, since amendment is
  structurally a creation.
- **Who may mark disputes?** The same `WRITE` check, targeted at the
  Assertion record being created (Section 9 below).
- **Who may create Knowledge** (trigger promotion into Knowledge
  Memory)? This remains, unchanged, **no one directly** —
  `MEMORY_CONTRACT_DESIGN.md` §9's already-settled architectural decision
  that promotion is never a caller-facing operation is preserved exactly
  as-is by this reconciliation. A caller's only Trust-gated act is
  submission (the same `WRITE` check, applied at Knowledge Memory's own
  boundary, itself downstream of Memory Core's); promotion itself remains
  internal, decided by `MemoryPromotionPolicy`, never by a permission
  grant.
- **Who may delete?** `PermissionAction.DELETE` on the same resource,
  reserved specifically for the narrower, owner-requested erasure path
  the Governance Review (Section 10) already distinguished from ordinary,
  non-destructive correction.
- **Who may summarise?** A summary is itself a new, derived record —
  subject to the same `WRITE` check as any other creation, and must carry
  a Provenance record (Section 11) explicitly flagged
  "summarised," never "original," so a later reader is never misled about
  what they are looking at.

Read access to any record whose Provenance or content is marked
sensitive requires the equivalent `READ` check before the content leaves
Memory Core's boundary — closing, by design rather than by inheritance,
the exact gap the Governance Review found already open on today's
`MemorySource.recall` path. This is the resolution to the Governance
Review's own Section 20 Question 2: the permission boundary is not
deferred until a sensitive record type happens to be proposed; it is
built into Memory Core's own read/write surface from the first record
type onward, so no future record type can ever be added without it.

---

## 13. Canonical Terminology

One vocabulary, recommended final:

| Concept | Canonical name | Notes |
| --- | --- | --- |
| The foundational identity/provenance/retrieval substrate (Entities, Documents, Evidence Items, Provenance, Assertions) | **Memory Core** | Unchanged from this Programme's own naming; now unambiguous once the item below is renamed. |
| Today's `MemoryStore`/`InMemoryMemoryStore`/`CandidateMemory`/`MemoryRecord`/`MemoryPromotionPolicy`/`MemoryCategory`/`MemorySource` | **Knowledge Memory** (types: `KnowledgeStore`, `CandidateKnowledge`, `KnowledgeRecord`, `KnowledgePromotionPolicy`, `KnowledgeCategory`, `KnowledgeSource`) | Renamed, not redesigned — same behaviour, same tests, new name reflecting its actual, narrower role once Memory Core exists beneath it (Section 8). Kotlin renaming is a future implementation unit's job, not performed here. |
| Raw, per-Turn interaction record | **Conversation History** | Unchanged — already correctly named, already correctly scoped. |
| Transient, sourced, confidence-scored current belief | **World Model** | Unchanged — already correctly named, already correctly independent. |
| The umbrella term for the whole family (Memory Core + Knowledge Memory) | **Memory** (used only as an umbrella/architectural-role term from this point forward, never again as a synonym for one specific store) | Resolves the naming collision at its root: "Memory" stops naming one flat store and starts naming the two-layer architecture this document describes. |

No new `PermissionAction` or `ResourceType` value is introduced by this
terminology — `ResourceType.MEMORY`/`DOCUMENT` already exist and already
fit both Memory Core and Knowledge Memory records without modification.

---

## 14. Migration Strategy

Governance only — no implementation is performed or scheduled by this
document.

- **Do existing `MemoryStore` contracts survive?** Yes, unchanged in
  field-level shape, for now. This reconciliation requires no Kotlin
  change. A future, separately-approved Contract Design/Scope Lock unit
  is required before any rename or field change is implemented.
- **Are adapters preferable?** Yes, but only once Memory Core itself
  exists. Until then, no adapter is needed or should be built — there is
  nothing yet for Knowledge Memory to adapt to. Once Memory Core's first
  unit is implemented, Knowledge Memory's submission path should be
  revised to construct/reference real Memory Core Provenance/Entity
  records rather than carrying its own flat fields — an additive,
  backward-compatible evolution of `CandidateMemory`, not a breaking
  redesign, consistent with `MEMORY_CONTRACT_DESIGN.md`'s own established
  "additive, defaulted, never breaking" extensibility pattern.
- **Is a repository rename required?** Recommended, not immediately
  required. It should be scheduled as part of a future Knowledge Memory
  Contract Design revision (the same unit that performs the adapter
  work above), not as a standalone renaming-only change — renaming
  `MemoryStore` to `KnowledgeStore` without also connecting it to Memory
  Core would be pure churn with no architectural benefit, and would cost
  a second full test-suite update for no behavioural gain.
- **Do existing tests remain valid?** Yes, entirely and immediately.
  This document changes no code, so `InMemoryMemoryStoreTest.kt` and its
  siblings continue to validate exactly what they validate today.  They
  remain valid as Knowledge Memory's own test suite once the rename
  above is eventually carried out — the tests describe promotion,
  evaluation, and retrieval behaviour that this reconciliation does not
  alter in any way, only reposition architecturally.

---

## 15. Long-Term Architecture

```
                              Owner
                                |
                          Constitution
                                |
                             Trust  <---------------------------+
                                |                                |
                          (gates every write and                 |
                           every sensitive read below)           |
                                |                                |
        +-----------------------+-----------------------+        |
        |                       |                       |        |
   Conversation             Memory Core             World Model  |
   (raw Turns,          (Entities, Documents,       (independently
    ConversationEngine,  Evidence Items,             sourced: Sensors,
    unchanged)            Provenance, Assertions) ---+ Plugins, Agents,
        |                       |                       Runtime, User)
        |                       v                       |
        |                 Knowledge Memory               |
        |               (renamed MemoryStore:            |
        |                CandidateKnowledge ->            |
        |                KnowledgePromotionPolicy ->       |
        |                KnowledgeRecord)                  |
        |                       |                          |
        +-----------+-----------+--------------------------+
                     |
                     v
              Reasoning Context
           (Conversation + Knowledge + World Model,
            three sibling read-only projections --
            never Memory Core directly)
                     |
                     v
                 Reasoning
                     |
                     v
                 Planning
              (also consults Knowledge Memory
               directly, as the Planner already
               does today, unchanged)
                     |
                     v
                 Execution
           (unchanged Execution Pipeline;
            has no direct relationship to
            Memory Core or Knowledge Memory,
            exactly as today)
```

**Information flow, summarised:** every write into Memory Core, and
every amendment, dispute, deletion, or summarisation of what it holds,
passes through Trust first. Memory Core feeds Knowledge Memory's
evaluation/promotion seam, one-directionally. Conversation and World
Model each remain independent, feeding Reasoning Context directly, never
through Memory Core. Reasoning Context assembles exactly three sibling
projections. Planning consults Knowledge Memory directly (unchanged from
today's Planner Runtime behaviour) in addition to whatever Reasoning
Context already assembled. Execution remains exactly as far from Memory
as it is today — no relationship, direct or indirect.

---

## 16. Recommendation

- **Recommended architecture:** Candidate A (Section 4) — Memory Core
  beneath Knowledge Memory, both within one "Memory" family, Conversation
  and World Model each independent and parallel to that family, Reasoning
  Context assembling three sibling projections and never reading Memory
  Core directly.
- **Recommended terminology:** Section 13's table — "Memory Core"
  unchanged; today's `MemoryStore` renamed to "Knowledge Memory"
  (`KnowledgeStore` et al., Kotlin renaming deferred to a future
  implementation unit); "Conversation History" and "World Model"
  unchanged; "Memory" reserved henceforth as the umbrella term for the
  two-layer family, never again as a synonym for one flat store.
- **Recommended ownership:** Memory Core owns Entity, Document, Evidence
  Item, Provenance, and Assertion identity and structure, and is the one
  place Trust is enforced for the whole family (Section 12). Knowledge
  Memory owns evaluation and promotion only, via its own unchanged
  `MemoryPromotionPolicy`-shaped seam, and remains the only path a
  submission can ever be promoted through (no caller-facing `promote`
  operation, preserved unchanged from `MEMORY_CONTRACT_DESIGN.md` §9).
  Conversation ownership remains entirely with `ConversationEngine`.
  World Model ownership remains entirely independent, per its own
  existing, unmodified architecture.
- **Recommended layering:** Section 6's corrected diagram — two parallel
  families (Conversation → Memory Core → Knowledge Memory; and World
  Model, alone) both terminating at Reasoning Context, not the single
  linear chain this task's own example diagram proposed.
- **Recommended sequencing:** (1) this reconciliation, accepted as the
  governing architecture; (2) Memory Core Contract Design, scoped to
  Entity, Document (registration/provenance only), Provenance, Assertion,
  and a typed relationship record, exactly as the Governance Review's own
  Section 18 already proposed, now additionally required to design the
  single Trust boundary Section 12 above mandates from its first unit
  onward; (3) Memory Core's first implementation unit; (4) a Knowledge
  Memory Contract Design revision that renames `MemoryStore`'s Kotlin
  identifiers and adapts its submission path to reference real Memory
  Core records, per Section 14; (5) Evidence Item and Assertion-to-Evidence
  linkage, once (2)-(4) are real, per the Governance Review's own
  Section 18 sequencing, unchanged.

---

## 17. Final Classification

Both blocking questions the Governance Review left open are resolved by
this document: the Memory Core / Memory relationship (Sections 4, 5, 8),
and the sensitive-record permission boundary, now mandated as a single,
Memory-Core-level Trust enforcement point from the first record type
onward, not deferred (Section 12). The remaining open items — exact
field-level shapes for Entity, Document, Provenance, Evidence Item, and
Assertion — are properly Contract Design's own work, not Governance's;
nothing found in this reconciliation requires a further governance pass
before that work begins.

```
READY FOR MEMORY CORE CONTRACT DESIGN
```
