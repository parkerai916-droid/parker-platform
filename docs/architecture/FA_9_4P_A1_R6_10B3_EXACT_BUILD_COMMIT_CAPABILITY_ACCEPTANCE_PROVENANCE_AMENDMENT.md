# FA.9.4P-A1E-R6.10B3 — Exact build-commit capability acceptance provenance

## 1. Status and precedence

This amendment supplements the R6.10B ordinary external region-ingestion architecture and
supersedes only its undefined “ancestor/equal governed commit according to the configured build
policy” rule. R6.10B1 and R6.10B2 are unchanged. This is architecture only; it creates no acceptance
record and changes no runtime or production state.

## 2. Confirmed defect

R6.10B required a promoting commit relationship without defining a build policy. Parker has no
governed runtime Git history, ancestry service, compatible-commit list, or branch/reference lookup.
Its existing provenance mechanism is exact: `PARKER_BUILD_COMMIT` becomes the application manifest
attribute `Parker-Source-Commit`, and runtime composition compares the configured production commit
to that embedded value.

An acceptance evaluator could not implement the former rule without inventing ancestry governance.

## 3. Governance decision

Capability acceptance is bound to **exact embedded build-commit equality**:

```text
acceptanceRecord.promotingBuildCommit
    == runtimeEmbeddedParkerSourceCommit
```

Both values are exactly 40 lowercase hexadecimal characters. Equality is byte-for-byte after normal
field parsing; there is no case folding, abbreviation, branch inference, ancestry lookup, fallback,
or compatibility list.

The embedded manifest value is the authoritative runtime build-provenance fact. The evaluator uses
the same `ParkerRuntime` build-identity source that reads `Parker-Source-Commit`; R6.10C may expose
that already-loaded value through a narrow internal provider rather than rereading manifests.

## 4. Capability identity and build provenance

These checks are independent and both mandatory.

Capability identity binds provider, model, endpoint/operation, adapter/version, profile, wire,
schema digest, instruction digest, processing profile, renderer, region geometry, reasoning,
`store=false`, and image detail.

Build provenance binds the exact `Parker-Source-Commit` of the runtime containing the composition
and governing code. Matching capability fields do not accept another build. Matching build commit
does not excuse any capability-field mismatch.

The acceptance record field is `promotingBuildCommit`. It is part of the canonical create-once
payload and record digest and must satisfy Parker's existing exact commit regex.

## 5. Evaluator and failure behavior

`RegionTranscriptionCapabilityAcceptanceEvaluator` shall:

1. load and integrity-check the create-once acceptance record;
2. validate the complete exact region-v5 capability identity and evidence chain;
3. obtain the runtime embedded `Parker-Source-Commit` from the existing build-identity provider;
4. require both commits to match `^[0-9a-f]{40}$`;
5. require exact equality;
6. return accepted only when every capability, evidence, lifecycle and build check passes.

It fails closed with bounded reasons when the embedded commit or record commit is missing,
malformed, duplicated/ambiguous, or unequal. It performs no repository read, network access or Git
command. There is no ancestry fallback.

## 6. Existing Parker mapping

- `docker-compose.yml` requires `PARKER_BUILD_COMMIT` for the image build.
- `Dockerfile` declares and propagates the build argument.
- `build.gradle.kts` writes it to `Parker-Source-Commit` in the JAR manifest.
- `ParkerRuntime`'s `buildIdentity` loader accepts a single exact lowercase 40-character value.
- Current runtime composition already requires configured `productionCommit == embeddedCommit` for
  governed acceptance/region composition.
- `ParkerRuntimeConfig` validates source and production commits using the same canonical format.

R6.10C therefore needs only a narrow reuse/exposure of the already obtained `embeddedCommit` to the
new evaluator. No second commit format or runtime Git facility is needed.

## 7. Future rebuild semantics

An immutable acceptance record for build X accepts only runtime X. If Parker is rebuilt at Y and
`Y != X`, the X record does not accept Y even when adapter 4.0.0, profile v2, wire v5 and every other
capability field remain textually identical. Runtime Y stays fail-closed until a new, separately
governed create-once acceptance record is admitted for Y. Record X remains immutable provenance for
runtime X and is never rewritten.

This conservative rule prevents implementation or composition changes from inheriting acceptance
silently.

## 8. R6.10D promotion sequence

R6.10D shall:

1. start from the exact reviewed R6.10C implementation commit X;
2. build with `PARKER_BUILD_COMMIT=X` using the existing mandatory mechanism;
3. verify the immutable image embeds `Parker-Source-Commit=X`;
4. deploy that exact image with region-v5 still unavailable for ordinary execution until acceptance
   exists;
5. verify runtime/container build identity is X;
6. create exactly one acceptance record binding the exact capability, R6.9 evidence chain, and
   `promotingBuildCommit=X`;
7. verify exact capability and exact embedded-commit evaluation succeeds;
8. verify provider-free ordinary routing eligibility;
9. make no provider request.

If composition requires the record to exist before a final restart, it may be created after the
image identity is verified and before that restart; it must still bind X exactly. This does not
create circularity because X is fixed by the already committed implementation and built image.

## 9. No ancestry service

Parker must not add a runtime Git-ancestry service for this acceptance decision. Production needs no
`.git` directory, Git executable, repository network access, branch name, reference lookup or commit
graph. The embedded manifest commit is sufficient and authoritative.

## 10. R6.10C readiness

The evaluator can deterministically obtain the record commit and current embedded commit and compare
them without Git history. Tests shall cover exact match, mismatch, missing, malformed and ambiguous
manifest identity, plus the rule that otherwise identical build Y is not accepted by record X.

Unresolved build-provenance states: **0**. R6.10C is ready under R6.10B, R6.10B1, R6.10B2, and this
amendment.
