# OI11R5M — Governed Production Deed Preparation and Owner-Reviewed Transport Equivalence Verification

## Verdict

**C — PREPARATION OR REQUEST GATE FAILURE**

Production identity, V8 authority, evidence custody and historical-state gates passed. Preparation stopped before reading the Deed into the achromatic builder because the deployed artifact has no governed production preparation-only operation and no composed persistent corrected-preparation store. Proceeding would have required either evidence-specific execution authorization or an ad hoc, non-governed invocation. Neither is permitted by OI11R5M.

## Starting repository state

Branch was `main`. HEAD and upstream were both exactly `90578cc74d5b12509777897a51ea8fa583d32b2d`; the worktree was clean and `git diff --check` passed.

## Production identity and health

Production exactly matched the required gate:

| Identity | Exact value |
|---|---|
| Container | `eb5a5bcf74ec26fe09bbb59b9a10a9eb0fd92d02a92cdb5812a18c35c3a4dd0f` |
| Image/index | `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f` |
| Source/build | `fe13047df0dd5f155d6a6921acf7bc85541af26f` |
| JAR SHA-256 | `fd259f70b58843a2bee8955edb98a767b89b0307643d8fe57eb155f22448fe89` |
| Restart count | 0 |

The canonical runtime diagnostic returned every predicate true, including exact build identity, readable required stores, `ordinaryExecutionReady=true`, `overallReady=true`, and empty reasons. No deployment or restart occurred.

## V8 authority and semantics

The authenticated canonical evaluator returned `ACCEPTED` for runtime and accepted promoting commit `fe13047df0dd5f155d6a6921acf7bc85541af26f`. It exposed capability `ordinary-external-request-region-transcription-v8`, adapter version `7.0.0`, profile `request-region-fidelity-acquisition-v4`, wire version 8, maximum 32 regions, 16,777,216-byte maximum, and batching false. The frozen capability digest remains `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`.

Acceptance record `ab14b21a53d1e2ddccb7c0dfbacb0189e6ec6ccbde46c1384467f58fdf062d76` remained present with SHA-256 `4291d3e429274712b343500d5538661b0dcd0f7d42b539e57efaa5bb3edeaa3a`. This is capability authority only; it was not treated as evidence-specific authority.

## Evidence identity

The registered source was verified directly inside the mounted production evidence stores before any preparation attempt:

| Field | Exact value |
|---|---|
| Evidence | `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9` |
| Evidence bytes | 1,887,733 |
| Source SHA-256 | `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e` |
| Manifest bytes | 196 |
| Manifest SHA-256 | `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34` |
| MIME | `application/pdf` |
| Declared pages | 5 |

No source or manifest mutation occurred.

## Historical R5F preservation

The historical page-1 representation remains `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e`, with region-set digest `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f` and disposition `SOURCE_ORDER_REVIEW_REQUIRED`.

The geometry record remains SHA-256 `8c8d9949b7fa9308381c3be3915e8ab5f78c4c0575cf30315481a4296565fcb4`; the order record remains `99b594592e18d812da7750e84873071a6ed51604a4a8a17708bbf3cf3ed70e79`. No owner-resolution record, corrected substitution, constituent reuse or reinterpretation was created.

## Production preparation composition failure

The accepted artifact contains the implemented profile, chromatic-risk gate, deterministic grayscale/PNG code, canonical request builder, codec and `FileSystemFullPageAchromaticPreparationStore`. OI11R5J already verified those artifact semantics and deterministic local results.

The deployed production composition does not, however, wire the persistence surface needed by this unit:

1. `FullPageAchromaticCanonicalRequestRegionV8Builder` accepts optional `pagePersistence` and `preparationPersistence` dependencies, both defaulting to `null`.
2. `OrdinaryRequestRegionV8RequestPreparer` constructs that builder with its default constructor.
3. `ParkerRuntime` constructs `OrdinaryRequestRegionV8RequestPreparer()` without a governed page/preparation store.
4. The deployed container has no corrected-preparation storage configuration, environment key or mount.
5. The runtime owner HTTP surface exposes proposal, evidence authorization and execution, but no preparation-only operation.
6. The request preparer is reached inside `OrdinaryRequestRegionV8IngestionWorkflow.execute`, after evidence-specific authorization checks and execution identity creation. OI11R5M expressly forbids invoking that route.

Consequently the live artifact can only create an in-memory achromatic construction as part of an authorized execution flow; even there, its default builder would not persist the corrected preparation document. It cannot meet R5M's create-once persistence/readback requirement through an authorized preparation-only production operation.

The artifact's `FullPageAchromaticLocalAcceptanceCli` was deliberately not used. Its own contract is local-only, it writes review copies rather than governed preparation state, it constructs no persistent `FileSystemFullPageAchromaticPreparationStore`, and invoking it would not prove production create-once/readback semantics. No temporary injected helper, manual codec reconstruction, direct filesystem fabrication or execution authorization was used to bypass this boundary.

## Gates not executed

Because persistence and invocation are prerequisites, the Deed was not passed to the production achromatic preparer. Therefore R5M does not claim a production chromatic-risk result, production geometry, preparation identities, transport digests, exact 5/5 equivalence, aggregate sizes, canonical request cardinality/digests, request-content inspection or persistence readback.

The previously accepted R5I/R5J local values remain historical evidence, but they cannot be relabelled as governed production preparation evidence. This is not a transport-equivalence failure, so verdict B does not apply; no production transport bytes were generated to compare.

## Store accounting and authority boundary

| Store | Before | After | Delta |
|---|---:|---:|---:|
| evidence | 29 | 29 | 0 |
| evidence source manifests | 29 | 29 | 0 |
| capability acceptances | 10 | 10 | 0 |
| owner authorizations | 11 | 11 | 0 |
| attempts | 8 | 8 | 0 |
| provider state/assessment | 6 | 6 | 0 |
| derivative generations | 22 | 22 | 0 |
| derivative content | 20 | 20 | 0 |
| region acceptance authority | 1 | 1 | 0 |
| corrected governed production preparations | 0 | 0 | 0 |

No evidence-specific authorization, reservation, execution ID, attempt, provider state, grayscale transport derivative, request state or transcription derivative was created. The request remains unprepared and unauthorized, rather than being described inaccurately as prepared.

## Provider accounting and stability

OpenAI calls: 0. Claude calls: 0. Other external calls: 0. Retries: 0. External egress: 0. Provider-state delta: 0.

Production remains on the same exact container/image/source, restart count 0, readiness PASS and V8 evaluator `ACCEPTED`. No provider response exists for this unit.

## Required bounded correction

A separately governed implementation unit must add a canonical preparation-only production operation that:

- is reachable without evidence-specific execution authorization, execution ID, attempt or transport;
- composes `FullPageAchromaticCanonicalRequestRegionV8Builder` with governed authoritative-page and corrected-preparation persistence;
- configures and mounts a durable corrected-preparation root;
- exposes create-once persist/readback and conflict failure;
- returns only structural identities, hashes and sizes, never raw sensitive request content;
- performs no provider call and cannot transition an attempt;
- retains the unchanged V8 request/response semantics and digest.

That change would alter implementation identity and therefore requires its own bounded implementation/test, artifact acceptance, deployment and implementation-bound capability-acceptance sequence before R5M can be retried. No such correction is implemented by this report.

## Exact stop state

**C — PREPARATION OR REQUEST GATE FAILURE.** OI11R5M stopped before preparation because the accepted production composition cannot persist or read back corrected preparation state through a preparation-only governed path. Historical R5F and all production stores remain immutable except for no changes at all in this unit. No evidence authority, execution, provider call, retry or external egress occurred.
