# OI11R4H — V8 Authoritative Provider Provenance Enrichment and Production Convergence

## Verdict

`PASS — OI11R4H COMPLETE`

## Correction and tests

The bounded adapter correction was committed as `d33518e85604083d620be08be4a4f001d7be3187`. It enriches nullable structured provenance from authoritative outer-response ID/model metadata before validation, rejects conflicting non-null values, preserves requested-model checks, and retains raw-before-parse ordering. Focused V8 enrichment/conflict/missing-envelope tests and the full Gradle suite passed with zero reported failures/errors.

The historical OI11R4F response and failure state remain immutable and were not replayed for admission.

## Accepted candidate

Steven accepted the exact candidate built from source `d33518e85604083d620be08be4a4f001d7be3187`:

- JAR SHA-256: `224b2fb3ed00589b2285e7620f6f03feb1ef1477460cd98d98733449812b0332`
- Embedded source: `d33518e85604083d620be08be4a4f001d7be3187`
- Docker image/index digest: `sha256:5ca82f03ce61eade58c60eb4d3783547b4b266f974ed2ac218c09cf43f86075a`
- OCI config digest: `sha256:eefedbba420d375edd354e5055b3b68b3ae3edcdf676427f8b81a07299eb3f7a`
- Archive: `/mnt/parker-data/parker/replacement-candidates/oi11r4h-provenance-enriched-d335-20260901.tar`
- Archive SHA-256: `875da26422a220bcd1fad29c18d0aadb0444eaae2a6414b6ab54bd79dfe7d214`

The Docker image ID/RepoDigest is the immutable local OCI image-index identity; its archive contains the corresponding config digest and layers. Production binding uses that exact digest, not a mutable tag.

## Acceptance and deployment

Artifact acceptance record: `/mnt/parker-data/parker/replacement-candidates/oi11r4h-artifact-acceptance-5ca82f03-v1.json`; SHA-256 `ad388ba75d04301112c0465ff412e32fe380783b281e9944c3ae5be113d6c025`.

Compose rollback preimage: `/home/steve/.config/parker/docker-compose.fa-a1r.yml.oi11r4h-preimage`; SHA-256 `1a8aa4f507d8a9188afce38149d123b0ee9fd2d7ca945f935feec41e1bc8a1b1`. Final render SHA-256: `f61fca20a58bf74cbc5aa6e2bda4c625a9c3f932f3d1fdcccb49d1fab7718926`.

Deployment used the existing no-build/no-pull path:

```text
PARKER_BUILD_COMMIT=d33518e85604083d620be08be4a4f001d7be3187 docker compose -f /home/steve/parker-platform/docker-compose.yml -f /home/steve/.config/parker/docker-compose.openai-enablement.yml -f /home/steve/.config/parker/docker-compose.fa-a1r.yml up -d --no-build --pull never --no-deps --force-recreate parker
```

## Production verification

Running container: `c3b739bdec37e594b63fe35bad43c4be545f75568db7b431b81b747e1b3c85b6`; status `running`; restart count `0`; image `sha256:5ca82f03ce61eade58c60eb4d3783547b4b266f974ed2ac218c09cf43f86075a`. Startup completed without readiness or configuration rejection. The running JAR embeds the exact 40-character source identity.

The corrected adapter classes and V8 readiness implementation are present in the running artifact. No V8 capability-acceptance record was rewritten; the existing single V8 record and six legacy records remain unchanged.

Store counts remain `4 / 2 / 1 / 21 / 19 / 6 / 5` for evidence, manifests, derivative generations/content and related governed stores; deployment created no evidence, provider-state, attempt, or derivative records. Provider accounting is OpenAI `0`, Claude `0`, other external `0`, retries `0`. The OI11R4F raw response/failure remains preserved.

## Boundary

Production now contains the provenance-enrichment correction and is stable. No provider transaction was executed. The next unit is a new separately governed synthetic V8 execution with a fresh one-call budget.
