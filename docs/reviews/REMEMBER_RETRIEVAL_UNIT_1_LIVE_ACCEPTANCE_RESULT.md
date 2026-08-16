# Unit 1 Live Acceptance Result

## Purpose

The manual acceptance test was intended to determine whether an explicit public owner REMEMBER request traverses the live conversational path into durable Memory Core and Knowledge Item persistence, so that the remembered information can subsequently be tested across runtime restart and retrieval.

## Tested implementation

Branch:
implementation/remember-retrieval-unit-1-durable-knowledge

Commit:
c83747984b89162c0f768c00b3dab42afc232819

Model:
qwen2.5-coder:7b

The test used an isolated Docker Compose project named:

parker-unit1-test

The test runtime used isolated Memory Core, Knowledge Item, Evidence Storage, and Evidence Audit volumes. It shared only the existing Ollama inference service.

The existing Parker runtime and its durability state were not modified.

The model timeout was set to 90000 ms because the initial 30000 ms attempt encountered the known Qwen cold-start timeout.

## Manual test

The runtime was started interactively through the public owner-message path.

The owner submitted exactly:

"Remember that the Parker server has 16 GB of RAM."

Parker responded:

"The Parker server has 16 GB of RAM. Is there anything specific you need me to remember regarding this information?"

Runtime A remained running while durability state was inspected.

## Observed durable state

The isolated Knowledge Item durability log existed but had size:

0 bytes

The isolated Memory Core durability log existed but had size:

0 bytes

The same zero-byte state was independently observed:

1. through read-only mounts of the Docker volumes; and
2. from inside the running Parker container.

Container logs confirmed that the public owner message was received and the quoted Parker response was produced.

No Memory Core durable record was created.

No Knowledge Item durable record was created.

## Result

LIVE ACCEPTANCE FAILED.

Unit 1 durability infrastructure is verified by the existing automated test suite, including restart/recovery composition coverage, but the live public REMEMBER acceptance path failed upstream of durable persistence.

The explicit owner instruction did not result in a durable Memory Core write or durable Knowledge Item write.

Because no durable memory was created, the planned Runtime B restart/retrieval portion of the acceptance test was not performed. Performing it could not test recovery of the requested memory because there was no persisted record to recover.

## Failure boundary

The evidence establishes that the failure occurred before durable persistence.

It does not establish that DurableKnowledgeItemPersistence, FileSystemKnowledgeItemDurabilityLog, Memory Core durability, or restart recovery is defective.

The live model produced a conversational response, but the explicit owner REMEMBER instruction did not reach durable admission/promotion.

The exact upstream cause has not yet been determined.

## Consequence

Unit 1 must not be treated as live end-to-end accepted.

Units 2–5 must not be started merely on the assumption that public REMEMBER is operational.

The next work must investigate the live decision boundary between model output, structured REMEMBER interpretation, conversational memory admission, and promotion sufficiently to determine why the explicit owner REMEMBER request did not invoke persistence.

That investigation must distinguish the live-model/protocol failure from the already-tested durability implementation and must not modify durability merely because the live acceptance test failed.

## Automated verification already completed

./gradlew test
BUILD SUCCESSFUL
tests=2150 failures=0 errors=0 skipped=5

./gradlew check
BUILD SUCCESSFUL

docker compose config --quiet
passed

git diff --check
passed

The five skipped tests were existing opt-in cases. No live-model campaign was invoked during the automated Unit 1 verification.

## Acceptance status

AUTOMATED DURABILITY VERIFICATION: PASS

LIVE PUBLIC REMEMBER ACCEPTANCE: FAIL

RUNTIME RESTART/RETRIEVAL MANUAL ACCEPTANCE: NOT RUN — BLOCKED BY ABSENCE OF PERSISTED MEMORY

UNIT 1 OVERALL LIVE ACCEPTANCE: NOT ACCEPTED
