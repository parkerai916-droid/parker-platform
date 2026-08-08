**Status:** Governance/Boundary Reconciliation — OPTION D. `system.knowledge-memory` is not an accepted governed production Principal. Registering it would adopt an identity model that accepted Authorization Context and Authorization Purpose governance expressly identifies as ungoverned, broken, and unsuitable as consumer distinction. No Unit 4 governance correction is authorized by this determination. No Kotlin or tests are modified. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 4 Principal Registration Governance Determination

## 1. Repository evidence

The exhaustive repository search finds `system.knowledge-memory` in production only as identical constants in:

- `DefaultKnowledgeCandidateEvaluator`, which is live and supplies it to every Memory Retrieval call; and
- `DefaultKnowledgeRevisionEvaluator`, which is dormant and supplies the same value to its retrieval calls.

It is never registered by `ParkerRuntime`. The production identity stage registers only `system.parker`, `system.conversation-engine`, `system.response-composer`, `system.planner-runtime`, `system.task-manager-runtime`, and the configured owner.

This omission was already known before this Unit 4 Boundary Review. The adopted Authorization Context Contract Design §4c expressly describes the `system.*` convention as KDoc-disclosed but not Trust-Framework-governed, identifies the shared evaluator/revision value as evidence that it is not a valid per-consumer identity mechanism, and records `system.knowledge-memory` as never registered and therefore already denied at the identity layer. Its accepted Independent Constitutional Review independently confirms that finding.

The accepted Authorization Purpose Programme §§5–6 identifies the candidate evaluator's fixed `system.knowledge-memory` value as an ungoverned identity substitution and requires Authorization Purpose to retire that substitution where it is load-bearing for consumer distinction. The adopted Memory Retrieval Contract Design §22 likewise prohibits resolving Gap #54 by reverting to this ad hoc substitution.

## 2. Current governance status

`system.knowledge-memory` is **not an accepted, governed production Principal identity**. Its appearance in source establishes an implementation fact, not governance authority. Existing accepted governance affirmatively classifies the fact as broken and ungoverned.

The identifier presently attempts to represent a broad Knowledge Memory subsystem actor, not the candidate-evaluation purpose. That broad meaning is itself unstable because the same identity is reused by candidate evaluation and dormant revision evaluation. Authorization Purpose governance has settled that those acts must be distinguished by purpose and that Principal must remain the real accountable actor.

Production non-registration was not documented as an intentional implementation deferral whose later registration was already authorized. It was documented as a live defect additional to Gap #54 and as evidence against the substitution model.

## 3. PrincipalType determination

The repository cannot constitutionally assign an exact accepted `PrincipalType` to this identifier because no accepted identity-adoption artifact defines it as a Principal at all.

If a future, separately governed decision were to adopt a genuine root Knowledge Memory internal-service identity, the existing production pattern and Chapter 41 would point to the already-existing `PrincipalType.SYSTEM`; no new enum value would be needed. That inference is not present authority to classify or register this specific identifier. Treating the pattern as sufficient would contradict the accepted finding that this particular convention is ungoverned.

**Finding:** exact type for production registration is a governance gap. `SYSTEM` is the architectural candidate, not an already-authorized assignment.

## 4. Lifecycle, ownership, authentication and delegation

No accepted artifact assigns this identifier a lifecycle or ownership record. Under the existing Identity Service mechanism, any Principal capable of reaching policy must be registered at `CREATED` and transitioned to `ACTIVE`; `DefaultPermissionEngine` denies every other state. A root `SYSTEM` Principal may have `owner = null`.

Static composition-time registration would create in-memory identity state only. The current implementation adds no identity persistence. Chapter 42 separates service authentication from registration, but no local service credential or authentication mechanism is authorized here. No owner association or delegation may be invented to repair the gap.

These are mechanism facts and likely future constraints, not a lawful lifecycle assignment for `system.knowledge-memory` under current governance.

## 5. Identity recognition versus authority

As an architectural principle, registration is identity recognition, not permission. `Principal.kt`, Chapter 41, `IdentityService.md`, and Chapter 42 all state that identity/authentication and authorization are separate. `DefaultPermissionEngine` first requires a resolvable `ACTIVE` Principal, then delegates to `DefaultPermissionPolicy`.

However, in the live composed system registration is a necessary eligibility gate. Turning an unknown Principal into an active one allows its requests to reach every existing policy rule. It therefore does not itself create a rule, but it can activate authority already expressed by purpose-agnostic production rules. Governance must assess that combined effect rather than claiming registration is operationally inert.

## 6. Authority non-widening analysis

For the two Memory Core verbs, the Unit 2 exact-verb guards still deny absent, Evidence Intelligence, unregistered, retired, mismatched and wrong purposes. The Unit 4 candidate rules approve only when the exact active candidate purpose and exact governed verb/action/type match. Removing those candidate rules returns both verbs to denial. Registration alone cannot defeat those guards.

Nevertheless, the current production policy also contains purpose-agnostic coarse approvals, including `READ`/`MEMORY`, `READ`/`DOCUMENT`, `WRITE`/`MEMORY`, `WRITE`/`DOCUMENT`, `DELETE`/`DOCUMENT`, `EXECUTE`/`DOCUMENT`, `EXECUTE`/`AGENT`, and `NOTIFY`/`TOOL`. Policy rules do not discriminate by Principal. An active `system.knowledge-memory` request matching one of those independently resolvable actions/resources could therefore reach an approval that the same unregistered identity currently cannot reach.

Consequently, production registration cannot be proven to create no unrelated authorization solely from the current Unit 4 rule matrix. Call-site unreachability may limit present exploitation, but it is not an identity-governance substitute and is outside this correction's evidence.

Specific conclusions:

- exact candidate Memory verbs still require the candidate Purpose and rules;
- Evidence Intelligence cannot inherit candidate authority because it propagates `EvidenceAnalysisRequest.requestingPrincipalId` and the distinct Evidence Intelligence Purpose;
- owner-facing, Reasoning Context, case-work and Unit 9 Knowledge Retrieval are not directly wired to this identity by Unit 4;
- but an active identity would become generally eligible to be evaluated against existing coarse production approvals, so global non-widening is not proven.

## 7. Evidence Intelligence identity relationship

`EvidenceIntelligenceInputResolver` does not use `system.knowledge-memory` or another fixed subsystem Principal. It propagates the real `EvidenceAnalysisRequest.requestingPrincipalId`. Its purpose-bound retrieval view supplies `evidence-intelligence.input-resolution`, for which no approving Memory rule exists. Registering `system.knowledge-memory` would not directly change Evidence Intelligence's identity, but this fact does not cure the candidate evaluator's own substitution of a fixed system identity for the accountable Principal.

Accepted governance has already settled the model: one real accountable Principal may perform different acts under distinct Authorization Purposes. It rejected separate subsystem Principals as a replacement for that additive distinction. Root system identities remain lawful only when they genuinely represent the accountable actor, not when used to encode which consumer or purpose is calling.

## 8. Unit 4 classification

```text
D — NEW ARCHITECTURAL/CONSTITUTIONAL IDENTITY DECISION
```

Option A fails because registration is neither implicit nor already lawful under the accepted Unit 4 scope.

Option B fails because the missing line is not merely a plan omission: accepted governance expressly warns against adopting this identity substitution, no exact Principal type/lifecycle has been assigned to this identifier, and unrelated-policy non-widening cannot be proven.

Option C fails as framed because a standalone principal-registration unit would still lack the prior architectural decision about the accountable Principal carried by candidate evaluation. Sequencing registration before Unit 4 would not resolve that constitutional question.

Option D is required because completing the real path demands deciding whether candidate evaluation should continue to substitute a fixed subsystem identity or instead propagate the real accountable Principal alongside its already-bound Authorization Purpose. The latter is consistent with frozen governance but may require a separately governed consumer/carrier change; the former contradicts frozen governance unless that governance is explicitly reconsidered.

## 9. Governance correction and independent review disposition

No minimum Unit 4 Principal-registration correction is drafted because the instruction permits that correction only for option B. Creating one under option D would exceed this governance task and disguise an architectural decision as configuration.

Accordingly, no Independent Constitutional Review of a correction is applicable. The independent constitutional challenge performed for this determination rejects immediate registration on four grounds:

1. the identifier is not a governed Principal and is currently load-bearing as an identity substitution;
2. no exact accepted PrincipalType/lifecycle assignment exists for it;
3. activation exposes the identity to existing coarse policy approvals beyond the two candidate rules;
4. registration would preserve the fixed-identity substitution that accepted Authorization Purpose governance requires to be retired, while the correct accountable-principal carrier remains unresolved.

## 10. Exact next permitted action

Unit 4 implementation must remain paused. The next permissible work is a separate architecture/governance artifact that resolves candidate evaluation's accountable Principal propagation and explicitly determines whether `system.knowledge-memory` remains any kind of genuine root system identity after it is no longer used as consumer distinction.

That artifact must define the narrow production/test surface and undergo independent constitutional review before Kotlin changes resume. It must preserve one Identity Service/Permission Engine/policy path, the candidate Purpose rules, Evidence Intelligence denial, and every Unit 2 fail-closed guard.
