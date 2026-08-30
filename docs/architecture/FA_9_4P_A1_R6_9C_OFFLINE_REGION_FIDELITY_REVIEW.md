# FA.9.4P-A1E-R6.9C — Offline region fidelity review

## 1. Scope and disposition

This unit performed a source-grounded, offline fidelity review of the immutable A1 region response.
The final classification is **PASS_FIDELITY**.

No provider was called, no retry or authority was created, no durable provider or assessment record
was modified, no lifecycle promotion occurred, and production was not deployed or restarted. The
original R6.9 assessment remains historically factual as `VALIDATION_MALFORMED_SCHEMA`. There is
no separately governed append-only production assessment mechanism for this superseding review, so
the R6.9C finding is recorded in repository documentation only.

## 2. Immutable identities and structural gate

- authority: `authority-fa-9.4p-a1e-r6.8c1`
- execution: `execution-fa-9.4p-a1e-r6.8c1`
- response: `resp_0007d6aa81587b3e016a92f716feb087d0ae9e005456676627`
- model: `gpt-5.6-sol`
- request digest: `1a691388478370add9bae4e920fb1071369efa543057403727b422e9000a3d36`
- provider-state record: `31b997b2a5208ab120fa483778bca9f1ec270c994b7937b3e5dd765db2bfabcd`
- raw response SHA-256: `500863d65c7f9ca69a66b2ffef3ef8a42b7033903cf1b5a5bd774d9f0decd87f`
- canonical structured-state SHA-256: `7031179aa4267fdc12a50a429eef184e4ecfb2efb3ae993b6a5527ecf9f4c476`
- complete canonical provider record SHA-256: `ad2542015546250bfe0640e5c31636bb6401a20d537d95db04248c81883ad135`
- provider-state file SHA-256: `edd6b3fcf357edfcfce0e6825e2b8734462fb2654f6816255d8e6305b080b4c9`
- original assessment file SHA-256: `62a5b9a266db3cf4702eb5583f61f8a2f23d96e4954371b2750ddf07ebb50ae8`

The exact copied response was replayed under the explicit R6.9B v5 observation semantics. The gate
passed with 24 requested IDs exactly once, 24 `TRANSCRIBED` statuses, 24 populated strings,
70/70 unchanged `LINE_BREAK [n,n)` points accepted, and no other structural defect. Raw bytes and
the structured map were not changed.

## 3. Authoritative rendered source

The source was the 111,122-byte, three-page PDF
`evidence-0275472f-535a-4cf1-b30d-f45ac7684743`, SHA-256
`7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182`.

Parker's `apache-pdfbox 3.0.7` deterministic renderer was run at the governed 300-DPI profile.
All results matched the existing v2 authority:

| Page | Dimensions | Canonical pixel digest | Encoded PNG SHA-256 | Regions |
|---:|---|---|---|---:|
| 1 | 2482×3507 | `e0bafdc978ff9e6ca7b6f312c34abebd02d6e408ce6c5a198f2b89e84a460683` | `45978b3bf9ea03b3c565ce54ab8f40e2ed072a836aa09bc6d504d172383a44e5` | 5 |
| 2 | 2482×3507 | `9c854c27deae0978f58086a77c0f29cfd1c202b719e3d843c58370a4d41dcfe2` | `f4a7c2093d71e1cdd7da97f108ae7c79c017eeb2db390ef6c9dc45b34fd4da90` | 15 |
| 3 | 2482×3507 | `39d1af8e8856a76f34d9401846be6f5af5aa363ab3d7256c64b1094a846a5e66` | `d8c3c0bd0f273a60fd581bc920360ec54ce99f5659ff6459c30f0b78c77579bc` | 4 |

Every one of the 24 reconstructed crop pixel digests and encoded PNG hashes matched its governed
authority fact before visual review. Native PDF text extraction was not used as an answer key or to
correct, supplement, or normalize the provider text.

## 4. Source-order reconstruction

The `order.source.1` through `order.source.24` facts in the existing v2 authority were decoded
using Parker's authority storage implementation. They define five page-1 regions, fifteen page-2
regions, and four page-3 regions. Provider-returned ordinals were preserved separately and form the
same 1–24 sequence in this response. Thus provider order happens to equal Parker source order here;
provider order was not used to establish that order.

## 5. Page 1 review

Page 1 contains five regions. Parker source order is regions 1–5.

### Reconstructed provider transcription

```text
Agreed. That is now your programme decision: reopen the Reasoning Protocol
programme as ACTIVE and take the Family F alternative-model path, initially only to its
own Planning Review.

The important bit is sequencing. We should not collapse your four decisions into one giant
authorisation document. Parker’s existing governance deliberately requires the decision to
reopen to remain separate from the later decision to run Qwen.

The authorised direction

The governance chain should now be:

Reasoning Protocol programme
        |
        | currently PAUSED
       ▼
1. REOPENING DECISION
        |
       ├─ PROGRAMME_STATUS = ACTIVE
        |
        ├─ REOPENING_TRIGGER = SATISFIED
        |
       ├─ AUTHORISED_PATH = FAMILY F
        |
       └─ FAMILY F authorised to:
            PLANNING REVIEW ONLY
        |
        | NO model run
        | NO implementation
        ▼
2. FAMILY F PLANNING REVIEW
        |
        | Question:
        | Does qwen2.5-coder:7b on the existing
        | Parker server warrant governed diagnostic
        | evaluation against the known failures?
       ▼
3. FAMILY F DIAGNOSTIC GOVERNANCE
        |
       ├─ minimum necessary Scope Lock
       ├─ diagnostic/evidence plan
       ├─ readiness verification
       └─ explicit execution approval
       ▼
4. SERVER DIAGNOSTIC
        |
       └─ qwen2.5-coder:7b through Ollama
        ▼
5. EVIDENCE REVIEW
```

### Region findings

| # | Region ID | Finding | Notes |
|---:|---|---|---|
| 1 | `bb167523&59b97` | EXACT | Text, `ACTIVE`, punctuation and line breaks match. |
| 2 | `89e890b3&b124` | EXACT | Prose, possessive punctuation and source position match. |
| 3 | `f7c410f0&25712` | EXACT | Heading matches. |
| 4 | `c5ff1a3f&4427e` | EXACT | Introductory label and colon match. |
| 5 | `e4c3079b&57278` | EXACT | Diagram labels, identifiers, arrows, branch characters, indentation and order match. |

Omissions, substitutions, inventions, duplicated text, ordering failures, and uncertainty defects:
none. Overall page assessment: **exact fidelity**.

## 6. Page 2 review

Page 2 contains fifteen regions. Parker source order is regions 6–20.

### Reconstructed provider transcription

```text
That respects the existing Reopening Assessment, which expressly says an ACTIVE
decision may nominate one already-classified path to proceed to its separate
Planning Review only. It cannot itself select a remedy, authorise a model call, or create
the Scope Lock.

There is another distinction worth freezing now: we are not switching Parker from Llama
to Qwen. We are not declaring Qwen better. We are not qualifying Qwen. And we are
certainly not quietly changing production while nobody is looking.

The initial proposition is simply:

Determine whether qwen2.5-coder:7b, in Parker’s existing server environment,
warrants governed diagnostic evaluation as a Family F alternative-model
candidate against the known Reasoning Protocol failure surface.

And when we eventually reach the server diagnostic, its results remain a separate Family
F evidence stream. They are not retrospectively mixed with the llama3.2:3b
Control/Family A/B/C campaign. The accepted assessment specifically requires that sort of
provenance separation.

One change to your step 3

I would phrase it slightly more carefully than “minimum Scope Lock / execution approval.”

We should let the Family F Planning Review determine what minimum governance is
actually required rather than deciding its answer in advance. The repository already says
Family F requires fresh governance, but the Planning Review should establish the precise
diagnostic tier, fixtures, evidence requirements and approvals.

That prevents us from writing the Planning Review with the conclusion conveniently
stuffed in its pocket.

What we do next

Only Step 1 now.

Author the standalone:

REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_DECISION.md

Its operative determination should ultimately be:

REOPENING_TRIGGER=SATISFIED
PROGRAMME_STATUS=ACTIVE
```

### Region findings

| # | Region ID | Finding | Notes |
|---:|---|---|---|
| 6 | `abfcdae2&9f0d1` | EXACT | `ACTIVE`, authorization wording and line breaks match. |
| 7 | `156de26c&dc22b` | EXACT | Llama/Qwen names and negations match. |
| 8 | `46ee798d&b6e95e` | EXACT | Proposition lead-in matches. |
| 9 | `5dfb6c25&ab668` | EXACT | Entire proposition, `qwen2.5-coder:7b`, punctuation and line breaks match. |
| 10 | `cf81d18a&faf15` | EXACT | Family F attribution, `llama3.2:3b`, campaign label and position match. |
| 11 | `805468eb&d43d` | EXACT | Heading and step number match. |
| 12 | `be780856&47aee` | EXACT | Quoted text and punctuation match. |
| 13 | `78fcff1e&8f2a2` | EXACT | Governance prose and line breaks match. |
| 14 | `868200fc&df4a` | EXACT | Sentence and line break match. |
| 15 | `0d1c3c2a&39a0f` | EXACT | Heading matches. |
| 16 | `cc46b282&c70a7` | EXACT | Step number and punctuation match. |
| 17 | `590e3b57&a09b8` | EXACT | Introductory label and colon match. |
| 18 | `e7ac0348&dfc4` | EXACT | Filename is exact. |
| 19 | `fedb44f6&20ebb` | EXACT | Operative-determination label matches. |
| 20 | `84be5cd5&4a563` | EXACT | Both identifiers, values, underscore placement and line break match. |

Omissions, substitutions, inventions, duplicated text, ordering failures, and uncertainty defects:
none. Overall page assessment: **exact fidelity**.

### Page 2 proposition witness

Witness `5dfb6c252dd668e7ae9dc1be95c8243c4505916e825ca143a0b02e543a9ab668`
visibly reads, and the provider exactly returned:

```text
Determine whether qwen2.5-coder:7b, in Parker’s existing server environment,
warrants governed diagnostic evaluation as a Family F alternative-model
candidate against the known Reasoning Protocol failure surface.
```

It occupies Parker source position 9, immediately after “The initial proposition is simply:” and
before the separate-Family-F-evidence paragraph. The earlier whole-document displacement failure
is absent.

## 7. Page 3 review

Page 3 contains four regions. Parker source order is regions 21–24.

### Reconstructed provider transcription

```text
AUTHORIZED_PATH=FAMILY_F_ALTERNATIVE_MODEL

FAMILY_F_CURRENT_AUTHORITY=
PROCEED TO A DEDICATED PLANNING REVIEW ONLY

REMEDY_SELECTED=NO
MODEL_SELECTED=NO
MODEL_QUALIFIED=NO

IMPLEMENTATION_AUTHORIZED=NO
MODEL_RUN_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO

QWEN_2_5_CODER_7B_RUN_AUTHORIZED=NO

KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO

NEXT_LAWFUL_ACTION=
FAMILY F ALTERNATIVE-MODEL DIAGNOSTIC PLANNING REVIEW

Once that document has been independently constitutionally reviewed, committed and
merged, then we author Step 2.

That gives you exactly the direction you’ve chosen while preserving Parker’s governance
chain.

And importantly, tonight we do not touch the running Parker server or Qwen yet. The
next operation is repository governance only.
```

### Region findings

| # | Region ID | Finding | Notes |
|---:|---|---|---|
| 21 | `e2c2d8fe&3e84ff` | EXACT | All authorization keys, values, underscores, negations, blank lines and final action match. |
| 22 | `b78c8e9d&7f77c2` | EXACT | Review/commit/merge condition and Step 2 wording match. |
| 23 | `29e27784&78e13` | EXACT | Governance-chain closing sentence and line break match. |
| 24 | `cf9708d6&415fec` | EXACT | Running-server/Qwen negation and final repository-only sentence match. |

Omissions, substitutions, inventions, duplicated text, ordering failures, and uncertainty defects:
none. Overall page assessment: **exact fidelity**.

### Page 3 authorization witness

Witness `e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff`
is the first source-ordered region on page 3. Its provider transcription exactly matches the visible
authorization block reproduced above. The closing prose remains after it in regions 22–24; no
closing prose was moved ahead of the authorization. The earlier whole-document ordering failure is
absent.

## 8. Whole-document findings

Exactly 24 regions were visually reviewed.

| Category | Regions |
|---|---:|
| EXACT | 24 |
| IMMATERIAL_FORMATTING_VARIATION | 0 |
| MINOR_FIDELITY_ERROR | 0 |
| MATERIAL_OMISSION | 0 |
| MATERIAL_SUBSTITUTION | 0 |
| INVENTED_CONTENT | 0 |
| DUPLICATED_CONTENT | 0 |
| SOURCE_ORDER_FAILURE | 0 |
| UNCERTAINTY_CORRECTLY_EXPOSED | 0 |
| UNCERTAINTY_INCORRECTLY_CONCEALED | 0 |
| ILLEGIBLE_OR_VISUALLY_INDETERMINATE | 0 |

No discrepancy changes a name, date, number, proposition, authorization, attribution, negation,
document structure, or evidential meaning. No omission, substitution, invention, duplication, or
uncertainty-handling defect was found.

**Final fidelity classification: PASS_FIDELITY.**

## 9. Production preservation and next action

Before the review, production was container
`192237eea021e46057c6cb84af86379dcf9741f481d7e262c0e87a8e350986ad`, image
`sha256:2e17e156cdceda160e9a354c2458b9dbec09b73156ff33d3522d4e91bc4ff923`,
restart count zero, and lifecycle `ACCEPTANCE_PENDING`. The legacy authority hashes were
`cbbfa02e779a1a7c524a43bb711dce5fd21bfe3d35bfaa93a472ce1b14ec8af1` and
`889b7c138fa9ef274b41e3f20bbb79411bf655e0bca11947d2a87e3dc5e8aee4`; the region authority
hash was `05b8407e14fc7b2bf9c8d3f2d2519981c5e39070559bc90eaf5edb1cc6ba984d`; the
attempt-ledger hash was `e6e3a6eba4a2033e58c23a06c39aed95fc1a7c880cd3e568ee0ae01d7806741d`.

The post-review check must match those identities plus the provider-state and original-assessment
file hashes in section 2. OpenAI requests: 0. Claude requests: 0. Retries: 0. Deployments: 0.
Restarts: 0. Production assessment writes: 0.

Because fidelity passed, the next bounded action is a separate, explicitly owner-governed lifecycle
promotion unit. The existing mechanism is the owner-controlled external-transcription provider
profile's `acceptanceState=ACCEPTED` setting, loaded by
`OpenAiExternalTranscriptionProviderProfileLoader` and enforced by
`OpenAiExternalTranscriptionBackendReadiness` and `ParkerRuntime`. Applying that setting would
require its own governed production configuration change and controlled restart/deployment. R6.9C
does not edit the profile, invoke that mechanism, deploy, or restart; lifecycle remains
`ACCEPTANCE_PENDING`.
