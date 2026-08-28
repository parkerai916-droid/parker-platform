# Fidelity-Preserving Evidence Acquisition — Real Acceptance Scope Lock

## Status and effect

**Adopted owner governance — Programme FA, Unit FA.9.1.** This instrument defines the
prerequisites and separately authorised execution units for real-document and provider
acceptance. It authorises no evidence access or processing, OCR, external egress, provider or
reasoning request, acceptance-state mutation, request-allocation consumption, deployment, or
ordinary production execution.

This lock is subordinate to the FA.1 Fidelity-Preserving Evidence Acquisition Scope Lock, the OCR
Transcription Fidelity and Verification Amendment, the OpenAI external-transcription provider
authorization, the Unit O and O.4R locks, and FA.2–FA.8 implementation. Historical records and
allocations remain immutable.

## 1. Purpose and authority boundary

FA.8 proved the acquisition-to-analysis architecture with synthetic evidence. Later FA.9 units
may establish mechanism-appropriate performance on exact owner-authorised real evidence. They do
not decide admissibility, credibility, legal importance, or case outcome. The original
`EvidenceArtifact` remains authoritative; every acquired representation and analysis remains
subordinate.

No later unit inherits authority from this document. Each unit requires an explicit authorization
naming its exact evidence, mechanism or configuration, purpose, egress status, and request
allocation where applicable. “Process available evidence” is never sufficient authority.

## 2. Inspected implementation baseline

At FA.9.1 the production catalogue contains:

- `parker-tier-a-native-v1`: local direct/native extraction for searchable PDF, UTF-8 CSV, EML,
  and DOCX;
- `parker-docling-local-ocr-v1`: local OCR for scanned/image-only or mixed PDF, JPEG, PNG, and
  WebP; and
- an optional externally projected capability only when an exact provider configuration is
  supplied and governed.

Tier A supports `PDF`, `CSV`, `EML`, and `DOCX`. Legacy binary DOC and XLS/XLSX are not routed or
extracted. There is no separately registered general external-vision capability. The OpenAI
adapter is a bounded PDF/image literal-transcription mechanism, not general reasoning or an
authority to interpret images.

Authoritative source resolution verifies the exact custody ID, manifest ID, SHA-256, byte length,
and bytes before creating a defensive verified token. Acquisition executors accept that trusted
source binding. Analysis retrieves exact immutable generations and has no acquisition executor.

## 3. Real-document class determination

| Class | Determination | Minimum disposition |
|---|---|---|
| Born-digital searchable PDF | A — supported and requires real acceptance | FA.9.2 direct/native case |
| Scanned/image-only PDF | A — supported and requires real acceptance | FA.9.3 clean and degraded local-OCR cases |
| JPEG/PNG/WebP image | B — supported; synthetic acceptance sufficient initially | Add only if a later selected source exposes an image-specific defect not covered by scanned PDF |
| EML | B — supported; synthetic acceptance sufficient initially | No real case in the minimum matrix |
| DOCX | A — supported and requires real acceptance | FA.9.2 one structurally representative document |
| CSV | B — supported; synthetic acceptance sufficient initially | No real case in the minimum matrix |
| Legacy DOC | C — not supported | No FA.9 execution; separate implementation programme required |
| XLS/XLSX | C — not supported | No FA.9 execution; separate implementation programme required |
| Mixed printed/handwritten PDF | A for local characterization and a separately authorised external candidate | FA.9.3 and, only after prerequisites, FA.9.4 |
| Large benchmark or unrelated formats | D — outside the minimum useful pipeline | Excluded |

The minimum matrix is intentionally small: one searchable PDF, one representative DOCX, one clean
printed scan, one degraded/mixed-layout scan, and one difficult mixed printed/handwritten PDF.
One document may satisfy more than one technical characteristic only when that does not obscure a
mechanism-specific result.

## 4. Technical evidence selection

The owner selects exact real `EvidenceArtifactId` values separately. Selection is based only on
technical acquisition characteristics: native searchable content, scan cleanliness, degradation,
layout/table complexity, rotation, annotations, handwriting, page count, and supported media.
Legal importance, evidential strength, narrative relevance, and expected case outcome are not
selection criteria.

Before every execution, a metadata-only preflight records the authoritative ID, SHA-256, byte
length, media type, bounded page facts where safely available, chosen case, and governing unit.
It exposes no source text, prior derivative, case narrative, credential, or review content.

## 5. Direct-source proof

Every later execution must mechanically demonstrate:

1. the exact selected ID is retrieved from Evidence Custodian;
2. the authoritative manifest ID, digest, length, and media type match;
3. the bytes supplied to extraction/OCR/transcription are those verified custody bytes or a
   directly derived governed processing representation;
4. any representation retains source and representation digests, lengths, media types, page
   mapping, processing profile, transformation facts, and `byteExactCopy` truthfully; and
5. the admitted generation has the selected EvidenceArtifact as its root parent.

A prior OCR/transcription, browser preview, screenshot, worksheet, analysis result, or other
derivative is never acquisition input. Tests and counters must show zero derivative retrieval as
acquisition input and zero automatic second acquisition.

## 6. FA.9.2 native/direct acceptance

FA.9.2 requires separate owner authorization for one searchable PDF and one representative DOCX.
Execution is local-only and creates no provider request.

Acceptance compares mechanism-appropriate structure rather than applying OCR character metrics:

- source digest and length remain unchanged;
- expected pages/paragraphs, headings, tables, headers/footers, and document order are accounted
  for where the format exposes them;
- literal control passages, names, dates, amounts, and material wording are checked against the
  authoritative source;
- omissions, parser warnings, unsupported structures, and formatting loss are recorded;
- malformed or unsupported sources fail without a generation; and
- exact generation retrieval after restart performs zero re-extraction.

Success requires no invented substantive text, no material source-binding defect, materially
complete supported structure, truthful qualifications, and usable exact-generation retrieval.

## 7. FA.9.3 local OCR acceptance

FA.9.3 requires separately authorised clean printed, degraded/mixed-layout, and difficult
mixed printed/handwritten PDF cases. It uses the accepted local Docling capability only; there is
no external egress.

Review is page-by-page against the authoritative rendered source. Review units cover names, dates,
amounts, material wording, ordinary text, handwriting, omissions, additions, reading order,
tables/layout, page coverage, and uncertainty. Each discrepancy records classification,
criticality, page, and bounded scope without mutating the derivative.

Success is not “text was produced.” It requires exact source binding; complete truthful page
accounting; no critical invented text; no invented name, date, amount, or legal proposition;
materially complete readable printed content; bounded disclosure of illegible/uncertain material;
and layout/table qualifications wherever linear text cannot preserve structure. Local execution
does not promote fidelity to `VERBATIM`.

Handwriting failure may be classified `MODEL_CAPABILITY_LIMITATION` when output is honest and
qualified. Confident invented handwriting is `FAIL_INVENTION`, not an acceptable limitation.

## 8. External transcription and vision boundary

FA.9.4 is a separately governed external acceptance, not a continuation of local OCR. It may use
only an exact real EvidenceArtifact, an exact provider/configuration tuple, an exact new request
allocation, the verified durable attempt ledger, explicit external-egress authority, and a
source-grounded acceptance review.

No independent external-vision capability currently exists. General vision, layout reasoning,
image interpretation, rotation normalization, or transformed page rendering is deferred. Any such
candidate requires a new capability identity, processing profile, transformation provenance,
privacy review, offline tests, and acceptance authority; it cannot inherit literal-v2 status.

## 9. External provider configuration

The only implementation candidate presently defined is the complete tuple:

- provider: `OpenAI`;
- endpoint: `POST /v1/responses` at credential-free `https://api.openai.com`;
- `store=false`;
- model-selection rule: `gpt-4.1-mini`;
- profile: `openai-literal-page-transcription-v2`;
- adapter version: `1.1.0`;
- processing profile: `external-transcription.direct-byte-exact-v1`;
- instruction SHA-256:
  `c721e63b29e56f9242ee24dd8f13ddcab5d4468d3d17e9e3b9b1d66a68cb2000`; and
- schema SHA-256:
  `3fe8a26be40a06f047b493094d06c52e1df056162583b8e0b81564f55de265b2`.

Acceptance binds the exact provider-returned model identity and truthful snapshot state as well.
“OpenAI” or a profile label alone is not an accepted configuration.

Repository governance places historical v1 in acceptance-only/non-ordinary status after its
critical invention failure. Literal-v2 is not `ACCEPTED` for ordinary execution. Deployment-local
profile readiness cannot be inferred from source, and provider/model availability or currentness
cannot be established offline:

**REQUIRES SEPARATE PROVIDER-CURRENTNESS CHECK.**

That check must verify provider documentation, endpoint, exact selectable/returned model behavior,
profile review dates, retention/training/project-account controls, schema compatibility, limits,
and credential readiness without making the acceptance request. Any tuple change requires new
governance and invalidates acceptance assumptions for the prior tuple.

## 10. Unit O history and request allocations

Unit O remains immutable:

1. the initial CLEAN_PRINTED provider request was consumed and produced critical
   source-inconsistent invention;
2. the CLEAN literal-v2 attempt crossed an indeterminate acceptance boundary and is treated
   operationally as consumed/quarantined; it must not be retried under the same authority; and
3. O.5 `HANDWRITTEN_MIXED` remains reserved and unconsumed.

O.5 remains untouched and reserved for historical Unit O unless a later owner decision expressly
closes or repurposes it. FA.9 does not silently inherit it. The preferred FA.9 policy is a new,
separately named, immutable allocation per exact external acceptance case. Each allocation binds
one evidence ID, source tuple, provider tuple, repository commit, request ordinal, purpose, issued
authority, and attempt identity. Allocations are non-transferable and cannot fund retry, fallback,
provider switching, or another document.

## 11. Acceptance review and metrics

Acceptance review is stronger than ordinary operational review and does not create a universal
future review requirement. A named reviewer compares every page of each provider-acceptance case
to the authoritative source and records exact generation IDs, reviewer/time, page and optional
character scopes, review-artifact digest, discrepancy classification, criticality, and bounded
sensitive notes outside Git.

Use the existing classifications where applicable: `EXACT_CORRECT`, `SUBSTANTIVELY_CORRECT`,
`INCORRECT`, `OMITTED`, `INVENTED_HALLUCINATED`, and
`GENUINELY_UNREADABLE_UNCERTAIN`; criticality is `CRITICAL_FACT`, `SUBSTANTIVE_WORDING`, or
`ORDINARY_TEXT`.

Mechanism-appropriate decision dimensions are:

- **fidelity:** no substantive paraphrase or semantic substitution represented as literal text;
- **completeness:** materially complete readable scope and exact truthful page accounting;
- **invention:** zero critical invention and zero invented names, dates, amounts, or legal
  propositions; any such event fails acceptance;
- **uncertainty:** unreadable or ambiguous material is qualified, scoped, or omitted rather than
  fluently completed;
- **page accounting:** requested, submitted, returned pages and outcomes reconcile exactly;
- **layout/table:** material reading order, row/column association, and qualification of lost
  structure are reviewed categorically; and
- **handwriting:** correct or honestly uncertain transcription is acceptable by scope; confident
  unsupported completion is not.

No single character-accuracy percentage replaces these rules. Numerical counts may summarize
reviewed units and omission/invention rates because their denominators are explicit, but they do
not override a zero-tolerance critical invention or governance failure.

## 12. Non-inferiority

The authoritative source and source-grounded human review are the reference. Native extraction or
local OCR is a governed comparator only where it processes the same exact source and helps measure
operational usefulness; local OCR is not presumed gold standard or constitutionally preferred.

For an external case, non-inferiority means no worse performance than the applicable local/native
comparator on critical errors, material omissions, page coverage, reading order, uncertainty
honesty, and owner usability, while independently satisfying zero critical invention and all
governance gates. A provider can pass by honestly declining unreadable material; it cannot gain
credit for fluent invention.

## 13. Outcomes and failure classification

Later acceptance records use the narrow outcome set:

- `PASS`;
- `FAIL_FIDELITY`;
- `FAIL_COMPLETENESS`;
- `FAIL_INVENTION`;
- `FAIL_SOURCE_BINDING`;
- `FAIL_PROVENANCE`;
- `FAIL_PROVIDER_CONFIGURATION`;
- `FAIL_ATTEMPT_ACCOUNTING`;
- `FAIL_HUMAN_REVIEW`;
- `INDETERMINATE`; and
- `MODEL_CAPABILITY_LIMITATION`.

Operational failure remains distinct from quality failure. A failure creates no fabricated
generation. Apparent quality never overrides a governance failure.

## 14. Durable attempt accounting and retry prohibition

Every external acceptance uses the verified Unit O ledger on a Linux filesystem:

`AUTHORIZED → PREFLIGHT_PASSED → SOURCE_RETRIEVED → REQUEST_PREPARED →`
`PROVIDER_ATTEMPT_STARTED → PROVIDER_RESPONSE_RECEIVED → GENERATION_ADMITTED →`
`TERMINAL_SUCCESS`, with bounded governed failure transitions.

The durable `PROVIDER_ATTEMPT_STARTED` marker is forced immediately before send. There is one
authority, one attempt identity, and at most one provider attempt. There is **NO RETRY, NO
FALLBACK, NO PROVIDER SWITCH, NO MODEL SWITCH, and NO ENDPOINT SWITCH**. A crash, timeout, or
indeterminate result after the attempt boundary consumes that allocation and blocks a second
execution.

## 15. Suspension

An accepted exact external tuple must be proposed for `SUSPENDED` upon provider/model/profile,
adapter, processing profile, instruction digest, schema digest, endpoint, retention/training,
project-account, or response-shape change; stale provider review; material fidelity regression;
unexplained invented text; source-binding or provenance defect; schema drift; or attempt-ledger
defect. Suspension is a separate governed state mutation, not automatically implemented here.
While proposed or applied, ordinary execution fails closed.

## 16. Privacy, egress, credentials, and records

For an external case, only the selected source's exact byte-exact processing representation and
fixed instruction/schema may leave Parker. No unrelated document, case collection, prior
derivative, Memory, Knowledge, saved analysis, worksheet, or narrative context is included.

The authorization records provider, necessity, exact representation, owner egress permission,
allocation, attempt identity, admission result, and retained provenance. Provider request and raw
response bodies are not ordinary logs. Human worksheets remain restricted acceptance records
outside Git.

Credentials come only from the existing deployment-local environment mechanism and are wrapped by
`OpenAiApiCredential`; values are never printed, copied, persisted, committed, placed in fixtures,
or included in reports. Readiness checks disclose only bounded states.

## 17. Owner workflow and UI

No acceptance dashboard or new production endpoint is required. Existing FA.6/FA.7 owner
surfaces remain the ordinary workflow. Detached acceptance tooling may perform exact-ID preflight,
attempt accounting, result recording, and review without granting browser metadata authority.

Every execution authority names the owner, acceptance unit, exact EvidenceArtifactId, capability
and configuration, purpose, egress decision, repository commit, and allocation. Browser-supplied
fidelity, review, provenance, source facts, or stale routing decisions are never trusted.

## 18. Real acquisition-to-analysis and reasoning provider

FA.9.5 is separate from acquisition-provider acceptance. First, a real accepted derivative may be
selected for analysis using a fake/local deterministic reasoning seam to prove exact traceability
without another external request. Declaring the Minimum Useful Pipeline operational also requires
a separately authorised assessment of the actually intended reasoning provider on an exact real
derivative, because synthetic provider output proves orchestration but not useful real analysis.

Transcription-provider acceptance never authorizes reasoning-provider execution. Any real
reasoning request requires its own exact derivative selection, provider/configuration identity,
purpose, privacy decision, allocation, attempt accounting, human review, and stop conditions. It
must preserve source/derivative distinction and cannot mutate Evidence, acquisition generations,
Memory, or Knowledge.

## 19. Deployment boundary

FA.9.1 requires no deployment. FA.9.2 and FA.9.3 may execute through an already deployed,
verified Parker build only after separate authority; deployment itself, if a new commit must reach
the Parker host, is a distinct FA.9.2D/FA.9.3D authorization and does not authorize evidence
processing. The first external-request unit similarly separates any required deployment from
request authority.

Production stores are never used from ad hoc test workspaces. Offline verification may use
temporary stores; real executions use the governed deployed custody and durable stores only.

## 20. Exact later units

1. **FA.9.2 — Real Native/Direct Acquisition Acceptance.** Select exact searchable-PDF and DOCX
   evidence; perform local metadata preflight, explicit owner execution, source-grounded review,
   restart retrieval, and closure. No provider request.
2. **FA.9.3 — Real Local OCR Acceptance.** Select exact clean, degraded/mixed-layout, and difficult
   mixed printed/handwritten PDF evidence; execute local OCR once per separately authorised case;
   review fidelity/page/uncertainty; no external request.
3. **FA.9.4P — External Provider Currentness, Profile, Privacy, and Allocation Gate.** No provider
   request. Verify the exact candidate tuple and deployment-local readiness, authorize a new FA.9
   allocation, bind exact evidence, and place only that tuple into acceptance-pending execution
   state through a separately reviewed mechanism.
4. **FA.9.4 — Real External Literal-Transcription Acceptance.** One exact authority and at most one
   request per separately authorised case, durable ledger, exact admission/retrieval, mandatory
   page-by-page review, decision, and closure. O.5 is not used unless separately and expressly
   amended.
5. **FA.9.5 — Real Acquisition-to-Analysis Acceptance.** Select an exact accepted real derivative;
   first prove analysis traceability without external reasoning, then use a separately authorised
   reasoning-provider request only if required for Minimum Useful Pipeline acceptance.
6. **FA.9.6 — Programme Closure.** Consolidate accepted capability tuples, negative evidence,
   suspension disposition, regressions, operational runbook, and Minimum Useful Pipeline decision.

If deployment is required, insert a deployment-only `D` unit immediately before the affected
execution unit. It grants no evidence-processing or request authority.

## 21. Stop conditions

Stop before execution if any exact source, owner authority, capability/configuration tuple,
currentness/privacy fact, credential readiness state, request allocation, Linux ledger, review
method, storage boundary, or repository commit binding is absent or mismatched. Stop on source
digest/length mismatch, stale provider review, unexpected transformation, attempt-ledger defect,
provider tuple drift, real-evidence leakage, or any need for retry/fallback/provider switching.

No unit may weaken direct-source lineage, immutable generations, exact retrieval, uncertainty,
fidelity separation, provider readiness, durable attempt accounting, or owner authority to make an
acceptance case pass.

## Final determination

FA.9 real acceptance is constitutionally bounded and split into independently authorised local,
external, analysis, deployment, and closure operations. This instrument authorises none of those
operations.

**UNIT FA.9.1 ADOPTED — REAL ACQUISITION ACCEPTANCE SCOPE LOCKED.**
