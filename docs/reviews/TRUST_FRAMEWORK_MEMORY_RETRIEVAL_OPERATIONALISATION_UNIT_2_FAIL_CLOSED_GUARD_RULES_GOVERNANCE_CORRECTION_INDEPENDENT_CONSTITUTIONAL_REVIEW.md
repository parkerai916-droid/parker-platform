**Status:** Genuine Independent Constitutional Review of the Unit 2 Fail-Closed Guard Rules governance correction. Governance review only. The accepted Scope Lock, live Unit 1 policy mechanism and production policy were read independently; the amended Implementation Plan was treated as a proposition to challenge. No Kotlin or tests are modified or authorised by this review. Unit 2 implementation has not begun. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 2 Fail-Closed Guard Rules Governance Correction — Independent Constitutional Review

## 1. Baseline and defect confirmation

Baseline: `main` at `700f422` (`feat: implement memory retrieval operationalisation unit 1`), with a clean working tree before this governance correction.

Direct production inspection confirms two existing coarse rules:

- `READ` / `MEMORY` → `APPROVED` / `AUTOMATIC`;
- `READ` / `DOCUMENT` → `APPROVED` / `AUTOMATIC`.

Unit 1's accepted policy selects an exact-verb rule over a coarse rule but lawfully uses a sole coarse rule where no more-specific rule exists. Therefore registering and deriving the two Memory verbs without guards would make the existing approvals applicable. The original Unit 2 plan's assertion that “no rule applies” was false. The Planning Review correctly stopped before implementation.

## 2. Challenge: are the guards the minimum necessary correction?

Yes. One guard is required for each newly resolvable verb/action/type pair. Removing either leaves that verb exposed to its corresponding coarse approval. Adding a Purpose dimension to a guard would not protect absent-purpose Unit 2 requests. Adding caller logic, fake resources or a second engine would be broader and unconstitutional. Changing Unit 1's global coarse fallback would regress accepted policy semantics.

The exact verb-only denials are therefore the smallest correction that operates through the already-accepted mechanism and production surface.

**Finding:** minimum necessary correction.

## 3. Challenge: does the correction alter accepted architecture?

No. It adds policy data to `ParkerRuntime`, the existing composition owner, using the `PermissionPolicyRule.proposedAction` mechanism accepted and implemented in Unit 1. It adds no new type, matching dimension, engine, registry, resource representation or authorization path.

**Finding:** architecture and Unit 1 remain unchanged.

## 4. Challenge: is coarse-rule backward compatibility preserved?

Yes. The existing coarse approvals remain present and continue governing their original acts where no more-specific rule applies. The guards affect only the two exact newly registered Memory verb phrases. This is the purpose of optional verb discrimination: independently governed verbs sharing action/type pairs can receive distinct outcomes without rewriting the coarse rules.

**Finding:** coarse behavior is preserved outside the exact guarded verbs.

## 5. Challenge: do the guards outrank coarse approvals deterministically?

Yes. Each guard matches the same action/type and additionally matches the exact proposed action, giving specificity one versus the coarse rule's zero. Unit 1 selects the unique maximal rule independent of list order. The guard outcome is `DENIED`; registration, derivation and Purpose eligibility cannot turn it into approval.

**Finding:** both coarse collision paths are closed deterministically.

## 6. Challenge: can Unit 4 lawfully outrank the guards?

Yes. Each accepted Unit 4 candidate rule matches both exact verb and active `knowledge-memory.candidate-evaluation` Purpose, giving specificity two. It is therefore more specific than the verb-only guard and may govern only when that exact Purpose is active and propagated. This is not a hidden exception: it is the precedence model frozen by the Scope Lock and implemented in Unit 1.

Absent, unregistered, retired, mismatched and Evidence Intelligence Purposes cannot select the Unit 4 rule, leaving the guard as the unique maximal applicable rule.

**Finding:** future candidate authority remains narrow and constitutionally ordered.

## 7. Challenge: does Evidence Intelligence remain denied?

Yes. Unit 2 propagates no Purpose. Even after Unit 3 eventually propagates `evidence-intelligence.input-resolution`, no Purpose-plus-verb approval exists for it. The verb-only guard therefore continues to govern. Registration of its Purpose remains eligibility data, not authority.

**Finding:** Evidence Intelligence remains fail-closed before, during and after Unit 2.

## 8. Challenge: does Unit 2 remain inert?

Yes. Its only policy additions are explicit denials. It registers vocabulary, closed derivations and Purpose values but supplies no consumer Purpose and no approving rule. Candidate evaluation and Evidence Intelligence continue to send absent Purpose and meet the verb-specific guards.

**Finding:** Unit 2 creates resolution/configuration but no retrieval authority.

## 9. Challenge: are Unit 3 or Unit 4 implemented prematurely?

No. The amendment expressly prohibits consumer views, propagation and Purpose-specific rules. It explains Unit 4 precedence only to prove the guards do not obstruct the already-accepted later policy; it does not authorize or implement that policy now.

**Finding:** programme boundaries remain intact.

## 10. Constitutional verdict

```text
ACCEPTED
```

No further corrective action is required before explicit acceptance of this governance correction. Acceptance would permit Unit 2 Planning Review to restart under the amended Plan; it would not itself implement Unit 2 or authorize Unit 3 or Unit 4.
