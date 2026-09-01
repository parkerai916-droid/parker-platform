# OI11R3D — replacement production candidate acceptance rule

## Problem and governing finding

The accepted OI11R2 source identity is
`ae63687eb5bea5832ff3c4904920540150da202a`. Its first production candidate
was lost, and OI11R3A proved that repeated builds of the same source produced
different Docker image digests while retaining the embedded source identity.
Therefore source lineage alone cannot prove that a newly built executable is
the lost immutable artifact. No build, deployment, provider call, or
production mutation occurred in this unit.

Existing Parker implementation establishes the layers rather than one generic
version: `PARKER_BUILD_COMMIT` is embedded by Gradle as
`Parker-Source-Commit`; `ParkerRuntime` reads that manifest identity;
`PARKER_SOURCE_COMMIT` and `PARKER_PRODUCTION_COMMIT` are deployment claims
that must match it; and capability acceptance, owner authorization, provider
state, and region authorities remain separate governed records. Startup is
fail-closed on a mismatch. Prior ordinary-ingestion promotion documentation
also requires exact-build equality and explicit acceptance evidence.

## Identity model

* **Source identity** (`ae63687…`) identifies the reviewed Git source lineage.
  It proves which source was accepted, not which bytes a new build produced.
* **Artifact identity** is the concrete packaged JAR/executable and OCI/Docker
  image identity (image ID, manifest/config digests, and immutable digest).
  It identifies what will run; it is not interchangeable with a source commit.
* **Capability identity** is the accepted V8 capability ID and digest. It fixes
  schema, profile, wire, adapter, parser, processing, and provider semantics;
  it is independent of both source and image identity.
* **Deployment acceptance identity** is the explicitly configured source/build
  commit plus immutable image reference. `PARKER_SOURCE_COMMIT` declares the
  source lineage and `PARKER_PRODUCTION_COMMIT` gates runtime composition;
  neither may be wildcarded or auto-learned.
* **Runtime identity** is the running image/container digest together with the
  embedded manifest commit and loaded deployment configuration. It proves the
  process is executing the accepted artifact under the accepted configuration.

## Docker reproducibility

No existing Parker rule makes Docker bit-for-bit reproducibility a constitutional
requirement. The observed digest variance is consequently not itself a reason
to reopen OI11R2 or weaken startup validation. It does create an artifact
traceability and recovery risk: every accepted production candidate must be
preserved immutably before deployment. A stable JAR digest is useful additional
evidence, but cannot replace the concrete image digest required to prove what
Docker will execute.

## Threat model

Replacement acceptance must prevent wrong-commit builds, dirty or uncommitted
source substitution, dependency/build-context drift, altered build settings,
mutable-tag substitution, post-build artifact replacement, deployment of an
untested image, and loss of rollback material. A claimed embedded commit is
necessary evidence but is not sufficient because it can be reproduced by an
untrusted build process.

## Chosen model

**Model C — Layered acceptance.** OI11R2 source acceptance remains authoritative
and is not reopened when source content is unchanged. Every replacement image
must receive a new bounded artifact-acceptance record tying the accepted source,
verified build, concrete artifact digests, verification evidence, and explicit
authority. Deployment consumes only that accepted artifact.

Required evidence bundle:

1. exact source commit and clean-worktree/upstream proof;
2. verified OI11R2/OI11R3 governance baseline and full test result;
3. exact build command, host, toolchain/base-image and dependency resolution;
4. embedded `Parker-Source-Commit` matching the accepted source;
5. packaged JAR/executable SHA-256;
6. Docker image ID, OCI manifest digest, config digest, and immutable image
   reference, with mutable-tag resolution recorded only as supporting evidence;
7. exact capability ID/digest and deployment configuration identity;
8. Compose render showing the image and source/build values selected for
   deployment;
9. zero provider-egress evidence during build and verification;
10. an immutable Parker-controlled archive (for example `docker save` OCI/tar)
    whose loaded digest is re-verified;
11. explicit acceptance record containing all above identities, timestamps,
    and verification-result digests; and
12. explicit human acceptance by Steven (or a later named authority), recorded
    separately from construction and verification.

Each field controls a distinct risk: source/build evidence controls lineage;
artifact digests control substitution; capability/configuration evidence
controls semantic drift; archive preservation controls recovery/rollback; and
human acceptance preserves separation of construction, verification, trust,
and deployment.

## Authority and preservation

Codex may construct and verify but may not self-authorize a replacement.
Steven's explicit acceptance act authorizes the artifact record; Parker then
persists it and deployment consumes that exact record. Artifact preservation is
mandatory for the immediate replacement candidate, before any deployment, in a
Parker-controlled persistent release/archive location with retention sufficient
for rollback and later recovery. The archive must be digest-verified after
creation and must not be confused with persistent evidence-store backups.

## Relationship to OI11R2 and OI11R3

OI11R2 source acceptance remains valid because the reviewed source is unchanged;
only the concrete replacement artifact requires fresh acceptance. OI11R3 must
not be retried until the new artifact is built, independently verified,
archived, explicitly accepted, and its deployment metadata binds both the
accepted source and exact image. OI11R3 then remains deployment verification,
not artifact acceptance.

## Exact next unit

**OI11R3E — replacement candidate build, preservation, and artifact-acceptance
unit.** It shall establish the exact accepted-source checkout, build one
candidate, collect the evidence bundle above, preserve and re-verify the
immutable artifact, obtain/record Steven's explicit acceptance, and stop before
deployment. Only after OI11R3E succeeds may OI11R3 be retried.
