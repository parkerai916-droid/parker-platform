# FA.9.4P-A1 durable region provider-state persistence and restart recovery

**Status:** Implemented and offline-verified on Ubuntu. No provider call, acceptance invocation, production-store mutation, runtime rebuild, or deployment is part of R6.5.

## Boundary and identity

`filesystem-region-provider-state-v1` persists every bounded HTTP response immediately after transport returns and before status interpretation, parsing, validation, admission, or reconstruction. Timeout and transport failure create no response record. The stable record ID is SHA-256 over correlation ID, provider, adapter ID/version, and the deterministic secret-free request digest. Time is metadata only and is not authority.

The proposed later production root is `/data/external-region-provider-state`, configured by a future composition/deployment unit. R6.5 does not create or mount it.

The request digest canonically binds correlation, evidence artifact and source digest, page representation/page number, ordered SourceRegionIds, crop and encoded-image digests, context presence/digest, transcription and processing profiles, instruction/schema digests, provider/model, adapter/version, `reasoning=none`, `store=false`, and `image_detail=original`. Credentials and transport headers are excluded.

## Records, canonical form, and integrity

The immutable response record contains exact bounded raw bytes as base64, byte length, SHA-256, HTTP status, nullable content type, requested provider/model, adapter/parser identities, request binding/digest, and receive time. A separate immutable assessment sidecar contains the parser/validator outcome and, when JSON parsing succeeds, the exact structured map in provider-returned array order. Thus parser failure, validation failure, and refusal never erase the factual response. A finalized response without a sidecar is explicitly `downstreamProcessingPending=true` after restart.

Canonical serialization is compact UTF-8 JSON with insertion-defined stable field order, stable enum strings, explicit JSON null, locale-independent numbers, and literal JSON escaping. It preserves Unicode, whitespace, block order, transcription strings, uncertainty, warnings, observations, ordinals, and provenance. Separate SHA-256 values cover raw bytes, canonical structured state, the complete factual response record, and the assessment sidecar.

## Durability, immutability, recovery

Each file is written to a private same-filesystem temporary file, fully written, file-`fsync`ed, atomically renamed without replacement, and followed by containing-directory `fsync`. Final records are create-once. An identical already-final file may be read; persistence never overwrites it. Any duplicate persistence attempt that differs (including metadata) is a conflict and fails closed. Incomplete `.tmp` artefacts are ignored by enumeration and never admitted.

On restart a new store object enumerates only finalized response records, validates canonical record checksum, request binding digest, raw length/digest, assessment bindings/checksum, and structured digest, and returns exact raw/structured state. Truncation, altered raw or structured content, checksum changes, request-binding changes, malformed serialization, and conflicting duplicates fail closed. Existing response state makes `responseExistsFor(request)` true, which is the no-retransmission fact for a future coordinator. R6.5 intentionally does not add retry or production execution composition.

## Limits, privacy, and security

Raw response maximum is 20 MiB, structured canonical state maximum is 16 MiB, and each stored file maximum is 32 MiB. The R6.3 contract independently caps requests at 32 regions, literal text at 100,000 code points per block, 200 uncertainties and observations, 50 warnings, warning length 1,024, and uncertainty alternatives at eight of 256 characters. Only synthetic fixtures are committed; raw bodies are never logged.

Directories and files use owner-only POSIX permissions where supported (`0700` directories, `0600` files). Storage is plaintext evidential derivative data and therefore requires a production service account and restricted mount. Encryption at rest is unresolved and must be decided before deployment based on host/volume controls; R6.5 invents no new cryptosystem.

## Attempt ledger and compatibility

The existing FA attempt ledger answers which execution stage was reached. This store answers exactly which response was received. No historical Unit O/FA stage or record changes. A future production composition may add a `PROVIDER_STATE_PERSISTED` stage, but R6.5 requires no new ledger stage: durable response existence is already independently provable. Historical direct-PDF records, R6.4 adapter behavior when no store is injected, and failed generations remain untouched and readable.

## Next prerequisite

The recommended next bounded unit is production-composition design and offline verification: configure the future root, connect response existence to the attempt coordinator's no-retry decision, define service-account/backup/encryption controls, and prove restart sequencing without enabling provider authority or deployment. Live region-aware acceptance remains separately governed.
