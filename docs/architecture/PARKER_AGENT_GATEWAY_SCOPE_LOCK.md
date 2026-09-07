# Parker Agent Gateway — Scope Lock

## Status

**Status: ACCEPTED — CANONICAL — FROZEN**

Owner: Steven Francis McTague

Accepted: 7 September 2026

The owner reviewed this scope lock in its revised form (AG-1A Owner Review —
REVISE, then AG-1A Owner Decision — ACCEPT) and accepted its substance:
Hermes is frozen as an external, untrusted, subordinate operator; Parker
retains all evidence, permission, egress, HFR, and execution authority; the
Agent Gateway is structurally separate from Owner UI authentication; the
implementation is decomposed into AG-1B through AG-1G; R1 remains blocked on
separately governed source-identity/idempotency work (Section 8, Section
16); and Section 9 freezes only the R2 governance requirement, not a
specific implementation shape. No substantive scope-lock content was altered
between the ACCEPT decision and this status update — only this Status
section and the Owner Decision checkboxes (Section 22) changed.

**This document remains governance/design only and authorises no
implementation on its own.** It freezes the constitutional boundary a future
Parker Agent Gateway must respect; it does not write, propose, or imply
Kotlin beyond the illustrative signatures already quoted from HEAD
`3024ecfab3b8ac86c2e2dd70482fbf412434a067`. Acceptance of this scope lock
does not itself authorise AG-1B or any later unit — each still requires its
own separate Owner Decision before implementation begins (Section 20).
Every claim about existing behaviour below was verified by direct inspection
of source at that exact commit — none is re-derived from README.md (stale,
per the original unit brief) or from any prior external description of
Parker.

---

## 1. Purpose

Define the canonical governance boundary for a **Parker Agent Gateway**: a
dedicated external-agent surface that lets an external, subordinate AI agent
— initially the Nous Hermes Agent, running in its own VM — orchestrate Parker
ingestion work (eventual owner experience: `/parker-ingest
michael-era-bundle.pdf`) **without** becoming part of Parker's trust chain and
**without** bypassing any governance mechanism Parker already enforces for its
Owner UI. This document is the boundary an Implementation Plan may not cross
in one direction (inventing new governance) and must be free to cross in the
other (choosing Kotlin types, field names, HTTP framing details).

---

## 2. Architectural status of Hermes

Hermes is an **external, untrusted, subordinate operator** — never a
component of Parker, never a Plugin in the `Plugin` interface sense
(`docs/specifications/volume-03-core-interfaces/Plugin.md`, in-process,
manifest-declared), and never eligible for `PrincipalType.INTERNAL_AGENT`
(`src/contracts/Principal.kt:16`) — that enum member exists for cognition
Parker itself runs, not for a remote process in a separate VM proposing work
across a network boundary. Hermes may **propose** operations. It never
**executes** them; ARCHITECTURE_PRINCIPLES.md §1 ("Language models propose
actions. They never execute them.") governs Hermes's output exactly as it
governs Parker's own reasoning layer's output — Hermes's proposals carry no
more inherent authority than an `AgentStepDecision.Propose`
(`src/contracts/AgentStep.kt`), whose own KDoc already states the operative
principle verbatim: it "carries no authority" until independently evaluated
by `PermissionEngine`. This is the closest existing structural precedent in
the codebase for "an external cognition proposes, Parker's pipeline decides,"
and the Gateway must not invent a different shape for the same idea.

---

## 3. Trust boundary

- Hermes runs outside Parker's process and outside Parker's host trust
  boundary (a separate VM). It is never granted filesystem, database, Docker
  socket, SSH, or credential access to the Parker host (Section 6).
- Every Hermes-originated call that reaches Parker runtime logic must resolve
  to one explicit, attributable `Principal` (Section 10) and pass through the
  same, single, unmodified `PermissionEngine.evaluate` gate
  (`src/interfaces/PermissionEngine.kt:4`) every other Parker request passes
  through — no second evaluator, no bypass.
- Hermes output is data until a Permission Engine decision says otherwise.
  Nothing Hermes reports about itself — a claimed hash, a claimed completion,
  a claimed authorization — is authoritative Parker state merely because
  Hermes asserts it (Section 13, Section 16).
- The trust boundary is enforced at the network/authentication layer (a
  dedicated Agent Gateway credential, never the Owner UI's session cookie or
  device pairing — Section 10, Section 18) and at the permission layer (fresh
  `PermissionPolicyRule`s scoped to Hermes's own principal — Section 11). Both
  layers must independently fail closed; neither alone is sufficient.

---

## 4. Non-authority statement

The Parker Agent Gateway is **authority-free by construction**. It holds no
governance decision of its own:

- It does not decide evidence identity, custody, or hashing — `EvidenceCustodian`
  (`src/interfaces/EvidenceCustodian.kt`) does.
- It does not decide acquisition routing or capability selection —
  `DeterministicEvidenceAcquisitionRouter` and `GovernedAcquisitionExecutionCoordinator`
  (`src/runtime/GovernedAcquisitionIntegration.kt`,
  `src/runtime/GovernedAcquisitionOwnerWorkflow.kt`) do.
- It does not decide permission — `DefaultPermissionEngine`/`DefaultPermissionPolicy`
  (`src/runtime/DefaultPermissionEngine.kt`, `src/runtime/DefaultPermissionPolicy.kt`)
  do, unmodified.
- It does not decide external-egress authorization —
  `ExternalTranscriptionOwnerAuthorizationCoordinator`
  (`src/runtime/ExternalTranscriptionOwnerAuthorization.kt`) does.
- It does not decide provider readiness/acceptance — the existing
  `OpenAiExternalTranscriptionProviderProfile.acceptanceState` /
  `AcquisitionAvailability` mechanism (wired in `src/composition/ParkerRuntime.kt`,
  commit `3024ecf`) does.
- It does not decide Human Fidelity Review outcomes —
  `GovernedHumanFidelityReviewRecordingService`
  (`src/interfaces/HumanFidelityReviewRecordingService.kt`) does, and only for
  the owner principal (Section 13).
- It does not decide analysis eligibility, provenance, or audit — the
  existing Memory Core (`MemoryCore.createProvenance`), acquisition/OCR
  provenance types, and per-domain audit writers (Section 12) do.

The Gateway's only job is: authenticate Hermes as its own distinct principal,
translate an HTTP request into the narrowest already-governed call, and
project the narrowest necessary response. Every substantive decision listed
above remains exactly where it already lives today.

---

## 5. Existing Parker mechanisms reused

No new identity, permission, execution, or audit architecture is invented.
The Gateway is built entirely from mechanisms already present at HEAD:

| Concern | Reused mechanism |
|---|---|
| Identity | `Principal`/`PrincipalId`/`PrincipalType`/`PrincipalStatus` (`src/contracts/Principal.kt`); ADR-013 ("Internal actors must have explicit principal identities") |
| Request carrier | `ExecutionRequest` (`src/contracts/ExecutionRequest.kt`), unmodified; `RequestOrigin.AGENT`/`RequestOrigin.REMOTE_INTERFACE` already exist (lines 37, 41) and require no new enum member |
| Permission decision | `PermissionEngine.evaluate`/`explain` (`src/interfaces/PermissionEngine.kt`), `DefaultPermissionEngine`, `DefaultPermissionPolicy` — interfaces and fail-closed behaviour completely unmodified (Section 17) |
| Action vocabulary | `ActionVocabulary`/`InMemoryActionVocabulary`/`ActionMapper` (`src/runtime/ActionMapper.kt`) — new verb phrases registered once at composition time, exactly as `NOTIFY_OWNER_VERB_PHRASE` etc. already are |
| Authorization dimension | `AuthorizationPurposeRegistry`/`AuthorizationPurposeId` (`src/runtime/AuthorizationPurposeRegistry.kt`, `docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md`) — a new namespaced value scopes gateway rules independently of owner rules for the same (action, resourceType) pair |
| Resource identity | `ResourceRegistry`/`InMemoryResourceRegistry` (`src/interfaces/ResourceRegistry.kt`) — unmodified; opaque `ResourceId`s only |
| Evidence custody | `EvidenceCustodian.accept/retrieve/retrieveManifest` (`src/interfaces/EvidenceCustodian.kt`) — `retrieveEvidence`'s own KDoc (`ParkerRuntime.kt:2436-2440`) already anticipates this exact use: *"the Permission Engine, not this method, is the intended gate for who may retrieve (Contract Design Section 6.3 anticipates a non-owner Evidence Intelligence consumer requesting authorised read access, 'acting only as a consumer')"* |
| Evidence registration | `EvidenceRegistrationCoordinator`, `submitEvidence` (`ParkerRuntime.kt:2409`) — already accepts a caller-supplied `requestingPrincipalId`, unlike most owner entry points (Section 10) |
| Acquisition orchestration | `GovernedAcquisitionOwnerWorkflow`, `GovernedAcquisitionExecutionCoordinator`, `DeterministicEvidenceAcquisitionRouter`, `AuthoritativeAcquisitionSourceResolver` (`src/runtime/GovernedAcquisitionOwnerWorkflow.kt`, `GovernedAcquisitionIntegration.kt`, `DeterministicEvidenceAcquisitionRouter.kt`, `AuthoritativeAcquisitionSourceResolver.kt`) |
| External transcription | `ExternalTranscriptionOwnerInvocationCoordinator`, `ExternalTranscriptionOwnerAuthorizationCoordinator`, `ExternalTranscriptionInvocationGate` (`src/runtime/ExternalTranscription*.kt`) |
| Derivative discovery | `OcrDerivativeGenerationDiscovery`, `TierBOcrDerivativeGenerationDiscoveryCoordinator.discover` (`src/runtime/TierBOcrDerivativeGenerationDiscoveryCoordinator.kt`) |
| HFR read | `EffectiveHumanFidelityReviewProjector`, `TierBOcrHumanFidelityReviewCoordinator.projectEffectiveReview` (`src/runtime/TierBOcrHumanFidelityReviewCoordinator.kt:228-239`) |
| Audit pattern | The existing per-domain append-only `FileSystem*Audit` pattern (`DocumentIngestionAudit`, `EvidenceDeletionAudit`, `CaseGovernanceAudit`) — reused as a *pattern*, not as one shared implementation (Section 12; no such shared implementation exists) |
| HTTP transport template | `OwnerEvidenceHttpServer`'s own documented shape (lines 55-86): "pure HTTP transport... never a second evidence-ingress mechanism" — thin dispatch, typed-id parsing, delegate to existing operations, narrow JSON projection |

---

## 6. Explicit prohibited capabilities

The Gateway, at every phase (R0/R1/R2 and beyond, until a separate,
explicitly-reasoned governance decision states otherwise) MUST NOT:

- Access `/mnt/parker-data`, Evidence Custodian filesystem paths, derivative
  storage paths, HFR storage paths, audit-log filesystem paths, or any Parker
  internal database/store directly.
- Access the Parker Docker socket, Parker SSH, Parker provider API
  credentials, or OpenAI external-transcription credentials.
- Expose any internal mutable runtime object, or any filesystem path, to
  Hermes. Every identifier Hermes receives is an opaque governed id
  (`EvidenceArtifactId.value`, `DerivativeGenerationId.value`, `CaseId.value`
  — all `@JvmInline value class` wrappers over an opaque string, never a path).
- Reuse the Owner UI session cookie, owner device-pairing state
  (`OwnerUiAuthentication`'s `SESSION_COOKIE`/`DEVICE_ID_COOKIE`/
  `DEVICE_CREDENTIAL_COOKIE`), or any mechanism whose semantics mean "the
  owner is directly performing this."
- Register evidence, execute acquisition, authorize egress, invoke a
  provider, record HFR, change case assignment, analyse evidence, modify
  memory, delete or replace evidence, alter provenance, alter configuration,
  or grant permissions, in R0 (Section 7).
- Record `HUMAN_REVIEWED_PASS`, `HUMAN_REVIEWED_WITH_DISCREPANCY` (the actual
  enum members — `src/interfaces/HumanFidelityReview.kt:43` — the task brief's
  "REVIEW_FAILED" does not exist under that name; see Section 13), corrections,
  or discrepancy decisions, through any Gateway phase. This is enforced today
  not only by convention but structurally:
  `TierBOcrHumanFidelityReviewCoordinator`'s `recordReview` derives its
  `HumanFidelityReviewRecordingAuthorityScope` from a constructor-fixed
  `ownerPrincipalId` (`TierBOcrHumanFidelityReviewCoordinator.kt:41, 218`), and
  `HumanFidelityReviewRecordingPermissionPolicy` independently checks the
  configured owner principal before consulting the Permission Engine at all.
  Extending HFR-write to any other principal is out of this scope lock's
  authority entirely, not merely deferred (Section 13, Section 19).
- Delete evidence, under any circumstance. `deleteEvidenceAsOwner`
  (`ParkerRuntime.kt:2456-2486`) is documented as deliberately taking **no**
  `requestingPrincipalId` parameter at all — "CDR-006 and the Phase 7 Boundary
  Clarification... require deletion specifically to be structurally, not
  merely policy-content, owner-only." No Gateway phase may construct a second
  path to `OwnerEvidenceDeletionAuthority`.
- Reassign or create cases. `CaseAssignmentCoordinator` is `internal`, fuses
  read and write against the same storage, and is itself constructed with a
  hardcoded `ownerPrincipalId` (`CaseAssignmentCoordinator.kt:38`) — there is
  no existing safe read-only subordinate projection to hand to Hermes
  (Section 7, item 9).
- Enable production Local OCR. `ParkerRuntimeConfig.productionLocalOcrEligible`
  is already fixed `false` in production and not settable via environment
  (commit `89d2102`) — the Gateway must not add any path around this existing
  invariant.
- Construct a provider invocation directly, bypass the acquisition router, or
  select an otherwise-ineligible capability.
- Dynamically register a new `ActionVocabulary` verb phrase, `ResourceRegistry`
  entry, or `AuthorizationPurposeId` at request time on Hermes's behalf.
  Registration happens once, at Parker composition time, in
  `ParkerRuntime.buildAndRegisterRuntimeGraph()` — never in response to an
  inbound Hermes request.
- Copy Parker evidence content into Hermes's own persistent memory (Section 15).

---

## 7. R0 read-only capability matrix

For each candidate read, the table states the existing mechanism it would
delegate to and whether that mechanism is *already* reachable by a
non-owner-hardcoded caller, or requires a narrow, additive `ParkerRuntime`
entry point first (Section 20, AG-1C/AG-1D). None of the items below write
anything.

| # | Capability | Existing mechanism | Reachable as-is by a distinct principal? |
|---|---|---|---|
| 1 | Gateway/runtime health | New, gateway-local (no Parker state) | N/A — no Parker call |
| 2 | Evidence metadata by exact `EvidenceArtifactId` | `EvidenceCustodian.retrieveManifest`/`retrieve` (`ParkerRuntime.retrieveEvidence`) | **Yes** — `retrieveEvidence(requestingPrincipalId, evidenceArtifactId)` already takes a caller-supplied principal (`ParkerRuntime.kt:2445-2454`) |
| 3 | Duplicate/source-identity query by SHA-256 | **Does not exist.** `EvidenceSourceManifestStorage` is explicit: "No listing, search, 'latest,' update, or query capability exists on this interface... a manifest is retrieved only by its own artefact's identity." No `findByHash` exists anywhere in `src/`. | **Not available.** Excluded from R0 until a new, narrow, `EvidenceCustodian`-owned capability is added as its own separate governance decision (Section 16) — this is core-platform work, not Gateway work |
| 4 | Acquisition decision/status for exact evidence | `GovernedAcquisitionOwnerWorkflow.evaluate` (`ParkerRuntime.evaluateGovernedAcquisitionAsOwner`) | **No** — `evaluate(evidenceArtifactId)` internally calls `evidenceCustodian.retrieveManifest(ownerPrincipalId, ...)` using a constructor-fixed owner principal (`GovernedAcquisitionOwnerWorkflow.kt:24, 40`); requires AG-1D (Section 20) |
| 5 | Derivative-generation discovery for exact evidence | `TierBOcrDerivativeGenerationDiscoveryCoordinator.discover` (`ParkerRuntime.discoverOcrDerivativeGenerationsAsOwner`) | **Partially** — `discover(evidenceArtifactId)` performs no internal `PermissionEngine` check of its own at all (`TierBOcrDerivativeGenerationDiscoveryCoordinator.kt:30-31`); today the Owner HTTP session cookie is the *only* gate in front of it. A Gateway route calling this directly must supply its own `PermissionEngine.evaluate` gate first (Section 11) |
| 6 | Exact derivative-generation metadata/content, only where existing governance already allows it read | `TierBOcrContentRetrievalCoordinator`/equivalent (Owner UI's `ocr-content`/`content` routes) | Same caveat as #5 — verify per-route whether an internal Permission check exists before exposing |
| 7 | Effective HFR state for an exact evidence/generation pair | `TierBOcrHumanFidelityReviewCoordinator.projectEffectiveReview` (`ParkerRuntime.projectEffectiveHumanFidelityReviewAsOwner`) | **Partially** — `projectEffectiveReview` also performs no internal `PermissionEngine` check (`TierBOcrHumanFidelityReviewCoordinator.kt:228-239`); same gating requirement as #5. Note the *real* target is the full six-field `HumanFidelityReviewTarget` (Section 13), not the informal pair named in the unit brief |
| 8 | Provenance/processing metadata for an ingestion report | Acquisition/OCR routing provenance types (`AcquisitionRoutingProvenance`, `OcrProcessingProvenance`) — narrower than, and separate from, Memory Core `Provenance` | Read-only projection only; no unified provenance store exists to query (Section 13) |
| 9 | Case/matter metadata, reusing the existing subordinate case projection | **No safe subordinate projection exists.** `CaseAssignmentCoordinator` is `internal`, fuses `listCases()`/`currentAssignment()` (read) with `createCase()`/`assign()` (write) against the same storage, and is itself owner-principal-hardcoded (`CaseAssignmentCoordinator.kt:38`) | **Excluded from R0.** The plain `caseId`/`caseName` fields Owner UI's evidence-list JSON already carries (`OwnerEvidenceHttpServer.kt:429-430`) may ride along with item #2's evidence-metadata projection; no dedicated case-read route is authorised without new, separately-scoped read-only work this document does not design |

R0 MUST NOT (restated from the unit brief, all confirmed structurally
enforceable given the above): register evidence, upload evidence, execute
acquisition, authorize egress, invoke providers, record HFR, change case
assignment, analyse evidence, modify memory, delete or replace evidence,
alter provenance, alter configuration, or grant permissions.

---

## 8. R1 candidate-source submission scope

Parker independently receives/copies the source, computes/verifies its
authoritative SHA-256, establishes `EvidenceArtifactId`, and registers the
source — via the existing, unmodified chain `EvidenceCustodian.accept` (sole
SHA-256 computation site, `DefaultEvidenceCustodian.kt`, `MessageDigest
.getInstance("SHA-256")`) → `EvidenceRegistrationCoordinator.register` →
`ParkerRuntime.submitEvidence(requestingPrincipalId, ...)`, called with
Hermes's own principal exactly as it already accepts one today
(`ParkerRuntime.kt:2409-2430`). Hermes-calculated SHA-256, if supplied, is an
independent check only; a mismatch fails closed by the existing convention
(`AuthoritativeAcquisitionSourceResolver`'s `ByteLengthMismatch`/
`DigestMismatch`/`ManifestIdentityMismatch` outcomes are the established
shape for this — Section 13).

**Hard prerequisite, not optional:** `DefaultEvidenceCustodian.accept` mints a
fresh, random `EvidenceArtifactId` (`UUID.randomUUID()`) on **every** call,
regardless of content — there is no hash-based collision detection anywhere
in Evidence Custody today (Section 7 item 3; Section 16). Submitting
identical bytes twice through the existing, unmodified path creates **two
distinct** `EvidenceArtifactId`s with identical manifests. R1 as specified
("Parker MUST independently... establish `EvidenceArtifactId`" in a way that
tolerates repeat orchestration attempts, Section 16) **cannot be delivered by
Gateway code alone**. It requires a separate, narrow, `EvidenceCustodian`-
owned addition — a source-identity-by-hash lookup — decided and authored as
its own governance unit, not invented inside the Gateway (which would
otherwise become "an alternate Evidence Custodian," the exact
architectural rule this document exists to prevent). This scope lock
identifies the need; it does not design the addition.

R1 does not grant Hermes authority over acceptance criteria, media-type
policy, or size bounds — all remain exactly as `EvidenceCustodian`/
`OwnerLocalFileIngressCoordorindator`'s existing bounds already enforce them
(e.g. the 64 MiB `MAX_SOURCE_BYTES` bound already present for owner-local
ingress).

---

## 9. R2 governed-acquisition-request scope

Hermes may request governed acquisition for an exact, already-registered
`EvidenceArtifactId`. Execution delegates to the same
`GovernedAcquisitionOwnerWorkflow`/`GovernedAcquisitionExecutionCoordinator`/
`DeterministicEvidenceAcquisitionRouter`/`AuthoritativeAcquisitionSourceResolver`
chain the Owner UI's `handleGovernedAcquisition` route
(`OwnerEvidenceHttpServer.kt:559-577`) already calls — never a second
implementation.

**Frozen requirement, not a frozen implementation.** R2 must reuse the
existing governed acquisition workflow and all existing permission, routing,
authoritative-source-resolution, external-egress, provider-readiness, and
execution machinery, unchanged — `GovernedAcquisitionOwnerWorkflow`,
`GovernedAcquisitionExecutionCoordinator`,
`DeterministicEvidenceAcquisitionRouter`,
`AuthoritativeAcquisitionSourceResolver`, and the external-transcription/
egress chain they call. This document freezes *that* requirement. It does
**not** freeze how Hermes's authenticated principal gets threaded through
that chain for correct attribution — that determination belongs to the
future R2 implementation unit (AG-1G, Section 20), informed by, but not
concluded by, this document.

`GovernedAcquisitionOwnerWorkflow` is, as of HEAD, constructed with a
constructor-fixed `ownerPrincipalId` (`GovernedAcquisitionOwnerWorkflow.kt:24`).
One candidate the future unit may find is the narrowest governance-compatible
solution: a second, principal-scoped instance of the identical, unmodified
class, composed at `ParkerRuntime`'s own composition root with Hermes's fixed
`PrincipalId` instead of the owner's, sharing every other dependency (router,
execution coordinator, evidence custodian, source resolver,
egress-authorisation source) by reference with the existing owner-composed
instance — the same class, the same internal call chain, the same
`PermissionEngine`, parameterised per-actor exactly the way
`Principal`/`ExecutionRequest` architecture (ADR-013, ADR-017) already
anticipates, and not itself a second execution pipeline. **This candidate is
permitted if AG-1G's own investigation demonstrates it is the narrowest
governance-compatible means available; it is not required by this document,
and AG-1G is free to adopt a different, equally narrow mechanism instead** —
provided that mechanism changes no acquisition semantics, alters no public
signature of the reused classes beyond what threading a caller-supplied
principal requires, and creates no second execution path.

This does NOT grant Hermes authority to: authorize external egress, change
provider acceptance/readiness, select an otherwise-ineligible capability,
enable production Local OCR (already structurally disabled — Section 6),
bypass the acquisition router, bypass permission gates, or construct a
provider invocation directly.

**External egress fail-closed contract, exact enum values confirmed:**
`ExternalEgressAuthorisation { AUTHORISED, NOT_AUTHORISED, NOT_REQUIRED }`
(`src/interfaces/EvidenceAcquisition.kt:181`);
`AcquisitionEligibilityReason.EXTERNAL_EGRESS_NOT_AUTHORISED` /
`AcquisitionNoSelectionReason.EXTERNAL_EGRESS_NOT_AUTHORISED` (lines 188,
325). When routing surfaces this reason, the Gateway's response DTO exposes
`authorizationRequired: true` and a single opaque status —
`AUTHORIZATION_REQUIRED` — and pauses. Hermes cannot satisfy this itself: the
actual authorization endpoint
(`ExternalTranscriptionOwnerAuthorizationCoordinator.authorize`) requires
`OwnerHighAuthorityVerification.verify(...)`, a high-authority owner
credential Hermes structurally never holds and the Gateway must never
proxy, request, or accept on Hermes's behalf.

---

## 10. Authentication/principal requirements

**Authentication.** A dedicated Agent Gateway credential mechanism, entirely
separate from `OwnerUiAuthentication` (`SESSION_COOKIE`, `DEVICE_ID_COOKIE`,
`DEVICE_CREDENTIAL_COOKIE`). Exact credential shape (bearer token, mTLS
client cert, or another mechanism) is Implementation-Plan-tier, not frozen
here — the one frozen constraint is that it must never be, wrap, or derive
from any Owner UI session/pairing credential.

**Principal.** One explicit, provisioned `Principal`
(`src/contracts/Principal.kt:44`) represents Hermes, conceptually
`HERMES_INGESTION_OPERATOR`:

- `principalType`: **a new, additive `PrincipalType` enum member is required.**
  `INTERNAL_AGENT` (`Principal.kt:16`) is not appropriate — its name and its
  every existing use describe cognition Parker itself runs, and reusing it
  for an external, untrusted, subordinate operator would misrepresent trust
  status in every future permission and audit record. `FUTURE_REMOTE_DEVICE`
  is the one existing enum member already reserved as "a not-yet-built
  category" — textual precedent that this kind of addition is anticipated,
  not novel. `PrincipalType` is a closed Kotlin enum (not open/stringly-typed)
  — adding a member is itself a small, additive **source** change and is
  therefore Implementation-Plan-tier, not something this Governance-only unit
  performs. Exact member name is deferred.
- `owner`: the configured owner `PrincipalId` — establishing the same
  "backed by, accountable to the owner" lineage `Principal.owner`'s own KDoc
  describes for a Plugin ("the user who installed a Plugin").
- `status`: `PrincipalStatus.CREATED` until the owner explicitly activates it
  to `ACTIVE`. **No new provisioning/revocation mechanism is needed** —
  `DefaultPermissionEngine.evaluate` already denies every non-`ACTIVE` status
  outright, before policy is even consulted (Section 17). Suspending or
  revoking Hermes's access is exactly one `PrincipalStatus` transition on this
  one record.
- `metadata`: implementation-defined; no governance content frozen here.

**Request origin.** Every `ExecutionRequest` the Gateway constructs uses the
already-existing `RequestOrigin.AGENT` or `RequestOrigin.REMOTE_INTERFACE`
(`ExecutionRequest.kt:37, 41`) — no new `RequestOrigin` member is needed.

**No impersonation.** The Gateway must never resolve a Hermes-authenticated
request to `PrincipalId(config.ownerPrincipalId)`, and must never accept an
owner session cookie, device-pairing cookie, or device credential as
Gateway-valid authentication (Section 6).

---

## 11. Permission/action/resource requirements

- **Narrowly-scoped rules only**, mirroring `ParkerRuntime`'s own existing
  policy content exactly: its own KDoc states the *entire* rule set it
  supplies today is two rules, each scoped to an exact resource id, "never a
  blanket [ResourceType] grant" (`ParkerRuntime.kt:301-327`). Every new
  `PermissionPolicyRule` the Gateway needs must follow this precedent — named
  to an exact `(PermissionAction, ResourceType)` pair and, where useful, an
  exact `proposedAction` verb phrase (`PermissionPolicyRule.proposedAction`,
  `DefaultPermissionPolicy.kt:53`) — never a coarse grant across a
  `ResourceType`.
- **A dedicated, namespaced `AuthorizationPurposeId`** (e.g. of the shape
  `<domain>.<purpose>` the registry already validates,
  `AuthorizationPurposeRegistry.kt:118-128`) distinguishes gateway-originated
  requests. This lets gateway-scoped rules coexist with owner-scoped rules
  addressing the identical `(action, resourceType)` pair without any change
  to existing owner policy — `DefaultPermissionPolicy`'s own precedence
  mechanism (a purpose-matching rule beats a coarser one; an absent/inactive
  purpose folds to "no purpose," never silently matching a
  purpose-restricted rule meant for someone else) already guarantees this
  (`DefaultPermissionPolicy.kt:116-136`, §2.4 of
  `AUTHORIZATION_PURPOSE_SCOPE_LOCK.md`).
- **New verb phrases, registered once, at composition time.** Each R0/R1/R2
  capability gets its own `ActionVocabulary` entry
  (`InMemoryActionVocabulary.register`, `ActionMapper.kt:29-41`), registered
  inside `ParkerRuntime.buildAndRegisterRuntimeGraph()` alongside the
  existing entries — never accepted or registered dynamically from an
  inbound Hermes request. Reject-on-conflict applies identically: a
  gateway verb phrase colliding with an existing mapping is rejected, never
  silently overwritten.
- **The Gateway dispatch layer, not the coordinator, is sometimes the only
  gate.** Two of the read coordinators R0 needs perform **no internal
  `PermissionEngine` check of their own** —
  `TierBOcrDerivativeGenerationDiscoveryCoordinator.discover` and
  `TierBOcrHumanFidelityReviewCoordinator.projectEffectiveReview` (Section 7,
  items 5 and 7). Today, the Owner HTTP session cookie is the only thing
  standing in front of them. Every Gateway route touching either must
  construct its own `ExecutionRequest` for Hermes's principal and call the
  shared `PermissionEngine.evaluate` **itself**, before delegating — it must
  never call these two methods on bare trust that "the caller must already
  be authorised," the same discipline `EvidenceCustodian`/
  `GovernedAcquisitionOwnerWorkflow`/`ExternalTranscriptionOwnerAuthorizationCoordinator`
  already apply internally for everything they gate themselves.
- **Resources remain opaque and pre-registered.** No new `ResourceType` is
  introduced. Where a fixed boundary resource is useful (mirroring
  `AGENT_RUNTIME_BOUNDARY_RESOURCE_ID`'s existing pattern,
  `ParkerRuntime.kt` startup sequence), it is registered once, at
  composition time, exactly the same way.

---

## 12. Audit requirements

**Governance collision discovered, disclosed here rather than silently
worked around:** the task brief's instruction to reuse "existing audit
facilities" presumes a canonical audit authority exists. It does not.
`src/interfaces/AuditService.kt` declares a generic `AuditService` interface
(`record`/`query` against `AuditRecord`/`AuditQuery`) — but **no type named
`AuditRecord`, `AuditRecordId`, or `AuditQuery` is defined anywhere in
`src/`, and no class implements `AuditService`.** It is an unimplemented
Volume-3 stub, not live production infrastructure. `ResourceType.AUDIT_LOG`
exists in the enum; no `Resource` of that type is ever registered.

The actual, live audit mechanism is domain-fragmented by design: one
bespoke, append-only, `FileSystem*Audit`-pattern record per subsystem —
`DocumentIngestionAuditRecord`/`DocumentIngestionAudit`,
`EvidenceDeletionAuditRecord`/`EvidenceDeletionAudit`,
`CaseGovernanceAuditRecord`/`CaseGovernanceAudit`,
`HumanCorrectionAuditRecord`, `HumanFidelityGovernanceAuditRecord` — each
with its own differently-named correlation-style field, no single generic
correlation id threaded uniformly across all of them. Each of these already
carries a `requestingPrincipalId`/`actorPrincipalId` field, so **once Hermes
has its own distinct `Principal` (Section 10), every existing per-domain
audit writer already attributes a Hermes-triggered evidence
admission/deletion/case event/HFR event to Hermes correctly, with zero new
audit code** — this is the single largest "free" governance win the Gateway
gets from proper principal attribution alone.

The genuine gap is at the Gateway's own boundary — authentication
success/failure, a malformed/rejected request, and the permission decision
for a call that never reaches a domain coordinator at all (e.g. a denied R0
read) have no existing writer. **Recommendation, not authorised here:** mint
one new, narrow, Parker-owned audit record — e.g.
`AgentGatewayAccessAuditRecord`/`AgentGatewayAccessAudit` — following the
exact same append-only `FileSystem*Audit` shape already used four times, at
minimum carrying: agent principal id, request/session/correlation identity
(`ExecutionRequest.correlationId`/`sessionId`, threaded through explicitly —
today's domain audits do not do this uniformly, and the Gateway must not
repeat that gap for its own boundary events), requested Parker operation
(verb phrase), exact resource/evidence target, permission decision where
applicable, execution result, and timestamp. This is additive to, never a
replacement for, the existing per-domain audits — it never re-records what
`DocumentIngestionAudit`/`EvidenceDeletionAudit`/`CaseGovernanceAudit`
already record once an operation reaches their coordinator.

---

## 13. Evidence/HFR/external-egress invariants

- **Hash authority.** `DefaultEvidenceCustodian.accept` is the one and only
  site that ever computes the authoritative SHA-256
  (`MessageDigest.getInstance("SHA-256")` over the accepted bytes) — fixed at
  acceptance, never recomputed from a later retrieval. A Hermes-supplied
  digest is compared against this, never substituted for it.
- **The real fail-closed integrity gate is `AuthoritativeAcquisitionSourceResolver`**,
  not `EvidenceCustodian.retrieve` itself (which trusts whatever bytes
  storage returns for a given id, unconditionally). Every acquisition and
  external-transcription path already routes through this resolver's
  `ByteLengthMismatch`/`DigestMismatch`/`ManifestIdentityMismatch` outcomes
  before trusting bytes; the Gateway must never construct a parallel
  verification step — it delegates to paths that already include this
  resolver (Section 8, Section 9).
- **HFR target scoping is a six-field composite, not a pair.**
  `HumanFidelityReviewTarget` (`src/interfaces/HumanFidelityReview.kt:353`)
  hashes `evidenceArtifactId, sourceSha256, preparationIdentity,
  derivativeGenerationId, derivativeGenerationSha256, derivativeContentSha256`
  — all six, into the governed `ResourceId` both
  `HumanFidelityReviewRecordingPermissionPolicy.resourceIdFor` and
  `HumanFidelityReviewExactTargetRegistrar.register` use. Any Gateway
  reference to "the (evidenceArtifactId, derivativeGenerationId) target"
  (the unit brief's own shorthand) must be understood, and implemented, as
  this full six-field target, resolved server-side exactly as
  `TierBOcrHumanFidelityReviewCoordinator.resolveTarget` already does —
  never accepted as a client-supplied shortcut.
- **HFR states, exact enum, confirmed:** `HumanFidelityReviewState { UNREVIEWED,
  HUMAN_REVIEWED_PASS, HUMAN_REVIEWED_WITH_DISCREPANCY, HUMAN_REVIEW_CONFLICT }`
  (`HumanFidelityReview.kt:43`). There is no `REVIEW_FAILED` member; any
  future document referring to one is in error.
- **HFR write is policy-locked to the owner principal, not merely
  Gateway-excluded.** `HumanFidelityReviewRecordingPermissionPolicy` checks
  the configured owner principal directly, and
  `TierBOcrHumanFidelityReviewCoordinator` is constructed with a fixed
  `ownerPrincipalId` used to derive every recorded review's identity. Review
  state for one generation can never be transferred to another generation —
  the six-field target above is exact-match only, by construction.
- **Production real OCR/transcription is always external-provider-based
  today**, confirmed by commit `89d2102` (local OCR production-disabled,
  `ParkerRuntimeConfig.productionLocalOcrEligible` fixed `false`) composed
  with HEAD `3024ecf` (external-transcription `AcquisitionAvailability`
  wired directly from `OpenAiExternalTranscriptionProviderProfile
  .acceptanceState == ACCEPTED`). The Gateway must not alter, work around,
  or add a second path to this rule.
- **Provenance is two separate, non-unified concepts.** Memory Core
  `Provenance` (`MemoryCore.createProvenance`, sole authoritative writer) and
  acquisition/OCR routing provenance (`AcquisitionRoutingProvenance`,
  `OcrProcessingProvenance`) do not share a store. The Gateway's ingestion-
  report projection (R0 item 8) reads from whichever of the two already
  serves the relevant data to Owner UI — it does not synthesize a merged view.

---

## 14. API projection rules

Mirror `OwnerEvidenceHttpServer`'s own established convention exactly: a flat
JSON object, a `"status"` field, outcome-specific fields alongside it, and an
`"error"` field for the 400/401/404/409/413/500 family — never a serialized
internal object. Concretely, for example, an acquisition-state response
exposes only:

```json
{
  "evidenceArtifactId": "...",
  "status": "...",
  "selectedCapabilityId": "...",
  "mechanism": "...",
  "reasons": ["..."],
  "authorizationRequired": false
}
```

— never an internal `AcquisitionSource`, `BoundAcquisitionCapabilityExecutor`,
or filesystem path. Every identifier crossing the boundary is the opaque
`.value` of its typed id (`EvidenceArtifactId`, `DerivativeGenerationId`,
`CaseId`), constructed and validated exactly as
`OwnerEvidenceHttpServer.parseEvidenceId`/`SAFE_ROUTE_ID` already do —
rejecting a malformed id with 400 before any operation runs, never
constructing a typed id from unvalidated input and letting a downstream
exception surface as a 500.

---

## 15. Hermes-memory restrictions

Hermes memory is operational memory only — its own record of what it asked
Parker to do and what Parker reported back, useful for its own orchestration
continuity. It is never a substitute for, or cache of, Parker state:

- Evidence content is never automatically copied into Hermes's persistent
  memory by the Gateway.
- The Gateway design does not depend on Hermes memory being accurate,
  present, or persistent for: evidence identity, provenance, current
  acquisition state, HFR state, permissions, case assignment, audit, or any
  canonical processing state. Every one of these is re-queried from Parker,
  fresh, on every call that needs it — consistent with Memory Core being the
  sole authoritative writer of its own `Provenance` records (Section 13) and
  with ARCHITECTURE_PRINCIPLES.md §5 ("Parker is a custodian, never the
  owner" — of its own state, including toward Hermes).

---

## 16. Idempotency rules

Four distinct concepts must not be conflated, per the unit brief's own
instruction, and Parker's current implementation draws the line differently
for each:

- **Source identity** (the SHA-256 of submitted bytes) — authoritative,
  computed once by `EvidenceCustodian.accept` (Section 13).
- **Evidence registration** (`EvidenceArtifactId`) — **not currently
  idempotent with respect to source identity.** `DefaultEvidenceCustodian
  .accept` mints a fresh random UUID on every call; no hash-based dedup
  exists (Section 7 item 3, Section 8). Running the same `/parker-ingest`
  command twice today, through the existing unmodified path, **will**
  silently establish a second, distinct evidence identity — the exact
  outcome the unit brief prohibits. Closing this gap requires the same
  new, `EvidenceCustodian`-owned, source-identity-by-hash capability
  Section 8 already identifies as a hard R1 prerequisite; it is out of
  this document's authority to design.
- **Acquisition attempt** — `GovernedAcquisitionExecutionCoordinator.execute`
  already re-validates the caller's `expectedCapabilityId` against a freshly
  re-routed decision before proceeding (`GovernedAcquisitionOwnerWorkflow
  .execute`, `ParkerRuntime.kt:2495-2498` KDoc: "the expected capability is
  revalidated before execution") — a stale or repeated request either
  re-executes against the current decision or is rejected as
  `StaleDecision`, never silently duplicated against a decision that no
  longer holds.
- **HFR state** — exact-match only against the six-field target (Section 13);
  irrelevant to Gateway idempotency since the Gateway never writes it.

The one existing idempotent primitive in the relevant subsystem is
`FileSystemExternalTranscriptionAuthorizationStore.createOrGet` — an
exact-match-or-fail-closed shape (`CREATE_NEW`; on conflict, compares
`sourceSha256`/`principalId`/`purpose`; exact match → `AlreadyExisted`;
any mismatch → `Conflict`, never silently reused). **This is the template**
any future source-identity-by-hash addition should follow: same-bytes,
same-scope resubmission returns the existing identity; a scope mismatch
fails closed rather than silently picking one.

---

## 17. Failure semantics

Unknown principal, action, resource, evidence id, generation id, permission,
authorization purpose, acquisition state, or authentication state never
results in broader access. This is not aspirational — it is the literal,
quoted, already-shipped behaviour the Gateway inherits by delegating to
unmodified code:

```kotlin
// DefaultPermissionEngine.evaluate
val principal = identityService.resolve(request.principalId) ?: return deniedDecision(request)
return when (principal.status) {
    PrincipalStatus.SUSPENDED, PrincipalStatus.REVOKED, PrincipalStatus.ARCHIVED, PrincipalStatus.CREATED
        -> deniedDecision(request)
    PrincipalStatus.ACTIVE -> policy.evaluate(request)
}
```

```kotlin
// DefaultPermissionPolicy.evaluate / ruleOutcomeFor
if (resolvedMappings.isEmpty()) return deniedDecision(request)          // unknown action or resource
val maximalSpecificity = applicable.maxOfOrNull(::specificity)
    ?: return DENIED to AUTOMATIC                                       // no rule matches
val rule = maximalRules.singleOrNull()
    ?: return DENIED to AUTOMATIC                                       // ambiguous rules -> deny
```

An unresolvable proposed action is `ActionMappingFailureReason.UNKNOWN_ACTION`
(Invalid, not a Permission decision — it never reaches `PermissionEngine` at
all); an unregistered `ResourceId` resolves to an empty `resourceTypes` set,
which — outside the two closed "targetless" verbs
(`memory.retrieve`/`memory.retrieve_document`) — yields zero resolved
mappings and the same `DENIED` default; an unregistered, retired, or absent
`AuthorizationPurposeId` folds to "no purpose" and falls through to the same
no-rule-matches `DENIED` default whenever no coarse rule exists either. The
Gateway adds no new failure path — it only adds new *inputs* (a new
`PrincipalType`, new verb phrases, a new `AuthorizationPurposeId`) that this
same, unmodified machinery evaluates.

Confirmed by existing unit tests (bodies not reproduced, names only):
`DefaultPermissionEngineTest` — suspended/revoked/archived/created principal
always `DENIED` without policy being consulted; unresolvable principal
`DENIED`. `DefaultPermissionPolicyTest` — unknown action `DENIED`;
unresolvable resource `DENIED`; no addressing rule `DENIED`; empty rule list
`DENIED` universally; unmatched authorization purpose `DENIED` via the
existing default; a coarse rule never resolves a request a more specific,
purpose-matching rule governs, regardless of rule-list order.

---

## 18. Network/composition recommendation

**Recommendation: a separate `AgentGatewayHttpServer` (Option A).**

`OwnerEvidenceHttpServer` structurally cannot host a second principal safely.
Its own class KDoc states the guarantee directly: *"There is no per-request
client-supplied principal anywhere in this class — every call into
[operations] resolves the owner identity... the same structural guarantee
the Compose Desktop UI already has"* (`OwnerEvidenceHttpServer.kt:73-80`).
Concretely, `isAuthorised` resolves every successful cookie/pairing check to
one hardcoded `ownerPrincipalId`
(`OwnerUiAuthentication.kt:96`) — there is no code path in this class through
which any second identity could ever be introduced without touching the
exact mechanism that today makes owner-authority confusion impossible.
Adding a second principal to this class would mean modifying that guarantee,
not building beside it.

A wholly separate class — reusing `OwnerEvidenceHttpServer`'s own template
exactly (thin HTTP dispatch, typed-id parsing rejecting malformed input
before any operation runs, delegation to already-governed
`ParkerRuntime`/coordinator methods, narrow JSON projection, no business
logic of its own) — makes accidental Owner-authority inheritance
**structurally** impossible: `AgentGatewayHttpServer` never constructs, reads,
or validates an `OwnerUiAuthentication` session/pairing cookie at all, so no
code path exists by which an authenticated Hermes request could ever resolve
to the owner principal. It binds its own address/port, uses its own
authentication mechanism (Section 10), and exposes routes conceptually under
`/agent/...` — never a permissive alias layered onto `/owner/...`. The exact
route composition (single new class vs. a package of smaller handlers) is
Implementation-Plan-tier; the constraint frozen here is the *separation*
itself, not its internal file layout.

---

## 19. Explicit non-goals

This scope lock does not authorise, and no future unit may treat as already
decided:

- A second execution pipeline, evidence-ingestion implementation, permission
  system, Evidence Custodian, HFR mechanism, external-transcription
  invocation path, provenance store, or audit authority (ARCHITECTURE_PRINCIPLES.md
  §2, §3, §9; Section 4, Section 12 above).
- Any HFR-write capability for Hermes, ever, under this document's authority
  — Section 13's policy-level lock is not a "phase 1 only" restriction; lifting
  it would be its own, separately-reasoned governance decision, unrelated to
  this Gateway's own rollout phases.
- Reactivating production Local OCR, or any path around
  `ParkerRuntimeConfig.productionLocalOcrEligible`.
- Case creation, case reassignment, or any write against
  `CaseAssignmentCoordinator`.
- Evidence deletion, under any principal, through any Gateway phase.
- A general case-read/case-list capability for Hermes (Section 7 item 9) —
  only the already-public `caseId`/`caseName` fields riding along on
  evidence metadata are in scope, and only as part of R0 item 2.
- Kotlin types, field names, exact HTTP framing, or the Gateway's internal
  file/class layout beyond the separation Section 18 freezes.
- A migration mechanism (none exists or is needed for genuinely new,
  additive routes and rules).
- Any change to `PermissionEngine`, `ExecutionRequest`, `ActionMapper`,
  `Principal`'s existing members, `EvidenceCustodian`'s existing three
  methods, or any other already-frozen core contract's public signature.

---

## 20. Implementation-unit sequence

Sequencing only — no unit below is authorised by this document; each
requires its own Owner Decision (Section 22 applies to this document; each
future unit gets its own).

- **AG-1B — External Agent Identity Foundation:**
  1. Add one new, additive `PrincipalType` enum member for an external,
     subordinate agent (Section 10) — a source change to a closed enum, not
     a new architecture.
  2. Provision one `Principal` record for Hermes, `status = CREATED`.
  3. Register one new namespaced `AuthorizationPurposeId` for gateway-
     originated requests (Section 11).
  4. No Gateway HTTP surface of any kind.
  5. No evidence or acquisition capability wired to this principal.
  6. No new `ParkerRuntime` operation.
  7. **Acceptance criterion:** a test demonstrating that the newly-provisioned
     Hermes principal, left at `CREATED` (or moved to any non-`ACTIVE`
     status), is denied for every action by the existing, unmodified
     `DefaultPermissionEngine.evaluate` status check (Section 17) — proving
     the foundation is inert until the owner explicitly activates it, with
     zero new denial logic of its own.
- **AG-1C — R0 Permission Vocabulary:**
  1. Add only the R0 read verb phrases (Section 7) to `ActionVocabulary`,
     registered once at composition time (Section 11).
  2. Add their minimal `(PermissionAction, ResourceType)` mappings — never a
     coarse grant across a `ResourceType`.
  3. Add exact, gateway-purpose-scoped `PermissionPolicyRule` entries, scoped
     by AG-1B's `AuthorizationPurposeId`.
  4. Preserve fail-closed behaviour exactly (Section 17) — no change to
     `DefaultPermissionEngine` or `DefaultPermissionPolicy` themselves.
  5. No HTTP Gateway.
  6. No evidence writes.
  7. No acquisition execution.
  8. **Acceptance criterion:** tests proving an unknown verb phrase, an
     unmatched `(action, resourceType)` pair, an inactive/unregistered
     Authorization Purpose, and any operation outside the exact R0 set all
     deny — via the same `DefaultPermissionPolicyTest`-style assertions
     already established (Section 17), never a new denial mechanism.
- **AG-1D — R0 Governed Runtime Projections:**
  1. Add only the minimum agent-attributed `ParkerRuntime` entry points
     required for R0 (an "AsAgent" family, mirroring the existing "AsOwner"
     naming convention).
  2. Every entry point constructs/uses Hermes's explicit, caller-supplied
     principal — never a hardcoded owner id.
  3. Every operation requiring a permission decision passes through the
     existing shared `PermissionEngine.evaluate` — no bypass, no second
     evaluator.
  4. Delegates to existing read coordinators (Section 7 items 5/7:
     `TierBOcrDerivativeGenerationDiscoveryCoordinator.discover`,
     `TierBOcrHumanFidelityReviewCoordinator.projectEffectiveReview`, and
     similar) only *after* that permission check succeeds — since those
     coordinators perform no internal check of their own (Section 11).
  5. Does not change any coordinator's own semantics, constructor, or
     internal logic.
  6. No HTTP server yet.
  7. No writes.
- **AG-1E — R0 Agent Gateway Transport:**
  1. Implement the separate `AgentGatewayHttpServer` (Section 18).
  2. Separate Agent Gateway authentication (Section 10) — never
     `OwnerUiAuthentication`.
  3. Narrow R0 request/response DTO projections (Section 14).
  4. Strict route/id/content validation, mirroring `OwnerEvidenceHttpServer`'s
     own `SAFE_ROUTE_ID`/typed-id-construction discipline.
  5. A gateway-boundary append-only audit record, separately authorised by
     this unit (Section 12) — additive to, never a replacement for, the
     existing per-domain audits.
  6. Routes delegate only to AG-1D's governed runtime projections — no direct
     coordinator or storage access from the HTTP layer.
  7. No writes.
- **AG-1F — R1 Candidate-Source Submission:** blocked on a separate,
  `EvidenceCustodian`-owned source-identity-by-hash capability (Section 8,
  Section 16) being designed and adopted **first**, as its own governance
  unit — not Gateway code. Once available, the Gateway calls
  `ParkerRuntime.submitEvidence(hermesPrincipalId, ...)` exactly as Owner UI
  already does.
- **AG-1G — R2 Governed-Acquisition Request:** determines the narrowest
  additive means of supplying Hermes's authenticated principal to the
  existing, unmodified governed acquisition workflow (Section 9) — which may
  or may not turn out to be a second, principal-scoped
  `GovernedAcquisitionOwnerWorkflow` instance; this document does not decide
  that for it.

---

## 21. Acceptance criteria

This scope lock is satisfied when a future Implementation Plan can be
written that:

- Builds only what Sections 4-19 freeze, in the shape those sections freeze
  it, without re-deriving or rearguing any constitutional question this
  document already answers by direct citation to HEAD.
- Treats Section 7 item 3, Section 8's hard prerequisite, and Section 12's
  audit-authority gap as explicit, named blockers to be resolved by their
  own separate governance units — never silently discovered or worked around
  mid-implementation.
- Introduces no mechanism, object, execution path, or exception this
  document's Section 5/Section 19 does not already describe as reused or
  explicitly out of scope.
- Never grants Hermes a capability Section 6 prohibits, regardless of
  convenience discovered during implementation.

---

## 22. Owner decision section

Owner Decision:
[x] ACCEPT
[ ] REVISE
[ ] REJECT

Until ACCEPT is recorded: **no Parker Agent Gateway production implementation
is authorised.**
