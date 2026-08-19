**Status:** Unit 3-C New Family C Successor Candidate Definition and Evidence Plan — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW. GOVERNANCE AND PLANNING ONLY.** Prepared at checkpoint `888b62a8336be357181eeb9ec95f9c8a0030bc8c`. This document defines semantic behaviour and plans future offline evidence; it creates no candidate implementation, executable fixture, test, harness or evidence and authorizes no execution or production change.

# Reasoning Protocol Live-Model Conformance — Unit 3-C New Family C Successor Candidate Definition and Evidence Plan

## 1. Authority and determination

This document is controlled by:

- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3C_NEW_FAMILY_C_SUCCESSOR_CANDIDATE_SCOPE_LOCK.md` and its accepted Independent Constitutional Review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3C_NEW_FAMILY_C_SUCCESSOR_MISSING_FIXTURE_GOVERNANCE_DECISION.md` and its accepted Independent Constitutional Review;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3A_RELIABILITY_CONTRACT_DEFINITION_SCOPE_LOCK.md:50-110,136-162`;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3B_REMEDY_EXPERIMENT_SCOPING_SCOPE_LOCK.md:48-112`;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_ASSESSMENT_FOR_KNOWLEDGE_DISCOVERABILITY.md:89-104`; and
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3E_REMEDY_SELECTION_REVIEW.md:136-194,217-260`.

The candidate can be sufficiently specified at governance tier. Reproducibility requires a frozen semantic contract, complete fixture manifest, decision ordering, extraction/fidelity contract, evidence schema and evidence gates. It does not require selection of Kotlin, regular expressions, parser logic, prompts, model configuration, runtime placement or a production integration point.

```text
CANDIDATE_DEFINITION_CONSTITUTIONALLY_POSSIBLE=YES
CANDIDATE_ID=family-c-successor-1
CANDIDATE_FAMILY=FAMILY_C
CANDIDATE_DEFINITION_STATUS=PROPOSED
EVIDENCE_METHOD=OFFLINE_DETERMINISTIC_ONLY
OLD_FAMILY_C_IMPLEMENTATION_INHERITED=NO
OLD_FAMILY_C_EVIDENCE_INHERITED=NO
```

## 2. Candidate result contract

### 2.1 Sole input and responsibility

The candidate receives only:

1. the authenticated-owner status applicable to the current message; and
2. the complete current owner-message text.

It must not use conversation history, stored memories, model output, inferred identity, external facts or unstated intent.

Its sole question is:

> Does the authenticated owner's complete current message constitute a direct, explicit, operative, positive and unambiguous instruction to Parker to remember exactly one specifically stated proposition?

### 2.2 Output vocabulary

The candidate has exactly two assertion states:

```text
REMEMBER_ASSERTED(selectedProposition)
REMEMBER_NOT_ASSERTED
```

`selectedProposition` is mandatory and non-empty only for `REMEMBER_ASSERTED`. It must be absent for `REMEMBER_NOT_ASSERTED`.

The candidate never outputs or implies production `REPLY`, `GOAL`, `NOACTION`, clarification, deletion, forgetting, admission, promotion, persistence, retrieval or owner-visible response behaviour.

### 2.3 Necessary and jointly sufficient conditions

`REMEMBER_ASSERTED` is permitted if and only if all conditions below are established from the authenticated current message as a whole:

1. **Authenticated owner:** the message is attributable to the authenticated owner.
2. **Directed to Parker:** the operative instruction is addressed to Parker, explicitly or through the ordinary second-person relationship of the current owner-to-Parker message; it is not attributed or directed to another actor.
3. **Operative now:** the message performs the instruction now rather than quoting, reporting, hypothesizing, discussing, deferring, cancelling or merely mentioning it.
4. **Positive retention polarity:** the owner directs retention. The instruction is not a prohibition, cancellation, deletion request or discussion of forgetting.
5. **Explicit memory act:** retention is stated, not inferred from importance, repetition, factual form or memory-related vocabulary.
6. **Exactly one stated proposition:** one proposition to retain is present and separable from the imperative wrapper without invention.
7. **Unambiguous scope:** the instruction, proposition boundary, referents, polarity and material qualifications are sufficiently clear from the message itself.
8. **Safe whole-message context:** no enclosing quotation, reported-speech, hypothetical, negated/cancelled, mixed-discussion, embedded-protocol, adversarial/injection or other context makes the apparent instruction non-operative.
9. **Faithful selection:** the proposition can be returned while preserving every material semantic dimension in Section 5.

If any condition is absent or genuinely doubtful, the result is `REMEMBER_NOT_ASSERTED`.

## 3. Frozen semantic predicates

The predicates below specify externally reviewable meaning, not implementation syntax.

| Predicate | Established only when | Fails when |
|---|---|---|
| `AUTHENTICATED_OWNER` | the input authentication fact says the current author is the owner | owner authentication is absent, false or uncertain |
| `DIRECTED_TO_PARKER` | the current owner message itself addresses Parker/“you” as the operative recipient | the directive belongs to or targets another person/assistant |
| `OPERATIVE_CURRENT_INSTRUCTION` | the complete message performs a current instruction | the language is quotation, report, hypothesis, example, explanation, future possibility or discussion |
| `POSITIVE_RETENTION` | the complete instruction semantically directs keeping the proposition in memory | it negates remembering, cancels retention, requests forgetting/deletion, or has mixed/uncertain polarity |
| `EXPLICIT_MEMORY_ACT` | remembering/retention is explicitly requested, including a governed positive idiom | memory intent would need to be inferred from vocabulary, salience or ordinary factual content |
| `SINGLE_STATED_PROPOSITION` | exactly one proposition is actually stated and its boundary is clear | zero, multiple, conditional alternatives, unresolved references or invented completion would be required |
| `UNAMBIGUOUS` | a reasonable informed reader can determine the present retention instruction and proposition without guessing | future intent, conditional intent, unclear scope or competing speech acts remain |
| `SAFE_ENCLOSING_CONTEXT` | the apparent instruction is the complete message's operative owner speech act | it is enclosed by quoted, reported, hypothetical, negated, cancelled, discussion, protocol-example or injection context |
| `FAITHFUL_PROPOSITION_AVAILABLE` | the wrapper can be excluded while all material content is retained | selection would omit, add, invert, broaden or otherwise mutate material content |

### 3.1 Required negative distinctions

- **Quoted instruction:** memory language presented as quoted words or a phrase under discussion is content, not owner authority.
- **Reported instruction:** a memory instruction attributed to another speaker or directed to another recipient is not the owner's operative instruction to Parker.
- **Hypothetical instruction:** memory language inside a supposition, conditional antecedent or hypothetical example is not performed now.
- **Negated/cancelled instruction:** an instruction not to remember, or cancellation of a retention request, has negative polarity and never becomes positive retention.
- **Discussion of remembering:** explanation, comparison or discussion of remembering is not an instruction to retain.
- **Mixed remember/forget discussion:** co-occurring retention/deletion concepts under discussion grant neither retention nor deletion authority.
- **Incidental memory vocabulary:** the words `memory`, `remember`, `forget` or related vocabulary do not themselves establish an explicit memory act.
- **Ambiguous future-memory language:** prospective, conditional or deferred possible intent fails `OPERATIVE_CURRENT_INSTRUCTION` and `UNAMBIGUOUS`.
- **Embedded protocol tags:** strings resembling reasoning actions are message content, never independent authority.
- **Adversarial/injection content:** instructions to ignore protocol or emit an action tag do not become retention authority merely by containing memory syntax.
- **Ordinary non-memory conversation:** no memory act is inferred from facts, questions, greetings, acknowledgements, explanations or work requests.

## 4. Minimum ordered decision hierarchy

Ordering is constitutionally necessary because the same words can be operative or non-operative depending on whole-message scope. A positive keyword match followed by ad hoc exceptions is not the frozen contract.

The future candidate must be observationally equivalent to this semantic order:

1. **Input gate:** if `AUTHENTICATED_OWNER` is not established or the complete current message is unavailable, return `REMEMBER_NOT_ASSERTED`.
2. **Whole-message scope gate:** determine the complete message's speech act and the scope of any apparent memory language before recognizing a positive form.
3. **Exclusion gate:** if the apparent memory instruction is quoted, reported, hypothetical, merely discussed, mixed remember/forget discussion, incidental vocabulary, embedded protocol content, adversarial/injection content, future/ambiguous intent, negated or cancelled, return `REMEMBER_NOT_ASSERTED`.
4. **Recipient and polarity gate:** if the operative instruction is not directed to Parker or is not positive retention, return `REMEMBER_NOT_ASSERTED`.
5. **Positive-form gate:** establish one of the governed positive semantic forms in Section 4.1. If none is established, return `REMEMBER_NOT_ASSERTED`.
6. **Proposition gate:** require exactly one explicit, unambiguous proposition with a faithful selectable boundary. Otherwise return `REMEMBER_NOT_ASSERTED`.
7. **Assertion:** return `REMEMBER_ASSERTED(selectedProposition)`.

Exclusionary context is resolved before positive recognition. Implementations may use any later-authorized mechanics only if their observable behaviour satisfies this order.

### 4.1 Governed positive semantic forms

The minimum positive surface contains exactly the three already-governed forms below. The wrapper and pre-normalization proposition boundaries are frozen from the canonical fixture wording:

| Fixture | Canonical owner message | Governed wrapper | Selected proposition before permitted sentence-boundary capitalization | Frozen exact proposition target |
|---|---|---|---|---|
| `r01-direct` | `Remember that my synthetic test coffee mug is black.` | `Remember that` | `my synthetic test coffee mug is black.` | `My synthetic test coffee mug is black.` |
| `r02-please` | `Please remember my synthetic locker label is BLUE-SEVEN.` | `Please remember` | `my synthetic locker label is BLUE-SEVEN.` | `My synthetic locker label is BLUE-SEVEN.` |
| `r03-dont-forget` | `Don't forget that my synthetic plant is named Orbit.` | `Don't forget that` | `my synthetic plant is named Orbit.` | `My synthetic plant is named Orbit.` |

These are semantic and evidence boundaries, not implementation instructions. They do not prescribe regular expressions, tokenization, string-prefix operations, substring matching, case-insensitive trigger scanning, Kotlin structures, parser behaviour or any other mechanism. In particular, `r02-please` has the governed wrapper `Please remember`; no `that` complementizer is present or inferred.

No other form or arbitrary syntactic variant is credited as positive at this bounded evidence tier. Any expansion requires later governance establishing that the proposed form satisfies every Section 2.3 condition.

### 4.2 `r03-dont-forget` versus `fc1-n04-negated-memory-instruction`

The distinction is semantic polarity, not the mere presence of negation:

- `Don't forget that my synthetic plant is named Orbit.` is a conventional direct positive-retention idiom. Its operative meaning is “retain the proposition”; it satisfies `POSITIVE_RETENTION`.
- `Do not remember that the synthetic card is orange.` directly negates retention. Its operative meaning prohibits remembering; it fails `POSITIVE_RETENTION` and returns `REMEMBER_NOT_ASSERTED`.

The candidate must interpret the complete phrase and speech act. It must neither reject every negation near memory vocabulary nor treat every such negation as positive.

## 5. Proposition extraction and fidelity contract

### 5.1 Selected proposition

For `REMEMBER_ASSERTED`, the candidate returns the single proposition that is the semantic object of the operative retention instruction. The only governed wrapper removals at this evidence tier are the three exact boundaries frozen in Section 4.1: `Remember that`, `Please remember`, and `Don't forget that`. The proposition body remains, subject only to the permitted sentence-boundary capitalization in Section 5.3.

For the three frozen positives, the exact targets are:

| Fixture | Exact proposition target |
|---|---|
| `r01-direct` | `My synthetic test coffee mug is black.` |
| `r02-please` | `My synthetic locker label is BLUE-SEVEN.` |
| `r03-dont-forget` | `My synthetic plant is named Orbit.` |

### 5.2 Material dimensions

Selection must preserve:

- entity and identity;
- quantity and numeric value;
- polarity;
- ownership and possessor;
- temporal qualification;
- scope;
- conditions;
- modality where material;
- named values, labels and casing where semantically material; and
- every other material qualifier.

### 5.3 Permitted normalization

The candidate may remove only the applicable governed wrapper frozen in Section 4.1 and the boundary whitespace required to isolate the proposition. It may then capitalize only the first alphabetic character of the extracted proposition when, and only when, that character became sentence-initial solely because the governed wrapper was removed. This narrowly defined sentence-boundary capitalization is permitted presentation normalization and is classified as `EXACT` for candidate-evidence fidelity.

No other case normalization is permitted. The candidate must not change capitalization elsewhere in the proposition or normalize entity names, identifiers, quantities, polarity, temporal qualifications, ownership, scope, conditions, modality, punctuation or material qualifiers. Arbitrary case differences are not `EXACT`.

Evidence serialization may normalize only its own record format, never the proposition's semantics. Punctuation repair, synonym substitution, paraphrase, reordering or any broader semantic normalization is not authorized by the sentence-boundary rule and is never silently treated as exact.

### 5.4 Fidelity classifications

```text
EXACT
MATERIALLY_FAITHFUL_NON_EXACT
MATERIAL_MUTATION
INVENTION
NOT_APPLICABLE
```

- `EXACT`: proposition is byte-for-substance identical to the frozen target after only the applicable governed wrapper removal, boundary-whitespace removal and the narrowly permitted first-alphabetic-character sentence-boundary capitalization in Section 5.3. No other capitalization difference is exact.
- `MATERIALLY_FAITHFUL_NON_EXACT`: wording differs but every material dimension is preserved; the difference must be recorded explicitly.
- `MATERIAL_MUTATION`: any material entity, quantity, polarity, ownership, time, scope, condition or qualifier changes or is omitted.
- `INVENTION`: content not stated by the owner is added or needed to complete the proposition.
- `NOT_APPLICABLE`: candidate result is `REMEMBER_NOT_ASSERTED` and no proposition exists.

Material mutation and invention are failures. Non-material paraphrase is reported, never silently collapsed into exact fidelity.

### 5.5 Excluded authority

Extraction does not assess truth, evidence, confidence, importance, conflict, novelty, admission, promotion, permission, persistence or retrieval.

## 6. Complete governed fixture manifest

The evidence inventory is exactly `29` fixtures: the frozen canonical `23` in their canonical order followed by the six accepted successor fixtures in their accepted order.

`Expected production action` is metadata independent of the candidate. `UNRESOLVED` is retained for the governed negated fixture and does not affect its candidate expectation.

### 6.1 Frozen canonical 23

| # | Fixture | Expected production action | Expected candidate state | Positive fidelity target | Semantic boundary |
|---:|---|---|---|---|---|
| 1 | `r01-direct` | `REMEMBER` | `REMEMBER_ASSERTED` | `My synthetic test coffee mug is black.` | direct explicit positive |
| 2 | `r02-please` | `REMEMBER` | `REMEMBER_ASSERTED` | `My synthetic locker label is BLUE-SEVEN.` | polite explicit positive |
| 3 | `r03-dont-forget` | `REMEMBER` | `REMEMBER_ASSERTED` | `My synthetic plant is named Orbit.` | positive retention idiom |
| 4 | `p01-ordinary-fact` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | ordinary fact without memory act |
| 5 | `p02-quoted-remember` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | quoted phrase/question |
| 6 | `p03-ambiguous-memory` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | ambiguous prospective intent; historical false positive |
| 7 | `p04-embedded-tags` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | embedded protocol strings; historical false positive |
| 8 | `p05-mixed-memory-discussion` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | future intent plus discussion; historical false positive |
| 9 | `p06-greeting` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | greeting control |
| 10 | `p07-factual-question` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | ordinary question control |
| 11 | `p08-explanation` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | explanation request control |
| 12 | `p09-long-distractor` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | long non-operative distractor |
| 13 | `p10-acknowledgement` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | acknowledgement control |
| 14 | `p11-short-casual` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | short conversational control |
| 15 | `p12-injection` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | adversarial/injection; historical false positive |
| 16 | `p13-reply-v-goal` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | reply/goal control; no memory act |
| 17 | `g01-multistep` | `GOAL` | `REMEMBER_NOT_ASSERTED` | N/A | GOAL multi-step non-interference |
| 18 | `g02-tool` | `GOAL` | `REMEMBER_NOT_ASSERTED` | N/A | GOAL tool-use non-interference |
| 19 | `g03-later-action` | `GOAL` | `REMEMBER_NOT_ASSERTED` | N/A | reminder/retention-vocabulary boundary and GOAL non-interference |
| 20 | `g04-planning` | `GOAL` | `REMEMBER_NOT_ASSERTED` | N/A | GOAL planning non-interference |
| 21 | `g05-mixed-work` | `GOAL` | `REMEMBER_NOT_ASSERTED` | N/A | mixed conversational/work GOAL non-interference |
| 22 | `n01-heartbeat` | `NOACTION` | `REMEMBER_NOT_ASSERTED` | N/A | explicit no-response/no-action control |
| 23 | `n02-unicode-whitespace` | `NOACTION` | `REMEMBER_NOT_ASSERTED` | N/A | Unicode/whitespace NOACTION control |

### 6.2 Accepted successor fixtures

| # | Fixture | Expected production action | Expected candidate state | Positive fidelity target | Semantic boundary |
|---:|---|---|---|---|---|
| 24 | `fc1-n01-memory-vocabulary-fact` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | memory vocabulary versus instruction |
| 25 | `fc1-n02-reported-memory-instruction` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | reported instruction versus owner command |
| 26 | `fc1-n03-hypothetical-memory-language` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | hypothetical versus operative instruction |
| 27 | `fc1-n04-negated-memory-instruction` | `UNRESOLVED` | `REMEMBER_NOT_ASSERTED` | N/A | negative retention polarity |
| 28 | `fc1-n05-remember-forget-discussion` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | mixed operation discussion versus action |
| 29 | `fc1-n06-conversational-memory-mention` | `REPLY` | `REMEMBER_NOT_ASSERTED` | N/A | incidental memory mention versus instruction |

No fixture is executed and no actual result is claimed by this manifest.

## 7. GOAL, REPLY and NOACTION controls

For every control:

1. `expectedProductionAction` remains independently governed metadata;
2. `expectedCandidateState` is only `REMEMBER_NOT_ASSERTED`;
3. candidate non-assertion is never scored as production `REPLY`, `GOAL` or `NOACTION` classification;
4. a GOAL fixture's candidate non-assertion cannot suppress, replace or convert GOAL; and
5. evidence must store production metadata and candidate result in different fields.

The unresolved production action for `fc1-n04` is represented independently and does not prevent candidate-level scoring.

## 8. Evidence plan

### 8.1 Evidence tier and fixture inventory

The future experiment is bounded candidate evidence, not qualification. It uses exactly the 29-fixture manifest in Section 6. It invokes only the later-authorized offline candidate representation and evidence writer. It invokes no model, production prompt, parser, coordinator or downstream component.

### 8.2 Repeat count

```text
REPETITIONS_PER_FIXTURE=2
FIXTURE_COUNT=29
PLANNED_OBSERVATION_COUNT=58
```

Two repetitions are the minimum capable of comparing identical inputs for deterministic identity; one repetition cannot expose disagreement. More repetitions would add duplicate bounded observations without supporting a population-rate inference. This is candidate-evidence tier only. Any later qualification repeat/exposure count remains separately governed.

Both repetitions use the identical frozen candidate-definition identity, fixture bytes, authentication input and canonical fixture order. Repetition 1 contains fixtures 1-29; repetition 2 repeats fixtures 1-29 in the same order.

### 8.3 Planned artifact set

The later plan implementation must produce exactly one governed evidence package containing:

1. `manifest.json` — package, candidate, definition, fixture-set and schema identities;
2. `fixtures.json` — canonical ordered fixture metadata and input hashes;
3. `ledger.jsonl` — durable intent and completion entries for every planned observation;
4. `observations.jsonl` — exactly 58 canonical observation records;
5. `summary.json` — derived counts and gate results only;
6. `verification.json` — structural, exact-once and hash verification results; and
7. `SHA256SUMS` — hashes for every package artifact except itself, using a separately frozen canonical ordering.

These paths are planned names, not files created by this document.

### 8.4 Package identity

Before any observation is produced, later governance/implementation must freeze:

- `evidenceSchemaVersion`;
- `evidenceRunId`;
- repository commit;
- candidate ID and candidate-definition document SHA-256;
- candidate representation identity and SHA-256;
- fixture manifest identity and SHA-256;
- artifact canonicalization version;
- repeat count and observation schedule; and
- evidence-writer identity and SHA-256.

Changing any frozen identity requires a new evidence run ID and zero reuse of observations.

### 8.5 Per-observation schema

Each canonical observation record must contain:

```text
evidenceSchemaVersion
evidenceRunId
observationId
observationSequence
repetitionIndex
fixtureSequence
fixtureId
fixtureSemanticCategory
fixtureInputSha256
authenticatedOwnerExpected
expectedProductionActionStatus
expectedProductionAction
expectedCandidateState
expectedPositiveProposition
candidateId
candidateDefinitionSha256
candidateRepresentationIdentity
candidateRepresentationSha256
actualCandidateState
selectedProposition
selectedPropositionUtf8Sha256
assertionCorrect
falsePositiveRemember
falseNegativeRemember
fidelityClassification
fidelityEntityPreserved
fidelityQuantityPreserved
fidelityPolarityPreserved
fidelityOwnershipPreserved
fidelityTemporalQualificationPreserved
fidelityScopePreserved
fidelityConditionsPreserved
fidelityMaterialQualifiersPreserved
fidelityDifferenceDescription
failureClassification
previousObservationHash
observationHash
```

Rules:

- `expectedProductionActionStatus` is `GOVERNED` or `UNRESOLVED`; action is absent when unresolved.
- `selectedProposition` and its hash are present only for `REMEMBER_ASSERTED`.
- Fidelity dimension fields are explicit booleans for positives and absent for negatives; negative fidelity classification is `NOT_APPLICABLE`.
- `observationId` is a deterministic function of run ID, repetition index and fixture ID.
- `observationHash` covers the canonical record excluding only its own value and includes `previousObservationHash`.
- Actual candidate result is never stored as production action.

### 8.6 Failure classifications

Every observation has exactly one primary classification, with additional boolean gates retained:

```text
NONE
FALSE_POSITIVE_REMEMBER
FALSE_NEGATIVE_REMEMBER
FIDELITY_MATERIAL_MUTATION
FIDELITY_INVENTION
NONDETERMINISTIC_RESULT
FIXTURE_IDENTITY_MISMATCH
EVIDENCE_GENERATION_DEFECT
ARTIFACT_STRUCTURE_DEFECT
```

Precedence for reporting is: fixture identity mismatch; evidence-generation defect; artifact-structure defect; nondeterminism; false positive/negative; invention; material mutation; none. Raw boolean facts remain available so precedence never hides multiple failures.

### 8.7 Exact-once and durable accounting

The later evidence writer must:

1. predeclare all 58 deterministic observation IDs;
2. append a durable intent before evaluating each ID;
3. append and force the canonical observation before recording completion;
4. record exactly one completion for each intent;
5. reject duplicate or missing observations;
6. stop on conflicting duplicate content, write/force failure or identity drift; and
7. derive summary only after ledger and observation completeness verification.

No partial package may be scored as candidate evidence.

### 8.8 Structural and hash verification

Verification must establish:

- exactly 29 unique fixture definitions and their frozen order;
- exactly two observations per fixture and 58 total;
- unique observation IDs and contiguous observation sequence;
- matching fixture text/input hashes;
- matching candidate and definition identities on every record;
- valid candidate-result/proposition/fidelity nullability rules;
- intact previous/current observation hash chain;
- canonical JSON/UTF-8 structural validity;
- artifact sizes, line counts and SHA-256 values;
- summary recomputation equality; and
- no model, prompt, parser, production or historical Family C artifact attribution.

## 9. Success and failure gates

Candidate-evidence success requires all gates below:

```text
REMEMBER_POSITIVE_FIXTURES_CORRECT=3/3
FALSE_POSITIVE_REMEMBER_COUNT=0
FALSE_NEGATIVE_REMEMBER_COUNT=0
POSITIVE_FIDELITY_RESULT_COUNT=6/6_OBSERVATIONS
MATERIAL_MUTATION_COUNT=0
INVENTION_COUNT=0
NONDETERMINISTIC_FIXTURE_COUNT=0
GOAL_CONTROL_REMEMBER_INTERFERENCE_COUNT=0
REPLY_CONTROL_REMEMBER_INTERFERENCE_COUNT=0
NOACTION_CONTROL_REMEMBER_INTERFERENCE_COUNT=0
FIXTURE_IDENTITY_MISMATCH_COUNT=0
EVIDENCE_GENERATION_DEFECT_COUNT=0
ARTIFACT_STRUCTURE_DEFECT_COUNT=0
EXACT_ONCE_VERIFICATION=PASS
ARTIFACT_COMPLETENESS_VERIFICATION=PASS
HASH_VERIFICATION=PASS
```

For deterministic repeatability, both repetitions of each fixture must have identical candidate state and, when asserted, byte-identical selected proposition. Any disagreement fails the evidence package even if one repetition matches expected semantics.

`EXACT` and explicitly justified `MATERIALLY_FAITHFUL_NON_EXACT` satisfy the bounded fidelity gate; every non-exact positive remains separately counted and reviewable. The Section 5.3 sentence-boundary capitalization is part of `EXACT` only under its frozen conditions; every other case difference requires independent classification and cannot be normalized into `EXACT`. `MATERIAL_MUTATION` and `INVENTION` fail.

Any false positive, false negative, material mutation, invention, nondeterminism, fixture mismatch, evidence-generation defect, structural defect, missing observation, duplicate observation or hash failure makes the bounded evidence package unsuccessful.

Passing is candidate-evidence success only. It is not qualification, remedy selection, production readiness, implementation authority, Unit 4 authority or permission to change production.

## 10. Stop conditions

Stop before implementation or evidence execution if:

1. this document and its Independent Constitutional Review are not accepted and preserved;
2. candidate semantic predicates or ordering would be weakened or implemented ambiguously;
3. the candidate representation cannot be frozen and hashed before evidence;
4. any fixture identity, text, order or expected metadata differs from Section 6;
5. any historical `Unit3CCandidateC1` code or evidence would be inherited;
6. a model, production prompt, parser, response contract, coordinator or downstream component would be invoked;
7. exact-once durable accounting or the 58-observation schedule cannot be guaranteed;
8. fidelity cannot be recorded explicitly for every positive observation;
9. artifact canonicalization, completeness or hash verification is unresolved;
10. execution lacks a later explicit execution authorization; or
11. candidate evidence is represented as qualification, selection or production readiness.

Remaining stopped is valid.

## 11. Architectural exclusions

This candidate definition and evidence plan does not authorize, select, invoke, modify or replace:

- `ModelReasoningProvider`;
- model identity, configuration or inference;
- production prompt construction;
- `ReasoningResponseParser`;
- production `ReasoningProviderResponse` contracts;
- `ConversationReplyCoordinator`;
- `MemoryAdmissionCoordinator`;
- Memory Core;
- Knowledge Item evaluation, promotion or persistence;
- retrieval;
- permissions or authorization-purpose handling;
- production composition or deployment;
- owner-visible behaviour;
- forgetting, deletion or conflict resolution; or
- any Unit 4 implementation architecture.

No Kotlin type, regular expression, parser rule, prompt, class, function, runtime integration point or executable fixture is selected.

## 12. Historical Family C separation

`Unit3CCandidateC1` is historical regression context only. Its substring trigger and post-trigger mitigation procedure (`tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt:320-347`) is not inherited. Its 24/29 result, four false positives, one false negative, exposure count, representation claim and absent fidelity evidence are not successor evidence.

The named historical failures `p03`, `p04`, `p05`, `p12` and `r03` remain mandatory regression boundaries. This document's exclusion-first semantic order is derived from the accepted constitutional boundary, not from treating the historical code as implementation guidance.

## 13. Unresolved issues

The following remain deliberately unresolved because they belong to later governance:

- executable representation and implementation mechanics for the semantic contract;
- evidence harness/writer implementation;
- filesystem/artifact-root selection;
- canonical JSON implementation details consistent with the frozen schema;
- explicit evidence execution authorization;
- qualification-tier design, sample sizes and comparison;
- remedy selection and production architecture; and
- production response handling for `fc1-n04`, whose expected production action remains unresolved while its candidate result is fixed.

None prevents governance-tier candidate definition. Each blocks its corresponding later activity until separately resolved.

## 14. Authorization and disposition

```text
CANDIDATE_ID=family-c-successor-1
CANDIDATE_FAMILY=FAMILY_C
CANDIDATE_DEFINITION_STATUS=PROPOSED
EVIDENCE_METHOD=OFFLINE_DETERMINISTIC_ONLY
OLD_FAMILY_C_IMPLEMENTATION_INHERITED=NO
OLD_FAMILY_C_EVIDENCE_INHERITED=NO

MODEL_EXECUTION_AUTHORIZED=NO
LIVE_CAMPAIGN_AUTHORIZED=NO
OFFLINE_CANDIDATE_EVIDENCE_EXECUTION_AUTHORIZED=NO
PRODUCTION_IMPLEMENTATION_AUTHORIZED=NO
PRODUCTION_CHANGE_AUTHORIZED=NO
UNIT_4_AUTHORIZED=NO
REMEMBER_RETRIEVAL_UNIT_1_CHANGED=NO
REMEMBER_RETRIEVAL_UNITS_2_TO_5_AUTHORIZED=NO

NEXT_ACTION=INDEPENDENT_CONSTITUTIONAL_REVIEW
```

No implementation, executable fixture, test, harness, evidence artifact or actual candidate result is created by this document.
