# Hermes Processing Service v1 deployment support

This directory contains reviewable deployment material only. Nothing here
installs a key, edits `authorized_keys`, enables routing, or deploys a service.

Stage A later installs the fixed entrypoint at:

```text
/usr/local/libexec/hermes-processing-v1-entrypoint
```

The service is invoked by a dedicated `authorized_keys` forced command. The
Parker private key is expected only in Parker secret storage; no private key is
generated or copied by these templates.

Required Hermes-owned directories are owner-only and separate from Parker
evidence:

```text
/var/lib/hermes-processing-v1/ledger
/var/lib/hermes-processing-v1/workspace
```

The remote route remains disabled until `HERMES_PROCESSING_V1_ENABLED=true` is
explicitly configured and the owner-approved deployment gate passes.

The synthetic canary helper accepts `txt`, `structured`, and `spreadsheet`
profiles plus `wrong-hash`, `malformed-frame`, `disabled-ocr`, and
`disabled-transcription` negative profiles. It validates fixture identity and
format only; the approved Parker transport harness performs the live chain.
