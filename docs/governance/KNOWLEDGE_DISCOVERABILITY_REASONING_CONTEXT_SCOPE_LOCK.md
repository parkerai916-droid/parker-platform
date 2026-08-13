**Status:** Scope Lock only. Governance and freezing document. No Kotlin is implemented, proposed
as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. This document
freezes exactly what the first implementation programme may build, from the already-accepted
`docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md` ("the Contract
Design"). It does not redesign any Contract Design decision, does not add capability, does not
select among alternatives the Contract Design already closed, and does not sequence
implementation units -- sequencing remains the Implementation Plan's own, later responsibility.
Gap #54 remains complete and is not reopened.

# Knowledge Discoverability and Governed Retrieval into Reasoning Context — Scope Lock

---

## 1. Status and Authority

Governing inputs, read completely: `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_PLANNING_REVIEW.md`
("the Planning Review"), `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_BOUNDARY_REVIEW.md`
("the Boundary Review"), and the Contract Design, each as currently accepted (the Planning Review
and Boundary Review as amended by their own accepted timing-boundary correction; the Contract
Design as corrected for rendering, the Memory Core status gate, auditability, and the U+2028/U+2029
render-safety gap). Every value this document freezes is transcribed, not reinterpreted, from the
Contract Design; where a value could be read two ways, the Contract Design's own literal text
governs, not this document's own paraphrase.

This Scope Lock authorises nothing by itself. It fixes the boundary an Implementation Plan must
build inside; that plan, and every implementation unit it sequences, remains a separate,
not-yet-authorised, later document and body of work.

---

## 2. Locked Objective

**Same-runtime governed conversational discovery of already-promoted Knowledge Memory content
through Reasoning Context.**

Required proof, exactly (Contract Design Section 14; Planning Review Section 11):

```
owner Remember X
  -> genuine evidence and KnowledgeItem promotion
  -> later owner query using X's content
  -> governed query-time dereference
  -> real ReasoningContext entry containing safely rendered X
  -> real assembled model prompt containing X
```

A friendly reply alone is insufficient -- proof must inspect real `ReasoningContext.entries` or
the real assembled model prompt directly (Contract Design Section 14, mirroring the Unit 5
Completion Review's own "real persistence, not a friendly reply" discipline). Restart durability
is excluded from this objective and from its proof (Section 14, below).

---

## 3. Locked Architecture

The following are frozen, unconditionally, as the Contract Design already adopted them (Contract
Design Section 2, Section 3, Decision Register items 1, 3, 6, 8, 10; Contract Invariants 1-3, 8-10):

1. Query-time governed dereferencing, mirroring `DefaultKnowledgeCandidateEvaluator.resolve()`.
2. Memory Core remains the sole authority for remembered proposition content and provenance.
3. `KnowledgeItem` remains the authority for promotion and evidential state.
4. `KnowledgeItem` carries no copied Memory Core content -- content is resolved fresh, per query,
   per candidate, never cached or persisted by this design.
5. No second durable or indexed content source of any kind.
6. A new, narrow `ReasoningKnowledgeSource` surface (Section 4, below) -- not a widened
   `KnowledgeRetrieval`.
7. `SafeKnowledgeResultEntry` as the sole safe projection crossing into Reasoning Context.
8. `DefaultReasoningKnowledgeSource` as the sole new production implementation of the algorithm
   (Section 5, below).
9. `DefaultReasoningContextAssembler` cutover: `memorySource: KnowledgeSource` replaced by
   `knowledgeSource: ReasoningKnowledgeSource`.
10. Legacy production `KnowledgeSource`/`InMemoryKnowledgeStore` feed retired from production
    composition only (Section 9, below) -- the interfaces themselves are not deleted.
11. `WorldModelSource` and `ConversationHistorySource` inputs to
    `DefaultReasoningContextAssembler` remain completely unchanged.

No alternative to any of the eleven items above is authorised.

---

## 4. Locked Contracts and Signatures

Transcribed exactly from Contract Design Section 4, Section 7, and Section 8. No alternative
signature, name, or shape is authorised; a discovered need to change any of the following is a
Contract Design stop condition (Section 12, below), not implementation discretion.

```kotlin
// src/interfaces/KnowledgeStore.kt (additive)
interface ReasoningKnowledgeSource {
    suspend fun recall(requestingPrincipalId: PrincipalId, query: KnowledgeRetrievalQuery): List<SafeKnowledgeResultEntry>
}

data class SafeKnowledgeResultEntry(
    val content: String,
    val evidentialState: EvidentialState,
    val status: KnowledgeItemStatus,
    val staleness: StalenessDisclosure,
)
```

```kotlin
// src/runtime/DefaultReasoningKnowledgeSource.kt (new file)
internal class DefaultReasoningKnowledgeSource(
    private val persistence: KnowledgeItemPersistence,
    private val permissionEngine: PermissionEngine,
    private val evidenceMemoryRetrieval: MemoryRetrieval, // purpose-bound: forAuthorizationPurpose(REASONING_CONTEXT_RETRIEVAL_PURPOSE)
    private val authorizationPurpose: AuthorizationPurposeId,
    private val clock: Clock = Clock.systemUTC(),
) : ReasoningKnowledgeSource
```

**Fixed result fields, exact order:** `content`, `evidentialState`, `status`, `staleness`. No fifth
field, no reordering, no field renamed.

**Purpose identifier, exact:** `knowledge-memory.reasoning-context-retrieval`.

**Action verb, exact:** `knowledge.retrieve_for_reasoning_context` -- a genuinely new verb, never a
narrowing of `knowledge.retrieve`.

**Resource reuse, exact:** `DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID` -- no new
`Resource` is registered.

**Required policy-rule shapes, exact** (Contract Design Section 7; added alongside the existing
rule list in `src/composition/ParkerRuntime.kt`, after line 675):

```kotlin
PermissionPolicyRule(
    action = PermissionAction.READ, resourceType = ResourceType.MEMORY,
    outcome = PermissionDecisionOutcome.DENIED, level = PermissionLevel.AUTOMATIC,
    proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
)
PermissionPolicyRule(
    action = PermissionAction.READ, resourceType = ResourceType.MEMORY,
    outcome = PermissionDecisionOutcome.APPROVED, level = PermissionLevel.AUTOMATIC,
    authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE,
    proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
)
PermissionPolicyRule(
    action = PermissionAction.READ, resourceType = ResourceType.MEMORY,
    outcome = PermissionDecisionOutcome.APPROVED, level = PermissionLevel.AUTOMATIC,
    authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE,
    proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
)
```

Exactly three new `PermissionPolicyRule` entries -- no fewer, no more, no fourth rule invented.

**Least authority: no `memory.retrieve_document` rule, and none is authorised.** A fourth rule -- a
specificity-2 `APPROVED` rule for `PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME`
(`"memory.retrieve_document"`) and `ResourceType.DOCUMENT` -- is **not locked and is not authorised**
(Contract Design Section 7's own least-authority decision, Contract Invariant 13). No
`memory.retrieve_document` authority is granted to `knowledge-memory.reasoning-context-retrieval`, at
all. `MemoryCoreRecordReference.ToDocument` and `ToRelationship` remain structurally excluded from the
locked algorithm (Section 5, below) -- `getDocument` is never invoked anywhere in this design; only
`getEntity` and `getAssertion`, both through `memory.retrieve`, are ever called. The pre-existing Gap
#54 Unit 2 verb-only `DENIED` guard for `memory.retrieve_document` remains completely untouched and
applicable to this Purpose exactly as to every other Purpose lacking its own specificity-2 override --
an accidental future document call therefore fails closed, by the existing mechanism, with no new code.
`KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE`'s own existing Document authority remains completely unchanged
and is not precedent for granting one here: that Purpose's evaluator genuinely calls `getDocument`,
exercised authority, structurally unlike this design's own never-reached `ToDocument` branch.

---

## 5. Locked Retrieval Algorithm

The exact ten-step ordering Contract Design Section 4 fixes, frozen unconditionally:

1. Query structure validation (already satisfied by `KnowledgeRetrievalQuery`'s own
   construction-time checks -- no separate validation step is authorised).
2. Act-level Knowledge Retrieval authorization.
3. On denial: return `emptyList()`. No persistence read occurs on this path.
4. Enumerate structurally eligible `KnowledgeItem`s (`persistence.findAll()`, filtered).
5. Apply `KnowledgeItem` lifecycle shaping (`ACTIVE`/`RETIRED`, Section 8, below).
6. Item-level Knowledge Retrieval visibility authorization, per candidate, before dereference.
7. Governed Memory Core dereference (purpose-bound `MemoryRetrieval`), only for item-level-approved
   candidates.
8. `ACTIVE`-only Memory Core record-status gate applied at dereference (Section 8, below).
9. Deterministic content normalization, then deterministic content-relevance matching, against
   lawfully resolved content only.
10. Safe result construction (`SafeKnowledgeResultEntry`), then `maximumResults` bounding, applied
    last, after every authorization, visibility, and relevance filter.

**Authorized-partial semantics are frozen.** A resolution failure for any one candidate (denied,
missing, deleted, non-`ACTIVE` status, or an unsupported reference kind) silently excludes that
candidate only -- it never fails the whole query. Fail-whole behaviour is not authorised (Contract
Design Section 9, Decision Register item 7).

Step placement is fixed, not incidental: item-level authorization before dereference (cheapest,
Memory-Core-free filter first); relevance evaluation after dereference (structurally requires
resolved content); bounding last (every earlier filter can shrink the candidate set, and a caller
must never receive fewer than `maximumResults` merely because bounding truncated an unfiltered
list).

---

## 6. Locked Normalization and Rendering

**Normalization** (Contract Design Section 5), applied once, immediately after a record's own
free-text field is read, before matching or rendering:

- Every CRLF pair and every remaining lone CR is replaced by a bare LF; no other transformation.
- Ordinary authorized Unicode text is preserved unchanged -- no trimming, no whitespace collapsing.
- No stemming, tokenization, synonym expansion, classification, or semantic ranking, anywhere.

**Matching** (Contract Design Section 5):

- Case-insensitive substring match of `KnowledgeRetrievalQuery.relevance` against normalized
  content, via Kotlin's `String.contains(other, ignoreCase = true)` -- locale-independent by
  construction (per-`Char` case folding, no `Locale` parameter threaded through anywhere).
- No embeddings, semantic search, or model-based classification, anywhere.

**Entity content construction:** `listOf(entity.primaryLabel).plus(entity.aliases).joinToString(" | ")`
-- the fixed, literal separator `" | "` (space, pipe, space), never a locale-sensitive
list-formatting function.

**Rendering** (Contract Design Section 8) -- the exact, single internal model-prompt format, fixed
field order `content` (escaped), `evidentialState`, `status`, `staleness`, enum values rendered via
Kotlin's stable, unlocalized `.name`:

```kotlin
"Memory: ${escapeForPrompt(entry.content)} (evidentialState=${entry.evidentialState.name}, " +
    "status=${entry.status.name}, staleness=${entry.staleness.name})"
```

**Exact escaping, frozen:** backslash, LF, CR, TAB (each a fixed two-character form: `\\`, `\n`,
`\r`, `\t`), every remaining C0 control character (`0x00`-`0x1F`), DEL (`0x7F`), every C1 control
character (`0x80`-`0x9F`), Unicode LINE SEPARATOR (`U+2028`), and Unicode PARAGRAPH SEPARATOR
(`U+2029`) -- the latter two named explicitly, since neither is a C0/C1 control character. Every
one of these, except the four named two-character forms, escapes to a deterministic, zero-padded,
four-hex-digit `\uXXXX` form. This closed set is the full "line-boundary audit" the Contract Design
already performed (its own Section 8): NEL (`U+0085`) already falls inside the C1 range; VT, FF,
and the File/Group/Record/Unit Separators (`0x0B`, `0x0C`, `0x1C`-`0x1F`) already fall inside the
C0 range. No further character remains capable of creating a line boundary this scheme leaves open,
and no broader Unicode sanitization is authorised beyond this closed set.

**Required tests, frozen:** content containing `U+2028` or `U+2029` produces the literal
`\u2028`/`\u2029` escape sequence, never the real separator character, in the rendered `String`; a
rendered entry whose source content embeds a raw LF, CR, `U+2028`, or `U+2029` still yields exactly
one `ReasoningContext` entry, with no additional prompt line or entry created by the embedded
character.

---

## 7. Locked Principal, Purpose, and Policy

- **Owner/requesting principal.** `message.senderPrincipalId` is the sole `requestingPrincipalId`
  passed to `recall`, reaching both the Knowledge Retrieval gates and Memory Core evidence-resolution
  authorization -- one propagated value, never re-derived.
- **No system-principal substitution.** `SYSTEM_PARKER_PRINCIPAL_ID` is never `requestingPrincipalId`
  for a `recall` call; it remains lawful only for unrelated composition-time acts.
- **Correlation propagation.** `message.correlationId.value` propagates unchanged through every
  `ExecutionRequest` this design constructs, at both granularities. No new correlation identifier is
  minted anywhere inside the retrieval chain.
- **Fresh request IDs only per evaluation.** Only `ExecutionRequest.requestId` is freshly minted,
  once per `permissionEngine.evaluate` call -- never a correlation value.
- **Purpose registration.** `knowledge-memory.reasoning-context-retrieval` must be registered and
  `ACTIVE` at composition time for `DefaultPermissionPolicy`'s own `isActive` check to fold it into
  `effectivePurpose`.
- **Purpose-specific approval and verb-specific denial guard.** Frozen exactly as Section 4's three
  `PermissionPolicyRule` entries above: a specificity-1 `DENIED` guard for
  `knowledge.retrieve_for_reasoning_context`, outranked only by a specificity-2 `APPROVED` rule
  naming both the verb and the exact, active Purpose.
- **Wrong/absent/inactive/unregistered/mismatched Purpose denial.** Each folds to "no purpose"
  (`DefaultPermissionPolicy`'s own existing, unmodified mechanism), which cannot satisfy the
  specificity-2 rule, leaving only the specificity-1 `DENIED` guard applicable -- denied,
  automatically, with no new code.
- **Memory Core purpose-bound approval.** Exactly one new specificity-2 `APPROVED` rule, for
  `memory.retrieve` alone, narrowed to the same Purpose -- the existing Gap #54 Unit 2 `DENIED` guard
  for `memory.retrieve_document` already governs this Purpose, and every other Purpose lacking its own
  specificity-2 override, unchanged (Section 4, above).
- **Least authority, as an invariant.** Only operations the locked algorithm actually reaches may be
  approved under `knowledge-memory.reasoning-context-retrieval` --
  `knowledge.retrieve_for_reasoning_context` and `memory.retrieve` -- and no operation beyond that; no
  document-retrieval authority (`memory.retrieve_document`) exists under this Purpose, at all.
- **Evidence Intelligence non-widening.** `EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE` and
  `knowledge-memory.reasoning-context-retrieval` are distinct values; no rule this programme adds can
  ever satisfy Evidence Intelligence's own Purpose, and no rule it already relies on is modified --
  a structural, not merely observed, proof.

---

## 8. Locked Lifecycle and Failure Semantics

- `KnowledgeItem.status` (`ACTIVE`/`RETIRED`) semantics are unchanged, mirroring
  `DefaultKnowledgeRetrieval.isRetrievable` exactly: `ACTIVE` eligible by default, `RETIRED`
  excluded unless `includeRetired = true`.
- Ordinary conversational retrieval excludes retired items -- `DefaultReasoningContextAssembler`'s
  own constructed `KnowledgeRetrievalQuery` does not set `includeRetired`.
- Memory Core content resolution is `ACTIVE`-only -- a binding Contract Design decision, not
  implementation-defined; changing it requires a future Contract Design revision.
- `DISPUTED`, `SUPERSEDED`, `ARCHIVED`, and `DELETED` referenced Memory Core content is unavailable
  for conversational discovery in this programme, distinct from and independent of
  `KnowledgeItem.status` lifecycle filtering -- both gates must pass; neither substitutes for the
  other.
- Denial and authorized-empty share the identical external empty-`List` shape -- externally
  indistinguishable, by construction.
- Authorized-partial results are frozen (Section 5, above).
- Genuine dependency faults (`persistence`/`permissionEngine`/`evidenceMemoryRetrieval` throwing)
  propagate unchanged -- no `try`/`catch` anywhere in `DefaultReasoningKnowledgeSource`.
- No durable permission-decision audit claim is made. `DefaultPermissionEngine` retains no decision
  history and publishes no event for these direct, self-gating calls; internal control-flow
  knowledge exists only within one call's own transient execution. Adding audit persistence or event
  publication requires separate, future governance.
- Natural latency variation across denial, authorized-empty, filtering, and dereference paths is
  disclosed, never claimed to be eliminated.
- No constant-time execution or resistance-to-active-timing-analysis claim is made, anywhere.
- No explicit timing field, count, denial marker, deliberate delay, or deliberately encoded
  protected-state timing signal crosses the `recall` result boundary.

---

## 9. Locked File Scope

**Production, exactly these four files -- no other production file is authorised without a Scope
Lock amendment:**

- `src/interfaces/KnowledgeStore.kt` -- additive only (`ReasoningKnowledgeSource`,
  `SafeKnowledgeResultEntry`). **Additive declarations here do not authorise deletion or alteration
  of any existing declaration in this file** -- `KnowledgeSource`, `KnowledgeStore`,
  `KnowledgeRetrieval`, `KnowledgeRetrievalQuery`, `KnowledgeRetrievalResult`, `KnowledgeResultEntry`,
  and every other existing declaration in this file remain byte-for-byte unchanged.
- `src/runtime/DefaultReasoningKnowledgeSource.kt` -- new file.
- `src/runtime/DefaultReasoningContextAssembler.kt` -- constructor signature change; `assemble`'s
  memory-rendering block replaced with the fixed rendering contract (Section 6, above).
- `src/composition/ParkerRuntime.kt` -- new Purpose constant and registration; the three locked
  `PermissionPolicyRule` entries (no `memory.retrieve_document` approval -- Section 4's own
  least-authority decision); one new `ActionVocabularyEntry`; `DefaultReasoningKnowledgeSource`
  construction; retirement of the `InMemoryKnowledgeStore`/`memorySource` production binding only.

**Tests, exactly these six files -- the prior provisional `tests/composition/` allowance is now
resolved and closed, not open-ended:**

- `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt` -- existing (added by merged Implementation
  Unit 2; named as a new file when this Scope Lock was first accepted, before Unit 2 merged).
- `tests/runtime/DefaultReasoningContextAssemblerTest.kt` -- existing; extended.
- `tests/composition/ParkerRuntimeAuthorizationPurposeCompositionTest.kt` -- existing. Authorised in
  Implementation Unit 3 only for compatibility corrections to the two stale closed-world assertions
  this file itself carries (of four total, across both files below) that this programme's own locked
  third Purpose and three-rule production contract (Section 4, above) directly and foreseeably
  invalidates. No new behaviour, proof responsibility, reflection mechanism, or production change is
  authorised in this file.
- `tests/composition/ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt` -- existing.
  Authorised in Implementation Unit 3 for the identical, narrow compatibility-only purpose as the file
  immediately above -- correcting the other two of the same four stale assertions (the file
  immediately above carries the first two), no more.
- `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` -- new file, authorised
  only for Implementation Unit 4, not before.
- `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` -- existing; extended, authorised
  only for Implementation Unit 5, not before.

**Resolution of the prior provisional allowance.** This Scope Lock's own Section 12 stop condition --
"Halt if any proposed implementation unit requires a file outside the locked scope (Section 9, above)
-- a discovered need is a stop condition requiring a return to this Scope Lock, not implied authority
to proceed" -- was triggered during Implementation Unit 3: `ParkerRuntimeAuthorizationPurposeCompositionTest.kt`
and `ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt` are existing files the
Implementation Plan's own Section 3 resolution did not name, although both carry closed-world
assertions (exact registered-Purpose-set counts, exact Purpose-bound-rule counts) that the third
Purpose and three-rule contract this Scope Lock already locked (Section 4, above) directly and
foreseeably invalidates. That stop condition functioned exactly as designed -- Unit 3 halted rather
than editing either file without authorisation. This correction resolves it by naming both files
above, narrowly, for compatibility correction only. It authorises no additional capability, Purpose,
rule, or production change beyond what Section 4 (above) already locked, and no `memory.retrieve_document`
approval of any kind. No other production or test file is authorised without a further Scope Lock
amendment.

---

## 10. Explicit Exclusions

Frozen out of this programme entirely -- none of the following may be touched, added, or claimed by
any implementation unit this Scope Lock authorises:

- Changes to `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`, `KnowledgeRetrievalResult`, or
  `KnowledgeResultEntry`.
- Changes to `PermissionFilteredMemoryRetrieval`.
- Changes to the Remember/promotion path (`MemoryAdmissionCoordinator`,
  `DefaultKnowledgeSubmission`, `DefaultKnowledgeCandidateEvaluator`).
- Deletion of `KnowledgeSource`, `KnowledgeStore`, or `InMemoryKnowledgeStore`.
- Restart durability, in any form.
- Semantic search, embeddings, databases, remote services, or indexes.
- Representation Engine work, or any final owner-facing explanation design.
- World Model or Conversation History changes.
- Evidence Intelligence capability expansion.
- Durable permission-decision audit infrastructure (persistence or event publication).
- Constant-time padding, batching, obfuscation, or any other timing-channel mitigation.
- Broader Programme 4 propositional-integrity or burden-of-justification work.

---

## 11. Required Verification Matrix

Frozen from Contract Design Section 14, condensed to the seams the Implementation Plan must cover:

- Deterministic matching and normalization (case-insensitivity, locale-independence, CRLF/CR-to-LF,
  Unicode preservation, Entity separator).
- Exact rendering and escaping, including `U+2028`/`U+2029` literal-escape and non-injection proof
  (Section 6, above).
- Lifecycle (`ACTIVE`/`RETIRED` default and `includeRetired`) and the binding `ACTIVE`-only Memory
  Core status gate.
- Ordering (insertion order preserved) and bounds (`maximumResults` applied last).
- Act-level denial before persistence inspection (zero `persistence.findAll()` invocations proven).
- Item-level denial (silent exclusion).
- Purpose denial matrix (wrong/absent/inactive/unregistered/mismatched) and coarse-rule/cross-Purpose
  fall-through prevention.
- Denied, missing, deleted, and unsupported-reference-kind evidence (silent exclusion, no exception).
- Authorized-partial results (one candidate resolves, another is denied).
- Evidence Intelligence non-widening, same runtime, immediately after a successful `recall`.
- Least-authority proof: `memory.retrieve_document` under `knowledge-memory.reasoning-context-retrieval`
  is `DENIED` (a direct `DefaultPermissionPolicy.evaluate` proof, not merely inferred); a direct
  `getDocument(...)` call through the real, purpose-bound `MemoryRetrieval` view
  `DefaultReasoningKnowledgeSource` itself holds returns no document, against a genuine, existing
  `Document` record; `KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE`'s own existing Document-approval
  behaviour continues to pass unmodified; Evidence Intelligence remains denied under this Purpose.
- Composition cutover proof: `DefaultReasoningContextAssembler` never receives a `KnowledgeSource`;
  no production path constructs `InMemoryKnowledgeStore`; no two production knowledge feeds active
  simultaneously.
- Regression proof: every non-memory Reasoning Context entry kind, rendering, and relative ordering
  unchanged.
- The real, same-runtime end-to-end proof (Section 2, above) -- inspecting real
  `ReasoningContext.entries` or the real assembled model prompt directly.

**No elapsed-time-threshold test may claim constant-time behaviour.** Tests may prove only the
absence of explicit timing metadata and the absence of intentional timing encoding.

---

## 12. Stop Conditions

Every Contract Design stop condition (its own Section 18) is frozen, unconditionally:

- No Kotlin implementation may begin before an accepted Scope Lock and Implementation Plan each
  exist.
- Halt if `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`, `KnowledgeRetrievalResult`, or
  `KnowledgeResultEntry` is found to require a change.
- Halt if `KnowledgeSource`, `KnowledgeStore`, or `InMemoryKnowledgeStore` deletion is proposed
  without a fresh repository check proving zero remaining consumers.
- Halt if remembered content would be duplicated outside Memory Core, in any form.
- Halt if Reasoning Context or the model would gain raw Memory Core access, or a reusable
  `MemoryRetrieval`-shaped capability.
- Halt if Evidence Intelligence authority would widen, in any form.
- Halt if authorization would occur after persistence or content disclosure, at any stage.
- Halt if a broad or coarse rule is found to override absent, inactive, unregistered, wrong, or
  mismatched Purpose for `knowledge.retrieve_for_reasoning_context`, `memory.retrieve`, or
  `memory.retrieve_document`.
- Halt if two production knowledge feeds are found active simultaneously.
- Halt if a frozen Programme 3 or Memory Core guarantee is found to require reopening beyond what
  the Contract Design explicitly authorises.
- Halt if live verification cannot inspect real `ReasoningContext.entries` or the real assembled
  model prompt directly.
- Halt if unescaped content, or content containing an unescaped LF/CR, other unescaped control
  character, or an unescaped `U+2028`/`U+2029`, is found reaching `ReasoningContext.entries`.
- Halt if the `ACTIVE`-only Memory Core record-status gate is treated as freely revisable,
  configurable, or implementation-defined without a future Contract Design revision.
- Halt if implementation introduces an intentional timing signal, a deliberate delay, or explicit
  protected-state timing metadata -- never merely because authorized and denied paths naturally take
  different amounts of time.
- Halt if any later review or test claims constant-time execution or resistance to timing analysis
  without a separately governed mitigation mechanism and its own, matching verification.
- Halt if durable audit persistence or event publication for `permissionEngine.evaluate` decisions
  is added without separate, future governance authorising it.

**This Scope Lock adds:**

- Halt if the Implementation Plan cannot name the exact `tests/composition/` file(s) it will create
  or extend -- the provisional allowance (Section 9, above) may not proceed to implementation
  unresolved.
- Halt if any proposed implementation unit requires a file outside the locked scope (Section 9,
  above) -- a discovered need is a stop condition requiring a return to this Scope Lock, not implied
  authority to proceed.
- Halt if the exact rendering contract, escaping scheme, Purpose identifier, action verb, or any
  locked `PermissionPolicyRule` shape (Sections 4, 6, 7, above) is treated as implementation
  discretion rather than a frozen value.
- Halt if any test substitutes a synthetic, hand-constructed `KnowledgeItem` for the required
  genuine promotion-to-recall end-to-end proof (Section 2, above) -- the required proof is real
  `MemoryAdmissionCoordinator` promotion through real `DefaultReasoningKnowledgeSource.recall`, never
  a fixture standing in for either.
- Halt if `knowledge-memory.reasoning-context-retrieval` gains `memory.retrieve_document` or any other
  operation not reachable in the locked algorithm (Section 4, Section 5, above).
- Halt if implementation adds a `ToDocument`/`getDocument` path to this design without a future
  Contract Design revision and its own corresponding Scope Lock amendment.

---

## 13. Completion Criteria

Scope Lock completion, and therefore this programme's own Closure Determination, requires:

- Every locked production contract (Section 4, above) implemented exactly as frozen, including exactly
  three new `PermissionPolicyRule` entries and no `memory.retrieve_document` authority granted under
  `knowledge-memory.reasoning-context-retrieval` -- confirmed by both the implementation units
  themselves and their own required Completion and Constitutional Reviews, below.
- All required tests (Section 11, above) passing.
- No excluded file (Section 10, above) changed.
- An independent Completion Review and an Independent Constitutional Review for every
  implementation unit, before the next unit begins -- mirroring Gap #54's own established
  discipline.
- Hosted and local Gradle build success.
- Real, same-runtime live verification of the required end-to-end proof (Section 2, above).
- A Closure Determination document.
- No claim of restart durability, anywhere, in any review or test this programme produces.

---

## 14. Explicit Non-Claims

- This document implements nothing -- no Kotlin, no test, no configuration.
- It does not begin an Implementation Plan, and does not sequence implementation units.
- It does not redesign, reinterpret, or relitigate any Contract Design decision -- every value
  above is transcribed, not chosen, by this document.
- It does not authorize an additional implementation file, capability, or alternative beyond the
  Contract Design's proposed set.
- It does not reopen Gap #54, which remains complete.
- It does not create or reserve a new numbered gap, or a new programme identity.
- It does not claim restart durability, in any form.
- It does not claim constant-time execution, resistance to active timing analysis, or durable
  permission-decision auditing -- both non-claims are inherited, unweakened, from the Contract
  Design (its own Section 16).
- It does not select final, owner-facing Representation Engine wording -- only the internal
  model-prompt format (Section 6, above) is fixed.
- It does not modify any of the three governing documents it reads from.

---

## 15. Next Stage

An Implementation Plan, scoped exclusively to the locked file set (Section 9, above), sequencing
independently reviewed implementation units against the locked contracts, algorithm, and
verification matrix (Sections 4-8, 11, above) -- each unit requiring its own Completion Review and
Independent Constitutional Review before the next begins. The Implementation Plan's own first
obligation is to resolve Section 9's provisional `tests/composition/` allowance into exact file
names before any unit may begin. Neither the Implementation Plan nor any implementation unit is
authorised or begun by this document.

```
KNOWLEDGE DISCOVERABILITY AND GOVERNED RETRIEVAL INTO REASONING CONTEXT
SCOPE LOCK -- COMPLETE, PENDING IMPLEMENTATION PLAN
```
