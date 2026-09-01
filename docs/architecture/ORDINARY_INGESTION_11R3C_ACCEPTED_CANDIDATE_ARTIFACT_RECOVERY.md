# OI11R3C — accepted candidate artifact recovery

## Starting state

Investigation ran on Ubuntu host `parker`, repository
`/home/steve/parker-platform`, branch `main`, with HEAD/upstream
`1cfff58f0f804af71b970523a7fe5d5e67f207db` and a clean worktree. Required
candidate digest:
`sha256:2475b2765c0f75731d8ada46272d8a55553ea0c85e98dd6ad620da00cf5db836`.

The production container remained stopped as found:
`17b19463f65beb22117185d34010369644717b3666c58b7937ee512941c7fd8b`, image
reference `sha256:2475b276…`, restart count 11. No service was started or
recreated.

## Search surface and findings

Read-only checks covered Docker image listings (including dangling images),
`docker system df`, image inspect/history, stopped-container metadata and
diff, BuildKit cache metadata, repository history, Parker repository and
deployment configuration, `/home/steve` and `/mnt/parker-data` archive
locations, and local registry containers/configuration.

Docker has no image object for the required digest; `docker image inspect` and
`docker image history` fail with “No such image”. The stopped container retains
only the stale image reference and no usable image metadata/rootfs graph.
BuildKit cache output contains no candidate digest. Repository references are
limited to OI11R3A documentation. Parker archives found are persistent-store
or source-repository backups, not Docker/OCI image exports. No local registry
container or accepted registry endpoint was found. No backup/snapshot exposed a
selectively recoverable image; restoring a VM/filesystem snapshot would be a
destructive rollback and was not attempted.

## Authenticity and production integrity

No artifact was recovered, loaded, retagged, rebuilt, or substituted. Therefore
there is no recovered image digest or new manifest/source-identity proof. The
candidate’s previously recorded embedded source identity remains
`ae63687eb5bea5832ff3c4904920540150da202a`, but source identity alone cannot
recover the required exact image digest.

Production state was not mutated: container status and restart count remained
unchanged, persistent counts remained the recorded baseline
`4 / 2 / 1 / 21 / 19 / 6 / 5`, and provider calls (OpenAI, Claude, external,
OCR/transcription) and retries were all zero.

## Root outcome

**NOT RECOVERABLE — EXACT ACCEPTED CANDIDATE ARTIFACT NOT FOUND**

The exact candidate is irrecoverable from the Parker-controlled Docker state,
archives, caches, repository records, and available registry surface examined
here. A fresh rebuild is not equivalent under the current exact-image gate.

## Next unit

Define the separately governed replacement-candidate acceptance path before any
rebuild or deployment. That decision must explicitly choose whether to require
reproducible image identity, accept a newly built candidate through a new
exact-build acceptance record, or amend the governed promotion identity model.
