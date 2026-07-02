# Parker Platform — Implementation Gaps

Recorded while implementing Phase 1 (Volume 1 Core Contracts) on `feature/phase-1-core-contracts`. None of these blocked implementation outright — each was resolved with a documented, conservative choice, per Phase 7's "stop, record, recommend" process. Nothing here invents architecture; every choice below is either a direct reading of existing prose, or an explicit refusal to guess where no prose exists.

## 1. `IdentityService` has no interface — not implemented this round

**Where:** `docs/architecture/41-identity-service.md` fully describes its responsibilities in prose, and `IMPLEMENTATION_ORDER.md` Phase 1 names "Identity Service" as required. But Volume 3's own "Included Interfaces" list (`docs/specifications/volume-03-core-interfaces/VOLUME_3_INDEX.md`) does not include it, and no `src/interfaces/IdentityService.kt` stub exists.

**Why it blocks:** Writing this interface now would mean inventing its method signatures (register? resolve? updateStatus? touch-lastSeenAt?) with nothing but prose to go on, contrary to Phase 7's "do not invent missing architecture."

**Options:**
- (a) Add `IdentityService.kt` to Volume 3 first (as a documentation change, by whoever owns the spec), then implement it in a follow-up.
- (b) Explicitly descope Identity from this "Phase 1" (contracts) pass and handle it only when `IMPLEMENTATION_ORDER.md`'s Phase 1 (Resource Registry / Identity Service / Permission Engine as running systems) is actually tackled.
- (c) Authorize me to draft a proposed `IdentityService` interface as a *reviewable proposal* (clearly marked as such, not a stub with silent authority) based on Chapter 41 + the `Principal` contract.

**Recommendation:** (b) now, (c) as the concrete next step if you want Identity Service work to keep moving before (a) happens.

## 2. `ExecutionRequest`: prose spec and JSON Schema disagree

**Where:** `docs/specifications/volume-01-core-contracts/ExecutionRequest.md` lists `expiresAt` and `correlationId` under "Required Fields." `docs/schemas/ExecutionRequest.schema.json` defines neither field at all.

**Why it blocks:** ADR-019 treats the JSON Schema as the versioned, normative artifact, but the schema is missing fields the prose spec requires and that the state machine (`Created -> Expired`) and `ExecutionResult.Expired`/`Deferred` statuses depend on. Picking one source silently would hide a real inconsistency.

**Decision made:** Implemented `ExecutionRequest` with `expiresAt: Instant?` and `correlationId: String` included, following the prose spec, since both fields are load-bearing elsewhere in the same volume. This is *implementing an existing (inconsistent) spec*, not inventing one.

**Recommendation:** Update `ExecutionRequest.schema.json` to add `expiresAt` (optional, date-time) and `correlationId` (required, string), matching the prose and this implementation, and note the fix with an ADR per ADR-019's versioning rule.

## 3. Several referenced types have no defined shape anywhere

**Where:**
- `PermissionEngine.explain(decisionId): PermissionExplanation` — no spec defines `PermissionExplanation`'s fields.
- `Tool.execute(request): ToolResult` — `Tool.md` says "Tools MUST return structured results" but never specifies the structure.
- `Tool.descriptor: ToolDescriptor` — no spec defines `ToolDescriptor`'s fields.
- `ExecutionPipeline.cancel(requestId): CancellationResult` / `.status(requestId): ExecutionStatus` — neither type is specified.
- `Resource.sensitivity` — required per `Resource.md`, but no enum or value set is given anywhere (contrast with `ExecutionRequest.riskEstimate`, which *is* a concrete enum).

**Why it blocks:** These types are referenced by name in Volume 3's own interface specs, so *some* shape is needed just to make the existing stubs compile — but no source specifies what that shape should be.

**Decision made:** Added minimal, explicitly-provisional shapes for `PermissionExplanation`, `ToolResult`, `ToolDescriptor`, `CancellationResult`, and `ExecutionStatus` (see KDoc comments on each in `src/contracts/`), and typed `Resource.sensitivity` as a plain `String` rather than guessing at enum members. None of these should be treated as final.

**Recommendation:** Add these five shapes to Volume 2/3 properly (with real field lists, not inferred from usage), then reconcile the placeholders here against the result.

## 4. `ADR-005` is cited but does not exist

**Where:** `docs/specifications/volume-03-core-interfaces/EventBus.md`'s "Related" section cites "ADR-005." The `docs/adr/` directory has no `ADR-004` or `ADR-005` file — numbering jumps from `ADR-003` to `ADR-006`.

**Why it blocks:** Not blocking for this round (EventBus is out of Phase 1 scope), but will block whoever implements EventBus next if the citation is never resolved.

**Recommendation:** Either recover/write the missing ADR-004/005, or remove the dangling citation from `EventBus.md`.

## 5. `Principal` and `Resource` lifecycles have no transition rules, only a linear chain

**Where:** `Principal.md`: "Created → Active → Suspended → Revoked → Archived." `Resource.md`: "Created → Registered → Available → Updated → Archived → Deleted." Neither has an accompanying state-machine diagram (unlike `ExecutionRequest`, which has `docs/diagrams/execution-state-machine.mmd`).

**Why it blocks:** Implementing a transition validator (like `ExecutionLifecycleTransitions`) requires knowing the actual edges — e.g. can a Suspended principal return to Active, or only proceed to Revoked? Can a Resource cycle between Available and Updated, or does Updated only happen once? The prose doesn't say.

**Decision made:** Implemented `PrincipalStatus` and `ResourceLifecycleState` as plain enums with **no transition validator**, unlike `ExecutionLifecycleState`. Phase 6's test requirement ("invalid state transitions are rejected *if a state machine is specified*") is satisfied for `ExecutionRequest`, the one lifecycle that actually has a diagram behind it.

**Recommendation:** Produce `.mmd` diagrams for `Principal` and `Resource` lifecycles (matching the existing style for `ExecutionRequest`), then add validators for both, matching the pattern already in `ExecutionLifecycleTransitions`.

## 6. Volume 2 ("core schemas") markdown files are templated placeholders

**Where:** `docs/specifications/volume-02-core-schemas/*.md` (e.g. `ExecutionRequest-Schema.md`, `PermissionDecision-Schema.md`, `Event-Schema.md`) all contain identical generic boilerplate text, not real field-by-field schema detail.

**Why it doesn't block:** The real schema detail exists and is properly filled in at `docs/schemas/*.schema.json` — this implementation was written against those, not against Volume 2's prose wrappers.

**Recommendation:** Either fill in Volume 2's markdown properly (referencing the real `.schema.json` content) or remove the wrappers and point Volume 2's index directly at `docs/schemas/`, so there's one source of truth instead of two, one of which is empty.

## 7. Build system scope decision (not a spec gap, but recorded for traceability)

`src/interfaces/{Agent,AuditService,EventBus,MemoryStore,ModelManager,NotificationService,Plugin,WorldModel}.kt` are excluded from this branch's `build.gradle.kts` compilation (see that file's comment). This is a build-configuration decision, not a specification gap — those eight files reference Phase 2+ types that don't exist yet, and excluding them was necessary to get anything to compile. Nothing on disk was changed; the exclusion list is one array in one Gradle file, reversible in one edit once those phases are implemented.
