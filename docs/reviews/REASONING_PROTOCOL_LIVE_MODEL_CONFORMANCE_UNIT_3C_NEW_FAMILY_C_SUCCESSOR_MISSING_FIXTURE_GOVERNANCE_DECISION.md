**Status:** Unit 3-C New Family C Successor Missing Fixture Governance Decision — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW. GOVERNANCE ONLY.** Prepared at checkpoint `fc0bf04f74e95d191eb426b3e6abadf3db72cb72`. This document freezes six synthetic fixture contracts and authorizes no candidate definition, implementation, test change, evidence execution, model call, production change, Unit 4 work, or Remember/Retrieval Unit 2-5 work.

# Reasoning Protocol Live-Model Conformance — Unit 3-C New Family C Successor Missing Fixture Governance Decision

## 1. Authority and purpose

This decision is controlled by:

- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3C_NEW_FAMILY_C_SUCCESSOR_CANDIDATE_SCOPE_LOCK.md:108-133,164-198,200-234,250-264`, accepted by `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3C_NEW_FAMILY_C_SUCCESSOR_CANDIDATE_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3B_REMEDY_EXPERIMENT_SCOPING_SCOPE_LOCK.md:48-66,68-104`, especially line 74's requirement that every new fixture receive its own governance review before use; and
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3A_RELIABILITY_CONTRACT_DEFINITION_SCOPE_LOCK.md:50-60,65-70,74-110,136-162`.

The accepted Scope Lock requires a separate governance act to decide each missing semantic purpose, fixture identity, synthetic text, expected action, expected content where applicable, and relationship to the frozen corpus (`Scope Lock:133`). This document is that governance decision. It creates governance metadata only, not executable fixture or test code, and it does not authorize fixture use or evidence production before its own Independent Constitutional Review and all later execution gates are satisfied.

## 2. Candidate and evidence boundary

```text
CANDIDATE_ID=family-c-successor-1
CANDIDATE_FAMILY=FAMILY_C
CANDIDATE_STATUS=NEW, UNSPECIFIED, UNEVALUATED
CANDIDATE_RESULT_VOCABULARY=REMEMBER_ASSERTED|REMEMBER_NOT_ASSERTED
```

For every negative/control fixture in this decision, the candidate may establish only whether candidate-level `REMEMBER` was asserted. `REMEMBER_NOT_ASSERTED` is not production `REPLY`, `NOACTION` or `GOAL`; it does not suppress GOAL and does not replace the general action classifier. `expectedProductionAction` is separate metadata governed independently of the candidate result.

All six fixtures are negative at the candidate boundary. None contains an operative owner instruction to Parker to remember a proposition. Therefore candidate-level `REMEMBER` is prohibited for all six. No content-fidelity target applies because none is a `REMEMBER`-positive fixture; future evidence must record fidelity as `NOT_APPLICABLE`, never as evidence that omitted positive fidelity is acceptable.

## 3. Fresh verification of the missing surface

The canonical frozen corpus is defined at `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt:90-258,732-793`. Fresh inspection confirms exactly six mandatory distinctions remain inadequately isolated:

| Missing purpose | Constitutional distinction protected | Why frozen corpus is insufficient | Unsafe Family C failure |
|---|---|---|---|
| Ordinary fact containing memory vocabulary without retention instruction | Vocabulary is not an operative instruction | `p01` is an ordinary fact but contains no memory vocabulary; `p03` and `p05` introduce ambiguity or discussion | Keyword recognition falsely asserts `REMEMBER` from the word “memory” |
| Reported memory instruction | Another person's instruction is not the authenticated owner's operative instruction to Parker | `p02` quotes a phrase inside a question but does not isolate indirect reported speech directed to another assistant | Candidate adopts reported content as the owner's command |
| Clean hypothetical memory language | A hypothetical condition is not a present operative instruction | `p03` is prospective/ambiguous intent, not a clean hypothetical | Candidate treats a hypothetical antecedent as presently operative |
| Negated/cancelled memory instruction | Explicit negation or cancellation cannot become positive `REMEMBER` | `r03` is a positive “don't forget” instruction; no frozen fixture isolates true negation/cancellation | Candidate triggers on the positive token inside a negated instruction |
| Explicit mixed remember/forget discussion | Discussion of opposite memory operations grants neither retention nor deletion authority | `p05` discusses remembering and timing but does not contain both remember and forget concepts | Candidate extracts one discussed proposition as an operative retention instruction |
| Clean conversational memory mention | Incidental memory language is not an instruction | `p03` is ambiguous/prospective and `p05` is explicit discussion; neither isolates a clean incidental mention | Candidate converts conversational vocabulary into consequential `REMEMBER` |

All six remain necessary. No conflict with the accepted Scope Lock was found.

## 4. Authority to freeze exact wording

`Scope Lock:133` expressly assigns the separate governance act responsibility for synthetic text and fixture identity. Unit 3-B line 74 requires governance review before a new fixture is used but does not require a further pre-wording tier. Accordingly, this decision may propose and freeze exact synthetic wording, subject to acceptance by its own Independent Constitutional Review.

```text
FIXTURE_TEXT_FROZEN=YES, SUBJECT_TO_ACCEPTANCE_OF_THIS_DECISION
FIXTURE_COUNT=6
EXECUTABLE_FIXTURE_CODE_CREATED=NO
```

The texts below contain no personal information, Parker operational fact, production secret, history dependency or embedded protocol syntax. Each isolates one semantic distinction. They supplement the governed evidence surface; they do not modify, renumber or reinterpret any of the frozen 23 fixtures.

## 5. Frozen fixture contracts

### 5.1 `fc1-n01-memory-vocabulary-fact`

```text
fixturePurpose=ordinary factual statement containing memory-related vocabulary without a retention instruction
semanticCategory=ORDINARY_FACT_MEMORY_VOCABULARY
exactOwnerMessage=The synthetic training card contains the word memory.
operativeOwnerInstructionToRemember=NO
candidateExpectedRememberAssertion=NO
candidateExpectedResult=REMEMBER_NOT_ASSERTED
expectedProductionAction=REPLY
contentFidelityTarget=NOT_APPLICABLE
boundaryTested=memory vocabulary versus operative memory instruction
```

`REPLY` is independently justified as ordinary conversational input by Unit 3-A lines 55 and 80. The candidate does not produce that action.

### 5.2 `fc1-n02-reported-memory-instruction`

```text
fixturePurpose=reported speech containing a memory instruction that is not the owner's operative instruction to Parker
semanticCategory=REPORTED_MEMORY_INSTRUCTION
exactOwnerMessage=A synthetic colleague told another assistant to remember that the training token is violet.
operativeOwnerInstructionToRemember=NO
candidateExpectedRememberAssertion=NO
candidateExpectedResult=REMEMBER_NOT_ASSERTED
expectedProductionAction=REPLY
contentFidelityTarget=NOT_APPLICABLE
boundaryTested=reported instruction versus authenticated owner's operative instruction to Parker
```

The indirect report is a conversational statement, not the owner's command to Parker. `REPLY` is separate production metadata under Unit 3-A lines 55 and 80.

### 5.3 `fc1-n03-hypothetical-memory-language`

```text
fixturePurpose=clean hypothetical memory language
semanticCategory=HYPOTHETICAL_MEMORY_LANGUAGE
exactOwnerMessage=Hypothetically, if I asked you to remember that the synthetic badge is copper, that would be a memory instruction.
operativeOwnerInstructionToRemember=NO
candidateExpectedRememberAssertion=NO
candidateExpectedResult=REMEMBER_NOT_ASSERTED
expectedProductionAction=REPLY
contentFidelityTarget=NOT_APPLICABLE
boundaryTested=hypothetical instruction versus present operative instruction
```

The conditional antecedent explicitly marks a hypothetical and does not perform the hypothesized request. `REPLY` is independently governed conversational metadata, not a candidate result.

### 5.4 `fc1-n04-negated-memory-instruction`

```text
fixturePurpose=negated memory instruction
semanticCategory=NEGATED_MEMORY_INSTRUCTION
exactOwnerMessage=Do not remember that the synthetic card is orange.
operativeOwnerInstructionToRemember=NO
candidateExpectedRememberAssertion=NO
candidateExpectedResult=REMEMBER_NOT_ASSERTED
expectedProductionAction=UNRESOLVED_BY_THIS_DECISION
contentFidelityTarget=NOT_APPLICABLE
boundaryTested=negated retention instruction versus positive operative retention instruction
```

The fixture prohibits retention and must not be normalized into positive `REMEMBER`. This decision grants no forgetting or deletion authority. Existing governance fixes the candidate-negative result but does not uniquely determine production `REPLY` versus `NOACTION` for this exact negated instruction, so production action metadata remains explicitly unresolved rather than invented.

### 5.5 `fc1-n05-remember-forget-discussion`

```text
fixturePurpose=explicit mixed remember/forget discussion
semanticCategory=REMEMBER_FORGET_DISCUSSION
exactOwnerMessage=In this synthetic example, remembering the blue token and forgetting the green token are opposite memory operations.
operativeOwnerInstructionToRemember=NO
candidateExpectedRememberAssertion=NO
candidateExpectedResult=REMEMBER_NOT_ASSERTED
expectedProductionAction=REPLY
contentFidelityTarget=NOT_APPLICABLE
boundaryTested=discussion of remember/forget operations versus an actual instruction to retain or delete
```

This is an explanatory statement about two operations, not an instruction to perform either. It grants no forgetting/deletion authority. `REPLY` is independently governed conversational metadata.

### 5.6 `fc1-n06-conversational-memory-mention`

```text
fixturePurpose=clean conversational memory mention with no operative instruction
semanticCategory=CONVERSATIONAL_MEMORY_MENTION
exactOwnerMessage=Memory can be useful in a long conversation.
operativeOwnerInstructionToRemember=NO
candidateExpectedRememberAssertion=NO
candidateExpectedResult=REMEMBER_NOT_ASSERTED
expectedProductionAction=REPLY
contentFidelityTarget=NOT_APPLICABLE
boundaryTested=incidental conversational memory language versus operative memory instruction
```

This is a general conversational observation. It contains no proposition the owner instructs Parker to retain. `REPLY` is independently governed metadata.

## 6. Expected-result and fidelity rules

For all six fixtures:

1. the required candidate result is `REMEMBER_NOT_ASSERTED`;
2. any `REMEMBER_ASSERTED` result is a false-positive candidate-level `REMEMBER` failure;
3. candidate non-assertion is not credited as production action classification;
4. `contentFidelityTarget=NOT_APPLICABLE` because no positive proposition is authorized for retention;
5. a candidate-selected proposition is itself evidence of the prohibited false positive, not a fidelity success; and
6. no result may infer unstated intent, grant forgetting/deletion authority, or exercise admission, promotion, persistence or retrieval.

The five `expectedProductionAction=REPLY` values are independently governed metadata for ordinary conversational statements. The negated fixture's production action remains unresolved. This does not weaken its fixed candidate-level prohibition.

## 7. Relationship to the frozen corpus

These six fixture contracts are additive governance for a future evidence corpus. They do not edit the existing `FamilyFCorpus`, change any frozen identifier, text, order, category, expected action or expected content, or retroactively reinterpret any historical observation.

```text
EXISTING_FROZEN_FIXTURES_MODIFIED=NO
HISTORICAL_EVIDENCE_RESCORING_AUTHORIZED=NO
NEW_FIXTURES_USED_IN_EVIDENCE=NO
```

The three frozen positive fixtures remain unchanged and mandatory. These six negative fixtures cannot substitute for any frozen positive, negative or control fixture.

## 8. Safety and architectural boundaries

Collectively, the six contracts test vocabulary versus instruction, report versus operative command, hypothetical versus operative command, negation versus positive retention, discussion versus action, and incidental mention versus instruction.

They do not:

- require inference of unstated owner intent;
- grant forgetting or deletion authority;
- test or invoke production admission, promotion, persistence or retrieval;
- test or invoke model or parser behavior;
- give the candidate GOAL, REPLY or NOACTION classification authority;
- alter the three positive fixtures;
- define a candidate mechanism or implementation; or
- authorize evidence production or execution.

## 9. Self-review

The proposed contracts were checked for accidental general-classifier authority, production semantics attributed to candidate non-assertion, ambiguous wording, operative positive memory instructions in negative fixtures, semantic overlap, frozen-corpus mutation, and implicit implementation or execution authority.

```text
GENERAL_ACTION_CLASSIFICATION_AUTHORITY_ADDED=NO
PRODUCTION_ACTION_INFERRED_FROM_CANDIDATE_NON_ASSERTION=NO
NEGATIVE_FIXTURE_CONTAINS_OPERATIVE_POSITIVE_REMEMBER=NO
EXISTING_FROZEN_CORPUS_MODIFIED=NO
EVIDENCE_EXECUTION_AUTHORITY_ADDED=NO
CANDIDATE_IMPLEMENTATION_AUTHORITY_ADDED=NO
SUBSTANTIVE_UNCERTAINTY=NEGATED_FIXTURE_PRODUCTION_ACTION_ONLY; EXPLICITLY UNRESOLVED
SELF_REVIEW_RESULT=PASS
```

## 10. Prohibitions and disposition

```text
CANDIDATE_ID=family-c-successor-1
CANDIDATE_IMPLEMENTATION_AUTHORIZED=NO
FIXTURE_GOVERNANCE_PURPOSES=6
FIXTURE_TEXT_FROZEN=YES, SUBJECT_TO_ACCEPTANCE_OF_THIS_DECISION
EXISTING_FROZEN_FIXTURES_MODIFIED=NO
OFFLINE_CANDIDATE_EVIDENCE_EXECUTION_AUTHORIZED=NO
MODEL_EXECUTION_AUTHORIZED=NO
PRODUCTION_CHANGE_AUTHORIZED=NO
UNIT_4_AUTHORIZED=NO
REMEMBER_RETRIEVAL_UNIT_1_CHANGED=NO
REMEMBER_RETRIEVAL_UNITS_2_TO_5_AUTHORIZED=NO
TEST_CHANGE_AUTHORIZED=NO
EXECUTABLE_FIXTURE_ADDITION_AUTHORIZED=NO
```

No candidate mechanism, implementation plan, evidence plan, executable fixture, test or production change is created or authorized.

## 11. Next lawful action

```text
NEXT_LAWFUL_ACTION=INDEPENDENT_CONSTITUTIONAL_REVIEW_OF_THIS_MISSING_FIXTURE_GOVERNANCE_DECISION
```

Only after this exact decision is independently accepted and preserved may later governance prepare the candidate/evidence plan required to define how the candidate and governed corpus would be represented and evaluated. That later planning still does not authorize implementation or execution. Evidence execution requires its own complete later governance and explicit authorization.
