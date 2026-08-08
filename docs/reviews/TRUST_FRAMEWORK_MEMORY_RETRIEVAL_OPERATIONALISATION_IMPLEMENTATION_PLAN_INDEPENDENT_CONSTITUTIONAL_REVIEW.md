**Status:** Genuine Independent Constitutional Review of the proposed Gap #54 Memory Retrieval Operationalisation Implementation Plan. Planning review only. The accepted Scope Lock is authority; the proposed Plan is evidence to challenge. No Kotlin or tests are modified or authorised by this review. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation Implementation Plan — Independent Constitutional Review

## 1. Baseline and review method

Reviewed against the explicitly accepted Operationalisation Scope Lock and its accepted Independent Constitutional Review; the primary Gap #54 governance chain; accepted Authorization Purpose mechanism and Units 1–6; current policy, action mapping, registry, retrieval decorator, consumer and composition code; and directly relevant tests.

The review reconstructed the live dependency graph independently. It did not assume that the Plan's ordering, carrier or test claims were correct merely because the Plan asserted them.

## 2. Challenge: does implementation order remain fail-closed?

Yes. Unit 1 installs only empty-by-default policy capability. Unit 2 adds resolution and vocabulary data but no consumer Purpose or approving rule. Unit 3 propagates real active Purposes but still has no rule. Unit 4 is the first authority-bearing unit and adds only the two frozen Purpose-and-verb-specific rules. Unit 5 changes no production authority.

No state contains a coarse or global Memory retrieval approval. Each pre-Unit-4 state is required to prove `DENIED` through the real engine before review acceptance. Evidence Intelligence has no approving state at any point.

**Finding:** ordering is fail-closed; no broad-then-tighten interval exists.

## 3. Challenge: does any unit grant authority before all dimensions exist?

No. Derivation, action registration, Purpose registration and propagation are deliberately separated from policy authority and are individually non-authoritative. The candidate rules appear only after the exact verbs resolve, both values are registered and each consumer's request is distinguishable.

The Plan also retains existing Authorization Purpose semantics: an inactive Purpose cannot select its specific rule. It does not globally reinterpret existing coarse rules; instead it prohibits coarse approval for the two Memory verbs.

**Finding:** new authority first appears only when action, type, active Purpose, exact verb and policy rule can all match.

## 4. Challenge: is the carrier purpose-based or caller-based?

The selected carrier is an immutable view created explicitly at composition from a governed Purpose ID. It never discovers the caller. The view contains no class-name condition, Principal substitution, stack inspection, reflection, mutable metadata or authority logic. Its behavior is stable if either consumer class is renamed.

The values themselves state governed reasons: candidate evaluation and Evidence Intelligence input resolution. `ParkerRuntime` associates those reasons with consumer dependencies once at composition, which is exactly the accepted adoption model rather than runtime caller identity.

**Finding:** the carrier represents Purpose, not caller identity.

## 5. Challenge: is the carrier the minimum lawful Kotlin shape?

Yes. The alternatives are broader or unlawful:

- changing every `MemoryRetrieval` method to accept Purpose expands a frozen public contract and every implementation/caller;
- injecting Purpose directly into both consumers changes two production classes without need and risks leaking composition authorization concerns into runtime consumers;
- constructing two permission-filtering decorators violates the accepted single-decorator property;
- placing Purpose on Principal, metadata or thread-local context violates accepted governance.

One private delegating view surface inside the single decorator preserves both existing consumer constructors and the public interface. Although the private view implements `MemoryRetrieval`, it is not a second retrieval authority: it has no raw delegate or engine and can only re-enter the one parent decorator.

**Finding:** minimum lawful carrier selected; no interface or consumer production change is required.

## 6. Challenge: does Evidence Intelligence remain denied throughout?

Yes. It receives its distinct real Purpose in Unit 3 but no rule in any unit. Unit 4 and Unit 5 require denial against a genuinely existing record through the same production runtime, including a challenge involving otherwise applicable coarse authority. The Purpose registry's active status is treated as eligibility, never approval.

**Finding:** Evidence Intelligence non-widening is both structural and behavioral.

## 7. Challenge: does verb matching preserve existing coarse behavior?

The optional field defaults to `null`, and the targetless configuration defaults to empty. Existing policies therefore retain current construction and single-coarse-rule selection. Exact verb comparison uses `ActionMapper`'s already-produced `proposedAction`; no new parser or vocabulary authority is introduced.

The partial-order rule is faithful to the Scope Lock: Purpose-and-verb rules govern over rules omitting either dimension. Incomparable maximal rules are not resolved by arbitrary dimension priority or list order; they deny as ambiguity. This preserves existing unambiguous coarse behavior while preventing a coarse rule from defeating a more-specific Memory rule.

**Finding:** backward compatibility is preserved without treating coarse rules as implicit Memory retrieval approval.

## 8. Challenge: does ambiguity reliably deny?

Yes. Unit 1 makes ambiguity denial part of the mechanism before any production rule exists. Its tests include duplicate/equal-specificity and incomparable-maxima cases with reversed list order. Unit 4 repeats the material production-shape challenge through the composed engine.

The Plan does not use “first match,” “most permissive,” or “most restrictive” as a substitute for specificity. Where specificity does not yield one governing rule, the result is `DENIED`.

**Finding:** ambiguity is deterministic and fail-closed.

## 9. Challenge: does the Plan expand into adjacent retrieval work?

No. The production surface is three files. Knowledge discoverability, Reasoning Context, conversational retrieval, Unit 9 retrieval, semantic search and persistence redesign are explicitly excluded. Unit 5 uses an owner Remember message only to drive the existing admission/promotion pipeline; it does not add or assert later conversational recall.

**Finding:** no Knowledge Retrieval or Reasoning Context expansion.

## 10. Challenge: does the final test prove real composition rather than mocks?

Yes. Unit 5 must use `ParkerRuntime.submitOwnerMessage`, the real reasoning/admission coordinator, durable Memory Core, Knowledge Submission, candidate evaluator, shared permission-filtered decorator, real engine/policy/registry and real Knowledge persistence boundary. It must inspect persisted state rather than infer success from reply wording.

The same production composition must separately demonstrate Evidence Intelligence denial against an existing Memory record. Mock-only policy or retrieval tests remain useful in earlier units but cannot satisfy Unit 5.

**Finding:** final acceptance is tied to real composed behavior and persisted promotion.

## 11. Defect search and corrections

Two potential planning defects were tested against the presented Plan:

1. **Cross-action type borrowing:** a naive union of derived resource types for multiple proposed actions could allow one verb to resolve against another's configured type. The Plan avoids this by requiring independent per-verb mapping.
2. **Registry description overreach:** the accepted Scope Lock freezes immutable Purpose meanings, but the existing registry stores lifecycle entries keyed by ID and its contract is protected from change. The Plan lawfully documents immutable meanings beside production constants and registers the exact IDs without redesigning the registry.

Both controls are present in the reviewed Plan. No remaining defect requires correction or a Defect Confirmation Review.

## 12. Constitutional verdict

```text
ACCEPTED
```

No corrective action is required before explicit Plan acceptance.

Acceptance authorizes implementation planning completion only. Actual implementation must begin with Unit 1 and stop at its formal Completion Review and Independent Constitutional Review boundary before Unit 2 begins.
