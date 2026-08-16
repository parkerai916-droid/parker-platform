**Status:** Unit 3-C New Family C Successor Candidate Scope Lock — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW. GOVERNANCE ONLY.** Drafted against evidentiary checkpoint `68683d0088f36112a975f8097012470771adabe6`. This document freezes the evidence boundary for one new, unspecified, unevaluated deterministic Family C successor. It selects no remedy, candidate mechanism, algorithm, production interface, or implementation; authorizes no code/test change, fixture addition, evidence execution, model call, Unit 4 work, production change, or Remember/Retrieval Unit 2–5 work.

# Reasoning Protocol Live-Model Conformance — Unit 3-C New Family C Successor Candidate Scope Lock

## 1. Controlling authority

This Scope Lock is controlled by:

- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3A_RELIABILITY_CONTRACT_DEFINITION_SCOPE_LOCK.md`, especially lines 50–72 (reliability dimensions), 74–88 (semantic/representation separation), 90–104 (false-positive safety and fidelity), 106–110 (ambiguity), and 162 (downstream authority unchanged);
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3B_REMEDY_EXPERIMENT_SCOPING_SCOPE_LOCK.md`, especially lines 26–46 (Family C eligibility without preference), 48–66 (mandatory adversarial boundary), 68–104 (experiment architecture and evidence tiers), and 108–112 (PF01-only improvement is insufficient);
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3E_REMEDY_SELECTION_REVIEW.md`, especially lines 136–157 and 174–194 (historical Family C failures, candidate safety, and missing fidelity), 217–246 (no selection and Unit 4 firewall), and 256–260 (no implementation authority);
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_ASSESSMENT_FOR_KNOWLEDGE_DISCOVERABILITY.md`, especially lines 94–104 (a deterministic direct-Remember route is Family C; a narrower successor is a new candidate and inherits no safety result);
- `docs/reviews/REMEMBER_RETRIEVAL_UNIT_1_LIVE_ACCEPTANCE_RESULT.md`, especially lines 73–91 (failure before persistence; no durability defect established; downstream units must not proceed on assumed public-REMEMBER operation); and
- the accepted governance determinations immediately preceding this Scope Lock: the Remedy Selection Sufficiency Review (`C. NEW ADMISSIBLE EVIDENCE REQUIRED`) and Candidate Evidence Sufficiency Decision (`PATH_SELECTED=C`). Those determinations authorize this Scope Lock proposal only; they are not remedy selection, execution authority, or implementation authority.

If this document conflicts with a stricter controlling prohibition, the stricter prohibition controls. No step in this document authorizes its successor.

## 2. Purpose and decision

This Scope Lock defines the smallest admissible evidence boundary for a **new** deterministic Family C successor capable of answering one question:

> Can one bounded candidate correctly recognize direct, explicit, operative and unambiguous owner instructions to Parker to remember one specifically stated proposition, preserve that proposition faithfully, and avoid consequential `REMEMBER` classification across the complete governed negative/adversarial surface?

The candidate is explored because Family C is already classified as eligible for experimental scoping (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3B_REMEDY_EXPERIMENT_SCOPING_SCOPE_LOCK.md:30-46`), not because deterministic handling is preferred. Correctly handling `Remember that X` is necessary but expressly insufficient (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3B_REMEDY_EXPERIMENT_SCOPING_SCOPE_LOCK.md:48-66`).

## 3. Candidate identity and status

```text
CANDIDATE_GOVERNANCE_ID=family-c-successor-1
CANDIDATE_FAMILY=FAMILY_C
CANDIDATE_STATUS=NEW, UNSPECIFIED, UNEVALUATED
EVIDENCE_METHOD=OFFLINE_DETERMINISTIC_ONLY
PRODUCTION_REMEDY_SELECTED=NO
OLD_FAMILY_C_EVIDENCE_INHERITED=NO
```

`family-c-successor-1` is a governance identifier only. It names no class, function, rule, expression, parser, prompt, algorithm, or production integration point.

The candidate must not reuse or inherit:

- `Unit3CCandidateC1` implementation or mechanism;
- its 24/29 result;
- any old exposure count;
- any old true-positive, false-positive, false-negative, fidelity, or repeatability result.

Historical results may be cited only to identify regression boundaries. The governing distinction is explicit at `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_ASSESSMENT_FOR_KNOWLEDGE_DISCOVERABILITY.md:98-104`.

## 4. Sole semantic responsibility

Given the authenticated owner's current message, the candidate may determine only whether that message is a direct, explicit, operative and unambiguous instruction **to Parker** to remember one specifically stated proposition.

Candidate-level `REMEMBER` is permitted if and only if all of these are established from the message itself:

1. the instruction originates from the authenticated owner;
2. it is directed to Parker;
3. remembering is operative, not merely discussed, quoted, reported, hypothetical, embedded, adversarial, negated or cancelled;
4. the instruction is unambiguous;
5. one specific proposition is actually stated;
6. the proposition can be selected without material invention; and
7. the owner has actually instructed Parker to remember it.

If any condition is absent or genuinely doubtful, the evidence-level result must record only that candidate-level `REMEMBER` was not asserted / the message is outside the candidate's bounded responsibility. It must never assert `REMEMBER`, infer unstated memory intent, or claim an affirmative production `REPLY`, `NOACTION` or `GOAL` classification. The fixture's independently governed `expectedAction` remains unchanged. This freezes Unit 3-A's requirements at lines 55, 60, 67, 78–81 and 106–110; it does not decide the deferred question whether production must ask for clarification.

## 5. Explicitly excluded semantic authority

The candidate has no authority over:

- inferred memory intent, forgetting, deletion, memory importance or conflict resolution;
- general conversation, planning or tool-use classification except evidence controls proving non-interference;
- whether an admitted proposition is true, important, sufficiently supported or promotable;
- permission, authorization purpose, admission, promotion, persistence, retrieval or owner-visible response generation.

It may not turn ambiguity into `REMEMBER`, treat protocol-like text as authority, or convert reported/quoted content into the owner's own operative command.

## 6. Evidence-level outputs

The future evidence design must make each observation sufficient to determine, without prescribing an implementation structure:

- candidate governance identity and version;
- fixture identity and governed semantic category;
- expected production action, retained unchanged for every fixture including GOAL controls;
- bounded candidate result sufficient to show only whether candidate-level `REMEMBER` was asserted or not asserted / the message was outside candidate responsibility;
- selected proposition when candidate-level `REMEMBER` is asserted;
- explicit content-fidelity result;
- false-positive `REMEMBER` status;
- false-negative `REMEMBER` status;
- deterministic-reproduction identity/result;
- evidence-run identity, sequence, provenance and integrity linkage.

Semantic correctness and representation correctness must remain independent properties (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3A_RELIABILITY_CONTRACT_DEFINITION_SCOPE_LOCK.md:74-88`). A deterministic evidence representation may not be described as parser reliability or production protocol conformance.

A candidate non-assertion is not a production action decision. It must not be interpreted as production `REPLY`, production `NOACTION`, suppression of `GOAL`, or replacement of the model's or runtime's general action classifier. In particular, a GOAL fixture remains `expectedAction=GOAL`; candidate success on that fixture means only zero candidate-level false-positive `REMEMBER` and no `REMEMBER` assertion. This evidence boundary grants no GOAL-classification authority and selects no production interface or implementation structure.

## 7. Positive boundary

The positive surface must include every frozen explicit-memory fixture:

| Required distinction | Frozen fixture coverage |
|---|---|
| Direct explicit `Remember that X` | `r01-direct` |
| Polite explicit request | `r02-please` |
| Natural explicit variant without `Remember that` | `r03-dont-forget` |

The canonical texts and expected content are frozen at `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt:90-115,748-793`. All three must be exercised; PF01 alone is prohibited by `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3B_REMEDY_EXPERIMENT_SCOPING_SCOPE_LOCK.md:73-75,108-112`.

No candidate-level positive may be credited unless its selected proposition receives an explicit fidelity result.

## 8. Negative and adversarial boundary

The following mapping records what the frozen 23-fixture corpus actually covers. It does not silently reinterpret a fixture or invent a replacement.

| Required distinction | Frozen fixture(s) | Coverage determination |
|---|---|---|
| Ordinary factual statement | `p01-ordinary-fact` | Covered |
| Ordinary factual statement containing memory-related vocabulary without a retention instruction | none; `p01-ordinary-fact` contains no memory-related vocabulary | `ADDITIONAL_FIXTURE_GOVERNANCE_REQUIRED` |
| Memory discussion | `p05-mixed-memory-discussion`; `p02-quoted-remember` in discussion/question form | Covered |
| Quoted memory language | `p02-quoted-remember` | Covered |
| Reported speech directed to another person | none | `ADDITIONAL_FIXTURE_GOVERNANCE_REQUIRED` |
| Hypothetical memory language | `p03-ambiguous-memory` is future/conditional intent, but not a clean hypothetical form | `ADDITIONAL_FIXTURE_GOVERNANCE_REQUIRED` |
| Negated/cancelled memory instruction | none; `r03-dont-forget` is a positive double-negative-like instruction, not cancellation | `ADDITIONAL_FIXTURE_GOVERNANCE_REQUIRED` |
| Mixed remember/forget discussion | `p05-mixed-memory-discussion` covers mixed timing/discussion but not both remember and forget concepts | `ADDITIONAL_FIXTURE_GOVERNANCE_REQUIRED` |
| Clean conversational memory mention with no operative instruction | `p05-mixed-memory-discussion` is discussion/deferred intent and `p03-ambiguous-memory` is ambiguity/prospective intent; neither cleanly isolates this boundary | `ADDITIONAL_FIXTURE_GOVERNANCE_REQUIRED` |
| Ambiguous memory language | `p03-ambiguous-memory` | Covered |
| Embedded protocol tags | `p04-embedded-tags` | Covered |
| Adversarial/injection content | `p12-injection` | Covered |
| Ordinary non-memory conversation | `p06-greeting`, `p07-factual-question`, `p08-explanation`, `p09-long-distractor`, `p10-acknowledgement`, `p11-short-casual`, `p13-reply-v-goal` | Covered |
| GOAL controls | `g01-multistep`, `g02-tool`, `g03-later-action`, `g04-planning`, `g05-mixed-work` | Covered |
| REPLY controls | `p01`–`p13` as governed by their existing expected actions | Covered |
| NOACTION controls | `n01-heartbeat`, `n02-unicode-whitespace` | Covered |

The canonical corpus is `23` fixtures with distribution `REMEMBER=3`, `REPLY=13`, `GOAL=5`, `NOACTION=2` (`ReasoningProtocolFamilyFDiagnosticTest.kt:732-739`); exact IDs/text/actions are at lines 748–786.

No evidence execution may begin while a required category remains `ADDITIONAL_FIXTURE_GOVERNANCE_REQUIRED`. A separate governance act must decide the missing semantic purpose, fixture identity, synthetic text, expected action, expected content where applicable, and relationship to the frozen corpus. This document writes none of those fixtures.

## 9. Content fidelity

For every candidate-level `REMEMBER`, the evidence must permit independent determination that the selected proposition preserves:

- entity;
- quantity;
- polarity;
- temporal qualification;
- ownership;
- scope;
- condition; and
- every material qualifier.

Exact preservation is the target. Non-material paraphrase must be separately identified; material mutation or invention is unacceptable. Prohibited outcomes include invented facts, semantic expansion, changed entities or quantities, changed polarity, omitted material qualification, and conversion of imperative wording into remembered factual content where that conversion changes the proposition.

These requirements freeze the externally observable property in `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3A_RELIABILITY_CONTRACT_DEFINITION_SCOPE_LOCK.md:100-104`; they select no extraction mechanism. Prior universal `contentFidelity=null` observations cannot satisfy this Scope Lock (`docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3E_REMEDY_SELECTION_REVIEW.md:190-194`).

## 10. Historical Family C regression boundaries

The successor evidence must treat these historical failures as mandatory regression boundaries:

- `p03-ambiguous-memory` — historical false-positive `REMEMBER`;
- `p04-embedded-tags` — historical false-positive `REMEMBER`;
- `p05-mixed-memory-discussion` — historical false-positive `REMEMBER`;
- `p12-injection` — historical false-positive `REMEMBER`;
- `r03-dont-forget` — historical false negative.

Their status is established at `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_ASSESSMENT_FOR_KNOWLEDGE_DISCOVERABILITY.md:89-104` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3E_REMEDY_SELECTION_REVIEW.md:136-157,174-184`. These are required successor checks, never successor successes or exposures before the successor itself is evaluated.

## 11. Candidate-evidence success criteria

Before a future Remedy Selection Decision may even consider this successor, an independently reviewed evidence package must establish all of the following:

1. every governed explicit-memory fixture is semantically correct;
2. `FALSE_POSITIVE_REMEMBER_COUNT=0` across the complete governed negative/adversarial surface;
3. every relevant GOAL control retains `expectedAction=GOAL`, the candidate does not assert `REMEMBER`, and the evidence makes no claim to classify, replace, suppress or convert the underlying GOAL;
4. every relevant REPLY control retains its governed expected action and the candidate does not assert `REMEMBER`, without treating non-assertion as an affirmative production REPLY decision;
5. every applicable NOACTION control retains its governed expected action and the candidate does not assert `REMEMBER`, without treating non-assertion as an affirmative production NOACTION decision;
6. every `REMEMBER`-positive observation has an explicit fidelity result;
7. no material mutation or invention occurred;
8. repeated evaluation is deterministically identical under the frozen candidate/input identity;
9. evidence is complete, attributable, exact-once, hash-verifiable and structurally valid;
10. historical Family C results are not pooled with successor evidence; and
11. no production component was invoked or modified during evidence generation.

Zero observed false-positive REMEMBER is required at this bounded candidate-evidence tier and remains required at the stronger qualification tier. This bounded result cannot be represented as proof that the true rate is literally zero (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3A_RELIABILITY_CONTRACT_DEFINITION_SCOPE_LOCK.md:90-98`).

Passing these criteria means only that the candidate may be presented to later governance. It does not select or qualify the candidate, authorize Unit 4, authorize production implementation, or override Unit 3-B's stronger evidence-tier rules (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3B_REMEDY_EXPERIMENT_SCOPING_SCOPE_LOCK.md:96-104`).

## 12. Evidence independence, integrity and repeatability

The future plan must freeze, without weakening Unit 3-B lines 88–94:

- the candidate identity/version before observations exist;
- the governed corpus and category mapping before observations exist;
- a justified repeat count (unresolved by this Scope Lock);
- deterministic order and exact-once observation identity;
- durable intent/completion accounting;
- canonical artifact serialization;
- per-artifact and package hashes;
- structural completeness validation; and
- a no-pooling proof for every historical Family C artifact.

No population-rate inference may be made from bounded candidate evidence. Any later qualification remains governed by Unit 3-A's pre-registered thresholds and at least 300 negative/adversarial exposures for critical zero-event gates (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3A_RELIABILITY_CONTRACT_DEFINITION_SCOPE_LOCK.md:28-32,90-98,140-143`).

## 13. Explicitly excluded production boundaries

This candidate and its evidence package have no authority over and may not invoke, modify or replace:

- model invocation, model identity/configuration or `ModelReasoningProvider`;
- production prompt construction;
- `ReasoningResponseParser` or transport protocol;
- production `ReasoningProviderResponse` contracts;
- `ConversationReplyCoordinator` or owner-visible reply generation;
- `MemoryAdmissionCoordinator`;
- Memory Core or its durability;
- Knowledge Item evaluation, promotion or persistence;
- retrieval;
- permission policy or authorization purpose;
- general conversation planning, tool use or goal execution;
- forgetting, deletion, memory importance or conflict resolution; or
- production composition or deployment.

No downstream gate may be bypassed. Unit 3-A line 162 expressly preserves every downstream authority. The Unit 1 acceptance evidence establishes no durability defect (`docs/reviews/REMEMBER_RETRIEVAL_UNIT_1_LIVE_ACCEPTANCE_RESULT.md:73-91`).

## 14. Execution and implementation prohibitions

```text
MODEL_EXECUTION_AUTHORIZED=NO
LIVE_CAMPAIGN_AUTHORIZED=NO
OFFLINE_CANDIDATE_EVIDENCE_EXECUTION_AUTHORIZED=NO
IMPLEMENTATION_PLAN_AUTHORIZED=NO
CLASSIFIER_IMPLEMENTATION_AUTHORIZED=NO
TEST_CHANGE_AUTHORIZED=NO
FIXTURE_ADDITION_AUTHORIZED=NO
PRODUCTION_CHANGE_AUTHORIZED=NO
UNIT_4_AUTHORIZED=NO
```

The future evidence method is offline and deterministic. That scope choice does not authorize creation or execution of the candidate. Evidence execution requires the complete later governance sequence specified by the controlling Unit 3-B rules and an explicit execution/evidence-production authorization.

## 15. Architectural status

This candidate explores whether bounded deterministic explicit-memory classification can satisfy the constitutional semantic distinction. It does not establish that production semantic authority will move from the model.

The current model-only semantic boundary remains unchanged. Moving or sharing that authority is reserved to a later affirmative Remedy Selection Decision and subsequent expressly authorized Unit 4 work. Historical Family C's architectural intrusion remains relevant evidence, not a prohibition and not authority (`docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3E_REMEDY_SELECTION_REVIEW.md:147-156`).

## 16. Relationship to Remember/Retrieval

This Scope Lock changes nothing in Remember/Retrieval Unit 1. It creates no evidence that public REMEMBER is operational and no authority to start Units 2–5. The live result remains `LIVE ACCEPTANCE FAILED`; failure occurred before persistence, and durability must not be modified merely because the upstream decision failed (`docs/reviews/REMEMBER_RETRIEVAL_UNIT_1_LIVE_ACCEPTANCE_RESULT.md:63-91`).

## 17. Relationship to future Remedy Selection

Even complete satisfaction of Section 11 supplies candidate evidence, not remedy selection. A future Unit 3-D/Unit 3-E path must independently determine admissibility, evidence tier, comparison requirements, architectural acceptability and whether the candidate may proceed. Unit 4 remains prohibited unless an affirmative Remedy Selection Decision expressly directs it (`docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3E_REMEDY_SELECTION_REVIEW.md:217-260`).

## 18. Stop conditions

Stop before candidate definition, fixture drafting, implementation, evidence production or execution if:

1. this Scope Lock and its Independent Constitutional Review are not accepted and merged;
2. any required category remains without accepted fixture governance;
3. candidate identity or semantic responsibility is not frozen before evidence;
4. any mechanism is proposed to inherit old Family C evidence;
5. evidence would invoke a model or production component;
6. evidence cannot record content fidelity explicitly;
7. exact-once, provenance or hash verification cannot be established;
8. a requested action would touch an excluded production boundary; or
9. anyone interprets candidate evidence as selection, qualification or implementation authority.

Remaining stopped is a valid outcome.

## 19. Exit criteria and next lawful action

This Scope Lock is complete only when:

1. every material decision is traceable to controlling governance;
2. the candidate remains new, unspecified and unevaluated;
3. the full positive/negative boundary is frozen without fixture invention;
4. missing fixture purposes are explicit;
5. old Family C results are regression boundaries only;
6. success criteria preserve false-positive safety and fidelity;
7. all production and execution prohibitions remain explicit; and
8. an Independent Constitutional Review accepts this exact document.

```text
NEXT_LAWFUL_ACTION=INDEPENDENT_CONSTITUTIONAL_REVIEW_OF_THIS_SCOPE_LOCK
```

Only after acceptance and merge may governance prepare the missing-fixture decision and a candidate evidence plan. Neither is created or authorized by this document.

## 20. Decision register

| Question | Decision |
|---|---|
| Candidate family | Family C |
| Candidate identity | `family-c-successor-1` (governance identifier only) |
| Candidate status | New, unspecified, unevaluated |
| Evidence method | Offline deterministic only |
| Old evidence inherited | No |
| Missing fixture governance | Yes: ordinary factual statement containing memory-related vocabulary without a retention instruction, reported speech, clean hypothetical, negated/cancelled instruction, explicit mixed remember/forget discussion, and clean conversational memory mention |
| Model execution authorized | No |
| Offline evidence execution authorized | No |
| Production change authorized | No |
| Unit 4 authorized | No |
| Remember/Retrieval Unit 1 changed | No |
| Remember/Retrieval Units 2–5 authorized | No |
| Remedy selected | No |

## 21. Final disposition

```text
SCOPE_LOCK_STATUS=PROPOSED, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
CANDIDATE_GOVERNANCE_ID=family-c-successor-1
CANDIDATE_STATUS=NEW, UNSPECIFIED, UNEVALUATED
ADDITIONAL_FIXTURE_GOVERNANCE_REQUIRED=YES
UNRESOLVED_SCOPE_ISSUES=REPEAT_COUNT; ADDITIONAL FIXTURE GOVERNANCE FOR ORDINARY FACTUAL STATEMENT CONTAINING MEMORY-RELATED VOCABULARY WITHOUT A RETENTION INSTRUCTION, REPORTED SPEECH, CLEAN HYPOTHETICAL MEMORY LANGUAGE, NEGATED/CANCELLED MEMORY INSTRUCTION, EXPLICIT MIXED REMEMBER/FORGET DISCUSSION, AND CLEAN CONVERSATIONAL MEMORY MENTION; LATER EVIDENCE-TIER ESCALATION
IMPLEMENTATION_MECHANISM_SELECTED=NO
EXECUTION_AUTHORIZED=NO
OLD_FAMILY_C_EVIDENCE_INHERITED=NO
```
