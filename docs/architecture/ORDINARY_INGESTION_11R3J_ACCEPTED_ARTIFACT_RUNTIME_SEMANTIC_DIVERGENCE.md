# Ordinary Ingestion OI11R3J — Accepted Artifact / Runtime Semantic Divergence

## Starting state

- Host: `parker`
- Repository: `/home/steve/parker-platform`
- Branch: `main`
- HEAD/upstream: `1180bd4c0480899f9032ae71ade6d00d355c6847`
- Worktree: clean
- Production: stopped; latest failed container `afcbbcd2140272304287279eae53b4fbb2f69bdabbefaf743feeb8754347dffe`
- Accepted artifact: `sha256:0cefbbab93e33cd066e27ce5dc1d35bb8b0e5601323e4d3d88fc39b030e31fc9`
- Accepted source identity: `ae63687eb5bea5832ff3c4904920540150da202a`
- Provider profile: `ACCEPTED`, SHA-256 `3038538d53b98595631c76325062688b40c449d512bb94cae17be2e7f0d6e956`

## Source lineage

The accepted artifact was built from the OI11R2 source identity at `ae63687`.
Post-artifact runtime-bearing commits are:

1. `5bc30fe` — adds `RuntimeReadinessDiagnostic`, shared manifest lookup, and
   routes the region composition guard through the structured evaluation.
2. `854910c` — adds the explicit `ordinaryExecutionReady` diagnostic field and
   tests; no independent production guard change.

`4281393` and `1180bd4` are documentation commits. `OI11R3I` also included an
implementation-bearing diagnostic clarification at `854910c`; it was not in
the accepted image.

## Targeted source diff

Between `ae63687` and `1180bd4`, the relevant runtime difference is confined to
`ParkerRuntime.kt` and the new `RuntimeReadinessDiagnostic.kt`. Provider-profile
and credential classes are unchanged. The current source centralizes the
composition predicates in the diagnostic; the accepted artifact has the prior
inline compound guard. The current source additionally distinguishes
composition `overallReady` from lifecycle-gated `ordinaryExecutionReady`.

## Direct artifact inspection

The accepted image was inspected without starting it. Its application JAR:

- contains `Parker-Source-Commit: ae63687eb5bea5832ff3c4904920540150da202a1`;
- has JAR SHA-256 `571e455bc6e2ddcabaf75470b8d6d154a0f1187902f67de125733d8f8f659009`;
- contains no `RuntimeReadinessDiagnostic` classes;
- contains the old compound error string and pre-diagnostic `ParkerRuntime`.

Therefore the current-source diagnostic cannot be treated as proof that the
accepted artifact executes the same readiness implementation.

## Readiness comparison

| Concern | Accepted artifact | Current source | Equivalent? |
|---|---|---|---|
| acceptance roots/attempt ledger | inline nullable checks | structured diagnostic inputs | logically corresponding |
| build identity | inline equality with embedded commit | same equality through diagnostic | logically corresponding |
| profile structure/non-stale | profile evaluator result must be `Ready` | same evaluator result | yes |
| profile lifecycle | acceptance lane remains separately lifecycle-gated | explicit `providerProfileAccepted` and `ordinaryExecutionReady` | current visibility is richer |
| credential | non-null credential | presence/structural validity diagnostic | current visibility is richer |
| compound region guard | old inline expression | shared `overallReady` result | source implementation differs |
| diagnostic surface | absent | present | no |

The old and new known predicates are intended to be equivalent for region
composition, but only the current source exposes them independently. The
accepted artifact has no instrumentation to establish which nullable input
was false in its own runtime.

## OI11R3H and OI11R3I impact

OI11R3H changed runtime source by introducing the shared diagnostic evaluation
and routing the region guard through it; this was an implementation/refactoring
change with tests preserving the acceptance-lane semantics, not a V8 provider
schema change. OI11R3I added the explicit ordinary-execution readiness field
and its tests. Neither change was present in the accepted image.

## Reconciliation and deployability

The current-source non-egress diagnostic, run with the candidate's captured
environment and verified embedded commit, reports every known composition
predicate true and reports ordinary execution ready. The deployed accepted
artifact nevertheless fails its uninstrumented old guard at line 1516. The
proven fact is a source/artifact semantic and observability divergence: the
diagnostic PASS is not evidence about the old artifact's runtime-derived
values. The precise false nullable value inside the old artifact remains
unobservable without modifying or instrumenting that artifact.

Artifact `sha256:0cefbb...` remains historically accepted, but it is not
currently deployable with sufficient confidence under the post-OI11R3H
readiness implementation. It must not be silently revoked or rewritten.

## Replacement decision and governance consequence

A new concrete candidate is required from exact source commit `854910c`, the
latest runtime-bearing source state before the documentation-only closure.
That candidate must undergo the existing layered Model C build, verification,
immutable preservation, and explicit owner artifact acceptance process. The
previous source and artifact acceptances remain historical truth; they do not
authorize treating the new artifact as identical.

`OI11R3 RETRY NOT AUTHORIZED` remains in force. The next bounded unit is a new
replacement-candidate build and acceptance unit from `854910c`; it must stop
before deployment.

## Integrity

No image was built or pulled, no provider/profile/acceptance state was changed,
production remained stopped, and stores remained `4 / 2 / 1 / 21 / 19 / 6 / 5`.
OpenAI, Claude, external-provider, and retry counts were all zero.

