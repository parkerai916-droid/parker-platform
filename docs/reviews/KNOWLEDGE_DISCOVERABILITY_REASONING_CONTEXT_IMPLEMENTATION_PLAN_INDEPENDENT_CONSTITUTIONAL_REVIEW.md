**Status:** Independent Constitutional Review of the Knowledge Discoverability and Governed Retrieval into Reasoning Context Implementation Plan. Constitutional plan review only; no implementation, test result, live verification, audit, or closure claim is made.

# Knowledge Discoverability and Reasoning Context Implementation Plan — Independent Constitutional Review

## 1. Baseline and independent method

Reviewed from scratch against:

```text
base=ac6f861111d74be13fe5b220e598dfd7159b6e6a
plan=59521aed8df5c212bc03a43fb3142dc3564e456c
```

The full corrected Plan, all four governing documents, every frozen production/test path, and the relevant authorization, composition, Memory Core, Knowledge Memory, Remember/promotion, and prompt caller graph were freshly examined. Earlier accepted or rejected reviews were not treated as evidence that this immutable commit was correct.

## 2. Constitutional ownership and authority

The Plan preserves Memory Core as the sole authority for proposition content and provenance and `KnowledgeItem` as the authority for promotion and evidential state. Content is resolved at query time through a governed, Purpose-bound `MemoryRetrieval` view and crosses into Reasoning Context only as `SafeKnowledgeResultEntry`. No new durable or indexed content store, raw Memory Core capability, reusable retrieval handle, or production `InMemoryKnowledgeStore` feed is authorized.

The public `KnowledgeRetrieval` contract and its frozen result types remain unchanged. World Model, Conversation History, the Remember/promotion path, Evidence Intelligence, and Representation Engine ownership remain outside the implementation boundary.

**Finding:** ownership remains one-directional and no competing authority or duplicate durable source is introduced.

## 3. Least-authority policy challenge

The Plan adds exactly three rules:

1. a verb-specific `DENIED` guard for `knowledge.retrieve_for_reasoning_context`;
2. an exact Purpose-plus-verb approval for that act;
3. an exact Purpose-plus-verb approval for reachable `memory.retrieve` evidence resolution.

It adds no fourth rule and no `memory.retrieve_document`/`DOCUMENT` approval. `ToDocument` and `ToRelationship` are structurally absent from `DefaultReasoningKnowledgeSource`; only `getAssertion` and `getEntity` are reachable. The existing Gap #54 Document guard remains the governing fail-closed rule for this Purpose.

The new Purpose is distinct from both candidate evaluation and Evidence Intelligence. The real policy's exact-purpose comparison means no rule added here can widen Evidence Intelligence, while the existing candidate-evaluation Document approval remains untouched.

**Finding:** the authority granted is exactly the authority reachable by the locked algorithm and no more.

## 4. Corrected vocabulary ownership and non-vacuous denial

Fresh source inspection confirms that `memory.retrieve_document -> READ / DOCUMENT` already exists in production at `ParkerRuntime.kt` lines 529–533 as Gap #54 Unit 2 wiring. The corrected Plan assigns Unit 3 no ownership over that entry: it must preserve it unchanged. Lines 991–1014 are used only as the separate Knowledge Retrieval registration site for Unit 3's new `knowledge.retrieve_for_reasoning_context -> READ / MEMORY` entry. Unit 4 repeats the old Document value only in its independent test graph.

The required exact vocabulary lookup and real `ActionMapper.map(...)` result exclude both `UNKNOWN_ACTION` and `RESOURCE_TYPE_MISMATCH`. The principal must be registered as `CREATED`, transitioned to `ACTIVE`, and directly confirmed active, so identity rejection cannot satisfy the proof. The Purpose must be registered and active, so Purpose rejection cannot satisfy it. The targetless `ExecutionRequest` correctly carries no `resourceType`; the real policy derives `DOCUMENT` from the frozen mapping. The remaining applicable maximal rule is the existing verb-specific Document guard, which determines `DENIED`.

The same real engine and active principal drive the direct purpose-bound view. `PermissionFilteredMemoryRetrieval.getDocument` fetches the genuine delegate record and then returns `null` after denial. This proves nondisclosure honestly without pretending the delegate was not called.

**Finding:** the Document proof exercises the real policy path non-vacuously and does not manufacture denial at a preliminary gate.

## 5. Former sequencing and accessibility defects

The formerly non-compiling Unit 3/4 boundary is closed. Unit 3 owns the assembler implementation, the complete assembler test adaptation, and `ParkerRuntime` together. The constructor change, governed source construction, legacy-feed retirement, and sole production caller update occur in one unit and one review boundary. Unit 4 is test-only and cannot repair production.

The formerly inaccessible `ParkerRuntime`-local Purpose-bound view is no longer treated as something Unit 4 must reach. The least-authority proof constructs an independent graph from real, unmodified production classes and calls the existing `internal forAuthorizationPurpose(...)` factory directly from the test module. It requires no private-instance reflection, production accessor, visibility widening, raw capability exposure, new production file, or new Document path in the reasoning source.

**Finding:** the corrected sequence is compilable at unit boundaries and the proof does not weaken encapsulation to become testable.

## 6. Item-level denial and feasible proof allocation

The real `DefaultPermissionPolicy` has no item/evidence identity discriminator and cannot create mixed outcomes for two candidates sharing the same policy tuple. The Plan states this limitation explicitly and does not claim otherwise.

Differential item-level visibility denial and denied-evidence partial results belong to Unit 2, using controllable test doubles at the implementation boundary. Unit 2 separately requires denied Assertion, denied Entity, missing Assertion, missing Entity, unsupported Document, unsupported Relationship, specifically denied authorized-partial, every record status, and generic-basis false-match proofs. No proof may substitute for another.

Unit 5 is positive-only: real owner Remember, real Memory Core evidence, real `DefaultKnowledgeSubmission` promotion, a separate query turn, real governed recall, and inspection of the real assembled prompt. The previously proposed mixed denied-evidence companion is removed because the composed real policy cannot produce its required differential state for one active owner and one Purpose. Denied-evidence guarantees remain fully proven by Unit 2, so this removal weakens no constitutional guarantee.

**Finding:** each proof is placed at the lowest honest tier capable of producing its required state; no test seam misrepresents production policy.

## 7. Determinism, disclosure, and non-claims

The Plan fixes authorization before persistence/content disclosure, `ACTIVE`-only referenced-record status, deterministic CRLF/CR normalization, locale-independent substring matching, Entity label/alias construction, prompt escaping including `U+2028`/`U+2029`, exact rendering order, insertion-order preservation, authorized-partial behavior, and last-step result bounding.

Denial and authorized-empty share the same returned empty-list shape. Missing, deleted, denied, unsupported, and non-`ACTIVE` evidence are silently excluded under separately required tests. Dependency faults propagate rather than being converted into fabricated results.

The Plan honestly discloses naturally variable latency and makes no constant-time or timing-resistance claim. It makes no durable permission-decision audit claim because `DefaultPermissionEngine` retains no history and emits no such event. It makes no implementation-completion, test-success, live-recall, restart-durability, programme-closure, or Closure Determination claim.

**Finding:** uncertainty and limitations are disclosed rather than converted into unsupported guarantees.

## 8. Scope Lock and review-gate challenge

The Plan resolves the provisional composition directory allowance to exact filenames, freezes exactly eight implementation files and five strictly linear units, carries every Scope Lock stop condition forward, and adds sequencing-specific stops for file overreach, missing reviews, test-file drift, non-atomic cutover, and Unit 4 production repair.

Every unit has explicit entry criteria, exit criteria, tests, reviews, and stop conditions. The traceability table agrees with the unit sections and the proof matrices. The three new files are accurately marked new; the other five are accurately marked additive or extended. Citations and line ranges examined against the base are accurate, including the corrected Document vocabulary ownership and the separate reasoning-context registration site.

**Finding:** no unresolved constitutional decision, authority expansion, file-scope ambiguity, or sequencing escape remains.

## 9. Findings and verdict

```text
P0=0
P1=0
P2=0
P3=0
VERDICT=ACCEPTED
```

No substantive constitutional correction remains.

`ACCEPTED` means only that the Implementation Plan at commit `59521aed8df5c212bc03a43fb3142dc3564e456c` is constitutionally aligned and complete enough to govern future implementation against base `ac6f861111d74be13fe5b220e598dfd7159b6e6a`. It does not establish that implementation has begun or succeeded, that any future Gradle suite passes, that live conversational recall works, that state survives restart, that permission decisions are durably audited, or that the programme is complete or closed.
