# OWNER-IDENTITY-HARDENING-1 — Opaque Owner Principal and External Verification Credential Boundary

## Status

IMPLEMENTED — PROSPECTIVE SECURITY AND PRIVACY HARDENING; NOT DEPLOYED

## Starting state and scope

- Branch: `main`.
- Starting HEAD/upstream: `86c2faa5d661aa1d24c2a322df32eed461a4f3fc`, exact and equal.
- Starting worktree: clean.
- Scope: prospective owner identity/authentication hardening for the governed human-correction authority path.

No historical commit, canonical document, governed record, provider derivative, human review, correction value, offset, provenance fact, or eligibility rule was rewritten.

## Existing identity mechanism reused

Parker's existing `PrincipalId` remains the canonical identity primitive. The correction authority now wraps it in `OpaqueOwnerPrincipal`, which binds role `OWNER` to a stable opaque identifier in canonical form `owner-<64 lowercase hexadecimal characters>`. No legal name is required or accepted by this high-authority boundary.

The general historical owner configuration remains intact. A separate prospective runtime setting, `PARKER_OWNER_HIGH_AUTHORITY_PRINCIPAL_ID`, supplies the opaque principal for correction authority without reinterpreting historical principal facts.

Identity and authentication remain separate: possession of the opaque identifier alone grants no authority.

## External-secret mechanism

The correction-capable runtime now requires `PARKER_OWNER_HIGH_AUTHORITY_VERIFICATION_CREDENTIAL_FILE`. Docker Compose provides it as the read-only secret `/run/secrets/parker_owner_high_authority_verification`, sourced from the operator-selected protected host path named by `PARKER_OWNER_HIGH_AUTHORITY_VERIFICATION_SECRET_FILE`.

The runtime loader:

- rejects missing, non-regular, symlinked, blank/undersized, and excessive credential files;
- reads the credential only into an ephemeral non-serializable `OwnerVerificationCredential` boundary;
- uses constant-time byte comparison;
- renders the credential only as `[REDACTED]`;
- never logs or adds credential material to domain or audit records.

No password, token, secret, private key, verification code, or reusable credential is stored in Git.

## Exact verification binding

Before the existing permission engine is invoked, `HumanCorrectionPermissionPolicy` now requires all of:

1. the configured opaque `OWNER` principal;
2. active purpose `document-ingestion.human-transcription-correction`;
3. the exact correction target resource derived from evidence ID, source digest, preparation identity, provider generation ID/digest, provider content digest, and canonical review ID;
4. a valid externally supplied verification credential.

Wrong principal, wrong/missing purpose, wrong target/review, missing credential, and wrong credential deny before governed correction mutation. The existing permission engine and exact action `human-transcription-correction.create` remain an additional required authorization layer. There is no owner-wide authenticated session shortcut.

## Audit and privacy behaviour

Existing correction audit facts retain the opaque actor `PrincipalId`, exact representation/target/review/acceptance/content identities, factual event type, and timestamp. Credential material is structurally absent from `HumanCorrectionAuditRecord` and its serialization. The verification credential's string representation is redacted.

Prospectively, new code, tests, audit facts, and architecture reports for owner operations must use opaque identity and avoid a legal name unless a genuine legal/documentary requirement exists. Accepted historical records and documents are preserved rather than rewritten.

## Correction semantics and compatibility

The immutable provider representation, canonical review, exact literal-region offset fix, correction proposals/acceptance, deterministic corrected content, create-once persistence, provenance, retrieval, and source-confirmed eligibility semantics are unchanged. Existing correction-service tests remain green.

Production composition now fails closed when correction storage is configured without both the opaque high-authority principal and external credential file. No public correction endpoint or broad owner session was added.

## Focused verification

Focused tests covered:

- correct opaque owner, valid external credential, exact purpose, and exact target authorization;
- missing and wrong credential denial before permission evaluation;
- wrong principal, purpose, target, and review denial;
- inactive purpose and downstream permission denial;
- constant-time protected-file credential verification and redacted rendering;
- absence of secret material from audit identity/records;
- production composition with an isolated external secret file;
- existing governed correction creation, literal-offset, durability, and eligibility regressions;
- runtime configuration loader compatibility.

Result: 4 suites, 66 tests, 0 skipped, 0 failures, 0 errors. Per unit direction, no ceremonial full-suite rerun was performed; the existing green full-suite baseline remains unchanged.

## Production and provider boundary

- Deployment: not performed.
- Production restart: not performed.
- Production governed-state delta: 0.
- OpenAI calls: 0.
- Claude calls: 0.
- Other provider calls: 0.
- Retries: 0.
- External evidence egress: 0.

Deployment will require a separately governed operator provision of an opaque high-authority principal and protected secret file. This report contains neither value.
