**Status:** Planning Review only. Governance and scoping document. No Kotlin
is implemented, proposed as a diff, or changed by this document. Neither
`src/` nor `tests/` is touched. This document does not amend any frozen
governance document (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`,
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`,
`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`,
`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`,
`docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md`, or
any other frozen or adopted governance document) — it reads them, cites
them, and reasons from them. It does not select a final content-
representation design, does not select a final retrieval-surface
mechanism, does not decide the Authorization Purpose question, does not
rename Evidence Intelligence or modify any source comment, does not
create or reserve a new numbered gap, and does not begin a Boundary
Review or Contract Design. Gap #54 is complete and is not reopened.

# Knowledge Discoverability and Governed Retrieval into Reasoning Context — Planning Review

---

**Amendment Note (Timing Scope Correction).** Independent Contract Design review found that
Section 6's original, absolute "prevents... timing from becoming observable" wording conflicted
with this same section's own "passes the act-level authorization gate before any persistence
inspection occurs" requirement: act denial and an authorized retrieval necessarily perform
different amounts of work, and therefore can honestly differ in elapsed wall-clock time, in any
in-process architecture this repository has ever built. This amendment narrows only the timing
claim (Section 6, below); it does not weaken the content, match-existence, result-count,
authorization, Purpose, or Memory Core non-disclosure guarantees Section 6 and Section 8 already
fix, all of which remain unchanged. Gap #54 remains complete and is not reopened by this
amendment. No implementation is introduced by this amendment.

---

## 1. Governing Context

Documents read fresh, in full or in every relevant section, for this
Planning Review: `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`
(Section 9, Section 12); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md`
(Section 10); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`
(Section 3, Section 4); `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`;
`docs/architecture/IMPLEMENTATION_GAPS.md` (Gap #54, current, corrected
entry); `docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_OPERATIONALISATION_PROGRAMME_CLOSURE_DETERMINATION.md`;
`docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md`
(Section 22); `docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_OPERATIONALISATION_UNIT_5_COMPLETION_REVIEW.md`.
Production code read fresh, in full or in every relevant section:
`src/composition/ParkerRuntime.kt`; `src/runtime/DefaultKnowledgeRetrieval.kt`;
`src/runtime/DefaultKnowledgeCandidateEvaluator.kt`;
`src/runtime/DefaultKnowledgeSubmission.kt`;
`src/runtime/KnowledgeItemPersistence.kt`;
`src/runtime/DefaultReasoningContextAssembler.kt`;
`src/runtime/InMemoryKnowledgeStore.kt`; `src/interfaces/MemoryCore.kt`;
`src/composition/PermissionFilteredMemoryRetrieval.kt`; and the relevant
test files cited by section below.

A prior read-only research pass produced a full evidence trace and a
concise decision memo; both were saved outside the repository, at
`/tmp/parker-knowledge-discoverability-decision-package.txt` and
`/tmp/parker-knowledge-discoverability-decision-memo.txt` respectively.
Neither is repository state and neither is cited as governing evidence
below — every citation in this document traces to a repository file.
Both were used only as research input to accelerate this Planning
Review's own fresh reasoning, not as authority for it.

---

## 2. Purpose and Scope of This Planning Review

This Planning Review exists to determine what a future Contract Design
must resolve before any Reasoning Context wiring, content-representation
change, or Authorization Purpose decision may be implemented — not to
make those decisions itself. It follows the same Planning Review →
Boundary Review → Contract Design → Scope Lock → Implementation Plan →
independently reviewed implementation units → live verification →
Closure Determination discipline the accepted Gap #54 Memory Retrieval
Operationalisation programme already established and completed
(`docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_OPERATIONALISATION_PROGRAMME_CLOSURE_DETERMINATION.md`).

This is not a reopening of Gap #54. Gap #54 is complete (Section 4,
below).

---

## 3. Identity and Programme Ownership

Accepted governance formally assigns the Reasoning Context cutover —
wiring `DefaultReasoningContextAssembler` (or its successor) onto
Knowledge Memory's own retrieval surface in place of the legacy
`MemoryStore`/`MemorySource` path — to **Programme 4 (Reasoning
Context)**. This is not asserted once but independently reaffirmed by
three separate, already-accepted governance documents:

- `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`
  Section 9 ("Reasoning Pipeline Remediation") names "Stage 3 —
  Reasoning Context wired onto Knowledge Memory (Programme 4).
  `DefaultReasoningContextAssembler` is adapted to consume Knowledge
  Memory's retrieval surface instead of the legacy `MemorySource`
  path." Section 12 ("Programme Roadmap") allocates "Programme 4 —
  Reasoning Context... Reasoning Pipeline Stage 3" directly.
- `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md`
  Section 10 ("Programme Boundaries") states: "Programme 4 (Reasoning
  Context) owns: wiring `DefaultReasoningContextAssembler` onto
  Knowledge Memory's new retrieval surface in place of the legacy
  `MemoryStore`/`MemorySource` path (Remediation Plan Section 9, Stage
  3); Article V propositional integrity; and Article VII's
  burden-of-justification computation..."
- `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` Section 4
  ("Explicit Exclusions") lists, as excluded from Programme 3 and
  explicitly reassigned: "Reasoning Context's own consumption of
  Knowledge Memory — wiring `DefaultReasoningContextAssembler` (or its
  successor) onto the new retrieval surface, and any resulting change to
  what Reasoning Context actually reads at runtime | Explicitly
  allocated to Programme 4 by Governance Review Section 10 and the
  Remediation Plan Section 9 Stage 3."

**The Evidence Intelligence source-comment collision.** The
already-implemented, already-merged Evidence Intelligence subsystem
self-labels as "Programme 4, Evidence Intelligence" throughout its own
production KDoc — `src/composition/ParkerRuntime.kt` lines 226, 244,
663, 704, 836, and 1333; `src/composition/EvidenceIntelligenceInvocationOutcome.kt`
line 6. Evidence Intelligence's own governing Scope Lock,
`docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` line 5, claims
no programme number at all ("Programme: **Evidence Intelligence —
Scope Lock.**"). No governance document found anywhere in this
repository formally reassigns "Programme 4" from Reasoning Context to
Evidence Intelligence. This Planning Review records this as **informal
source-comment naming drift, not a governance reassignment** — the
three-document formal allocation above is unweakened and unrevised by
it. This Planning Review does not rename Evidence Intelligence, does
not modify any of its source comments, and does not adjudicate the
collision. This document uses the descriptive title above, not a
"Programme 4" label, precisely so that this Planning Review's own
existence does not require the collision to be resolved first. Whether
a future document formally reclaims "Programme 4" for Reasoning Context,
assigns a fresh number, or continues without one is left to Steve and
Codex, not decided here.

---

## 4. Gap Boundary

Gap #54 (Memory Retrieval Operationalisation) remains complete and is
not reopened by this document. `docs/architecture/IMPLEMENTATION_GAPS.md`'s
own current, corrected Gap #54 entry already records its resolution, the
governed promotion path it delivered, and the exact items — knowledge
discoverability, Reasoning Context injection, conversational recall, and
restart durability — that remained explicitly outside its own
resolution. This Planning Review addresses knowledge discoverability,
Reasoning Context injection, and same-runtime conversational recall; it
does not revisit, weaken, or re-litigate anything Gap #54 itself
resolved. Restart durability is recorded as an explicit exclusion and
non-claim of this Planning Review (Section 9, Section 12), not as an
item this Planning Review addresses.

This Planning Review does not create or reserve Gap #55, or any other
new gap number. It also does not decide whether later governance should
add one — that determination, including its timing relative to Contract
Design, is left open and unresolved by this document.

---

## 5. Current Production Boundary

Verified directly against current production source, documented here
as the fixed factual baseline for the future Contract Design:

**Real owner Remember-to-promotion path.** An explicit owner Remember
instruction reaches `MemoryAdmissionCoordinator`, which creates a
durable Memory Core `Assertion` and `Provenance`. `DefaultKnowledgeSubmission.submit`
(`src/runtime/DefaultKnowledgeSubmission.kt` lines 102-126) gates on its
own Evaluation B, then invokes `DefaultKnowledgeCandidateEvaluator.evaluate`
(`src/runtime/DefaultKnowledgeCandidateEvaluator.kt` lines 218-328),
which resolves the candidate's evidence through the candidate-purpose-
bound `MemoryRetrieval` view Gap #54 delivered, and on `Promote`, stores
a `KnowledgeItem`. This path is genuine, composed, and independently
verified — unchanged and untouched by this Planning Review.

**Promoted `KnowledgeItem` persistence.** `InMemoryKnowledgeItemPersistence`
(`src/runtime/KnowledgeItemPersistence.kt` lines 81-98) is a plain,
mutex-guarded, in-memory `MutableMap` with no file, database, or
serialization of any kind.

**`DefaultKnowledgeRetrieval`'s basis-text matching.** `matches()`
(`src/runtime/DefaultKnowledgeRetrieval.kt` lines 483-486) performs a
case-insensitive substring match of `KnowledgeRetrievalQuery.relevance`
against only the most recent history event's `basis` string — a
generic promotion-rationale explanation, never the original remembered
proposition. This class's own KDoc (lines 131-153) discloses this is a
deliberate design choice, not an oversight: "Unlike legacy
`KnowledgeRecord`, `KnowledgeItem` carries no free-text 'payload' field
of its own to match against (by design: Knowledge Memory never copies
or duplicates Memory Core content)."

**Absence of a conversational consumer.** `ParkerRuntime.kt` line 248
holds `knowledgeRetrieval` as a `private lateinit var`, with its own
adjacent comment (lines 238-247) disclosing "no production entry point
consumes it yet." Independently confirmed by an explicit negative
composition test: `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`
line 407, `` `no Knowledge Retrieval dependency is reachable from the
conversation coordinator chain` ``.

**Reasoning Context's separate legacy `KnowledgeSource`.**
`DefaultReasoningContextAssembler.assemble` (`src/runtime/DefaultReasoningContextAssembler.kt`
lines 296-312) reads its "Memories" entry from `memorySource: KnowledgeSource`
— a structurally different type hierarchy (`KnowledgeStore`/
`KnowledgeSource`/`KnowledgeQuery`/`KnowledgeRecord`) from
`KnowledgeItemPersistence`/`KnowledgeRetrieval`/`KnowledgeRetrievalQuery`/
`KnowledgeItem`. `src/runtime/InMemoryKnowledgeStore.kt` line 16's own
KDoc attributes its origin to "Sprint 4, Track A, Unit A3," predating
Programme 3 entirely; its `KnowledgeSource` narrowing was added later,
per lines 50-62's own KDoc, "Sprint 11 Unit 7."

**Empty `InMemoryKnowledgeStore` production wiring.** `ParkerRuntime.kt`
lines 391-392 construct a fresh `InMemoryKnowledgeStore()` and bind it
to `memorySource`; nothing in `ParkerRuntime.kt` ever calls `.remember()`
on it. The "Memories" Reasoning Context entry renders zero entries in
the live system today, independent of anything this programme resolves.

**Runtime-lifetime-only `KnowledgeItem` persistence.** Restated from
above: no durability mechanism of any kind exists for promoted
`KnowledgeItem` values. This is already disclosed and accepted as a
bounded limitation by `docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_OPERATIONALISATION_UNIT_5_COMPLETION_REVIEW.md`
Section 5 — not a new finding, and not addressed by this Planning
Review (Section 9, Section 12).

---

## 6. Content Discoverability

The leading candidate for a future Contract Design to evaluate —
**not adopted here** — is query-time dereferencing of
`KnowledgeItem.evidenceReference` through governed Memory retrieval: a
purpose-bound `MemoryRetrieval` call resolves a candidate item's own
referenced Memory Core record fresh, per query, for relevance
comparison and/or rendering — mirroring the pattern
`DefaultKnowledgeCandidateEvaluator.resolve()` (`src/runtime/DefaultKnowledgeCandidateEvaluator.kt`
lines 355-378) already uses to resolve evidence without ever storing a
second copy of it. This document does not claim `DefaultKnowledgeRetrieval`
already narrows candidates by content before any such dereference could
occur — its own current content-related narrowing (`matches()`,
`src/runtime/DefaultKnowledgeRetrieval.kt` lines 483-486) is exactly the
defective basis-text match Section 5 documents, not a lawful
pre-filtering step this direction could build on. Contract Design must
itself determine a lawful sequence — not assumed or pre-selected here —
that:

- passes the act-level authorization gate before any persistence
  inspection occurs;
- enumerates only structurally eligible `KnowledgeItem`s (status and
  lifecycle shaping, unchanged from `DefaultKnowledgeRetrieval`'s own
  existing Unit 9.4 behaviour);
- resolves referenced Memory Core evidence through a governed path
  before content relevance can be evaluated at all;
- prevents denied content, match existence, or result counts from
  becoming observable to an unauthorised caller, and prevents any
  explicit timing metadata or deliberately encoded timing signal from
  doing so — the wall-clock-latency component of this requirement is
  narrowed by amendment, immediately below;
- applies item-level Knowledge Retrieval authorization and result
  bounds (`KnowledgeRetrievalQuery.maximumResults`) in an explicitly
  justified order, not an incidental one.

**Timing scope, narrowed by amendment.** Elapsed wall-clock latency can
honestly vary between act denial, authorized-empty retrieval, candidate
filtering, and evidence resolution, because each performs a genuinely
different amount of work. Current in-process architecture provides no
constant-time execution or padding mechanism, and this Planning Review
does not authorise adding one. The absence of explicit timing metadata
in a result does not, by itself, eliminate an observer's ability to
measure how long a call took — that variance is an accepted and
explicitly disclosed limitation of this first same-runtime retrieval
programme, never a concealed one. This programme must not claim
resistance to active timing analysis. Adding constant-time padding,
batching, or other timing-channel mitigation would require separate
governance and is not authorised here.

This Planning Review does not adopt a final ordering for any of the
above.

Three alternatives are recorded, with their risks, for Contract Design
to weigh — none selected here:

1. **Dereference at query time** (the leading candidate, above). Risk:
   a second Memory Core round-trip per candidate item per query; a new
   `MemoryRetrieval`-shaped dependency for whichever component performs
   the dereference; must itself be permission-gated and non-widening
   (see the required proofs below).
2. **Add content representation to `KnowledgeItem`.** Risk: reopens the
   frozen Programme 3 Contract Design Version 2 Section 12 decision that
   `KnowledgeItem` "never copies or duplicates Memory Core content"
   (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` Section
   3's own deliverable table entry for the full Knowledge Memory
   contract set) — a genuine Contract-Design-tier reopening of
   already-frozen governance, not a mere extension.
3. **Create a separate indexed projection** of promoted content outside
   both `KnowledgeItem` and Memory Core. Risk: a second, unaudited
   source of truth for content already durably recorded in Memory Core,
   directly contrary to this repository's own repeated
   "never duplicate, never fork the source of truth" discipline seen
   throughout the Memory Core and Knowledge Memory governance chain.

Whichever direction Contract Design ultimately selects, it must prove
each of the following before Scope Lock may authorise it:

- **No duplicate source of truth** for remembered content.
- **No raw Memory Core bypass** by Reasoning Context or the model —
  every content read remains through a governed, permission-evaluated
  path, exactly as Gap #54 already established for candidate evidence
  resolution.
- **Permission filtering occurs before content or match results become
  observable** to the caller, mirroring `PermissionFilteredMemoryRetrieval`'s
  own established "evaluate before disclose" discipline
  (`src/composition/PermissionFilteredMemoryRetrieval.kt`).
- **No Evidence Intelligence authority widening** — `EvidenceIntelligenceInputResolver`'s
  own retrieval path must remain exactly as fail-closed as Gap #54 left
  it, regardless of what this programme adds.
- **Deterministic handling of missing, denied, deleted, or unavailable
  evidence** — a `KnowledgeItem` whose referenced Memory Core record can
  no longer be resolved (denied, deleted, or otherwise unavailable) must
  behave predictably and honestly, never silently fabricating content or
  silently omitting the item without disclosure.

---

## 7. Retrieval-Surface Direction

The already-governed direction, confirmed by `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md`
Section 10 ("Programme 3 does not implement Reasoning Context's own
consumption of that surface — it delivers a surface Programme 4 can
consume"), is that **Reasoning Context must cut over from the legacy
`KnowledgeSource`/`KnowledgeStore` path to `KnowledgeRetrieval`**.
Programme 3 was never governed to build two permanent, parallel
"knowledge" surfaces for Reasoning Context to choose between — it built
`KnowledgeRetrieval` specifically for Programme 4 to adopt.

**This Planning Review does not authorise preserving two active
knowledge sources in production.** The legacy path is already
unconsumed in any behaviourally meaningful sense (Section 5, above —
`memorySource` is bound to a store nothing ever writes to), so the
legacy store appears empty in current production composition and no
live "Memories" entries are expected to disappear as a result of
retiring it. This is not, by itself, proof that the cutover is
behaviourally safe: Contract Design and its own regression tests must
still prove the cutover preserves every existing non-memory Reasoning
Context entry (identity, participant, conversation, prior-message,
world-belief, and tool entries), their existing ordering, correct
model-prompt delivery, and correct empty-memory behaviour, until genuine
Knowledge results exist to render instead.

Adapter shape and translation mechanics — whether `DefaultReasoningContextAssembler`'s
own constructor signature changes directly, whether a translation layer
is introduced, and how `KnowledgeRetrievalResult` is rendered into
Reasoning Context entries — are left entirely to Contract Design. This
Planning Review does not specify or presume a mechanism.

`WorldModelSource` and `ConversationHistorySource` inputs to
`DefaultReasoningContextAssembler` (`src/runtime/DefaultReasoningContextAssembler.kt`
lines 233-239) remain unchanged by anything this programme considers —
neither is part of the boundary this Planning Review or its successor
Contract Design addresses.

---

## 8. Authorization Decision Gate

**Authorization Purpose is not optional or unnecessary by default.**
This Planning Review does not claim the current permission state is
sufficient merely because it happens not to deny.

Verified directly: `DefaultKnowledgeRetrieval`'s own `knowledge.retrieve`
verb is governed, at both its act-level and item-level gates
(`src/runtime/DefaultKnowledgeRetrieval.kt` lines 49-102's own KDoc), by
a single, **coarse** `PermissionPolicyRule` in `src/composition/ParkerRuntime.kt`
lines 611-616 — `(PermissionAction.READ, ResourceType.MEMORY, APPROVED,
AUTOMATIC)` with no `proposedAction` and no `authorizationPurpose`
discriminator. This rule was registered, per its own adjacent comment
(lines 601-610), as "the minimum required for `knowledge.retrieve` to be
reachable at all" — at a time when no conversational, or any other
production, consumer of `DefaultKnowledgeRetrieval` existed. **This
coarse rule predates a real conversational consumer and cannot
automatically be treated as sufficient for one merely because it is
already in place and already unconditionally permissive.** Its own
governing rationale was reachability for testing and future composition,
not a considered decision about what a live, owner-facing conversational
caller should be authorised to do.

Contract Design must decide, and this Planning Review does not
pre-decide:

- whether conversational retrieval requires a **registered, purpose-
  bound caller** (mirroring `PermissionFilteredMemoryRetrieval.forAuthorizationPurpose`'s
  own already-accepted Gap #54 pattern);
- whether **purpose-specific rules** should replace or supplement the
  existing coarse `(READ, MEMORY)` approval for `knowledge.retrieve`;
- whether **`DefaultKnowledgeRetrieval`'s own request construction**
  (`buildExecutionRequest`, `src/runtime/DefaultKnowledgeRetrieval.kt`
  lines 518-534) needs to change to carry a Purpose at all, since it
  currently constructs no `authorizationPurpose` on any request it
  builds.

**No production wiring may begin until fail-closed, non-widening
authorization is settled by Contract Design and Scope Lock.** This
mirrors Gap #54's own Contract Design Section 22 discipline exactly
(`docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md`
Section 22): a policy-content decision may not be made, or implied,
before the governed dimension it depends on is itself proven sufficient.

---

## 9. Scope

**Likely future production scope** (subject to Contract Design
authorisation, not authorised by this document):

- `src/runtime/DefaultKnowledgeRetrieval.kt`
- `src/runtime/DefaultReasoningContextAssembler.kt`
- `src/composition/ParkerRuntime.kt`
- conditional interface/policy files (e.g. `src/interfaces/KnowledgeStore.kt`,
  `src/runtime/DefaultPermissionPolicy.kt`) only if Contract Design
  explicitly authorises touching them.

**Explicitly excluded** from this Planning Review and from its first
implementation programme:

- restart durability (Section 5, above; already separately deferred by
  `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` Section 4's
  own "persistence technology beyond an in-memory implementation"
  exclusion);
- Representation Engine work;
- World Model changes;
- Conversation History changes;
- Evidence Intelligence capability expansion;
- modification of the completed Gap #54 promotion pipeline
  (`src/composition/PermissionFilteredMemoryRetrieval.kt`,
  `src/runtime/DefaultKnowledgeSubmission.kt`,
  `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`) unless a future
  Contract Design proves an unavoidable dependency, explicitly disclosed
  and separately justified, not assumed;
- remote services, databases, embeddings, or semantic-search
  infrastructure — never authorised anywhere in this repository's
  governance for either Memory Core or Knowledge Memory.

---

## 10. Required Governance Sequence

```
Planning Review (this document)
        |
Boundary Review
        |
Contract Design -- resolves Section 6 (content discoverability) and
        Section 7 (retrieval-surface direction) as its own two
        prerequisite questions; resolves Section 8 (Authorization
        Purpose) as a dependent, third question
        |
Scope Lock
        |
Implementation Plan
        |
Independently reviewed implementation units (each requiring its own
        Completion Review and Independent Constitutional Review before
        the next begins, mirroring Gap #54's own established discipline)
        |
Live verification (Section 11, below)
        |
Closure Determination
```

**Stop conditions:**

- No implementation may begin before content representation (Section 6)
  is settled by an accepted Contract Design.
- No implementation may begin before the retrieval-surface contract
  (Section 7) is settled by an accepted Contract Design.
- No implementation may begin before authorization non-widening
  (Section 8) is proven, not merely asserted.
- Any unit must halt, and return the question to Contract Design, if the
  selected design requires reopening a frozen Programme 3 or Memory Core
  guarantee not explicitly authorised by that Contract Design — mirroring
  Gap #54 Implementation Plan's own "a discovered need to change
  [a file outside declared scope] is a stop condition requiring plan
  review, not implied authority" discipline
  (`docs/implementation/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_OPERATIONALISATION_IMPLEMENTATION_PLAN.md`
  Section 11).

---

## 11. Required End-to-End Proof

The first genuinely end-to-end success case, precisely defined:

```
owner Remember X
  -> genuine evidence and promotion (Gap #54's own accepted mechanism,
     unchanged)
  -> later owner query using X's own content, in a separate
     conversational turn
  -> governed KnowledgeRetrieval, resolved by X's content
  -> a ReasoningContext entry genuinely containing X
  -> the real model prompt containing X
```

**A friendly reply alone is not evidence.** This mirrors
`docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_OPERATIONALISATION_UNIT_5_COMPLETION_REVIEW.md`'s
own "real persistence, not a friendly reply" evidentiary discipline
exactly — proof must inspect real `ReasoningContext.entries` or the
assembled model prompt directly.

**Required negative proof:**

- wrong, absent, inactive, or unregistered Purpose, if Purpose is
  adopted by Contract Design;
- denied Memory evidence (a record the requesting principal is not
  authorised to see);
- missing or deleted evidence (a `KnowledgeItem` whose reference no
  longer resolves);
- generic promotion-basis text not falsely matching unrelated content —
  proving the exact defect this programme exists to fix is actually
  fixed, not merely papered over;
- Evidence Intelligence remaining denied in the same runtime, immediately
  after conversational retrieval succeeds (mirroring Gap #54 Unit 5's
  own same-runtime non-widening proof).

**Verification boundary — restart durability.** Live verification under
this Planning Review's own successor programme is intentionally
same-runtime only. No unit's own result may be represented, in any
review or test, as proof of restart durability, and no test may be
added that locks in `KnowledgeItem` data loss across a restart as
required or intended behaviour — doing so would convert a currently
disclosed, honest limitation (Section 5, above) into a claimed
guarantee this Planning Review does not make. Restart durability
remains deferred and must receive its own, separate, future Contract
Design and its own tests before it may be claimed at all.

---

## 12. Explicit Non-Claims

- **Parker does not currently have conversational memory recall.**
- This document implements nothing — no Kotlin, no test, no
  configuration.
- It does not select the final content-representation design (Section 6
  names candidates; it adopts none).
- It does not select the final retrieval-surface mechanism (Section 7
  names the governed direction; it does not specify adapter mechanics).
- It does not decide the Authorization Purpose question (Section 8).
- It does not claim restart durability, in any form.
- It does not create a new gap or a new programme identity (Section 3,
  Section 4).
- It does not reopen Gap #54, which remains complete.
- It does not rename Evidence Intelligence or modify any of its source
  comments.

---

## 13. Recommended Next Step

A Boundary Review, followed by a Contract Design pass scoped
specifically to the three prerequisite questions this Planning Review
names (Section 6, Section 7, Section 8) — beginning with the candidate
directions and required proofs recorded above as its own starting
material, not as pre-made decisions. Neither is authorised or begun by
this document.

```
KNOWLEDGE DISCOVERABILITY AND GOVERNED RETRIEVAL INTO REASONING CONTEXT
PLANNING REVIEW -- COMPLETE, PENDING BOUNDARY REVIEW
```
