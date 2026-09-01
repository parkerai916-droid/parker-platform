# OI11R4C — V8 Capability Promotion Endpoint/Codec Correction

## 1. Baseline and defect

OI11R4B proved that the mounted capability store contained only legacy `.region-capability-acceptance-v2` records and that the canonical promotion parser rejected the V8 identity before the existing V8 coordinator could be reached. OI11R4A consequently stopped with `CAPABILITY_NOT_ACCEPTED` before authorization reservation or provider transport.

The affected route is authenticated `POST /owner/admin/region-capability-acceptance`. Its request codec previously required `ORDINARY_REGION_CAPABILITY_ID` (the legacy family) unconditionally. `ParkerRuntime` already composed `OrdinaryRequestRegionV8CapabilityAcceptanceCoordinator`, but the route could not deliver a V8 promotion request to it.

## 2. Correction

The parser now accepts exactly the two already governed identities: the legacy `ORDINARY_REGION_CAPABILITY_ID` and `ordinary-external-request-region-transcription-v8`. It still requires a 40-character lowercase commit and rejects unknown or malformed identities. `ParkerRuntime` now dispatches V8 requests to the existing V8 coordinator and legacy requests to the existing legacy coordinator, preserving the two record families and delegating acceptance decisions rather than deciding them at the endpoint.

The V8 coordinator continues to require the exact capability digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, exact runtime commit binding, fixed V8 acceptance evidence identity, and its `.request-region-v8-capability-acceptance-v1` writer. Legacy records remain `.region-capability-acceptance-v2` and are neither migrated nor reinterpreted.

## 3. Verification

Focused `OwnerEvidenceHttpServerTest` passes. It proves authenticated legacy dispatch, authenticated V8 dispatch, unknown-capability rejection, and bounded request rejection. The complete `./gradlew test --no-daemon` suite passes with zero failures and zero errors.

Production was not restarted or redeployed. The running container remains on image `sha256:9268d5d1685f6760cc6daea7fb40000c437584ec2721156a40143266530a3ec7`, restart count 0. The production acceptance directory remains six legacy records and no V8 record; evidence, manifest, authorization, attempt, provider-state, derivative-generation, and derivative-content counts remain `28 / 28 / 5 / 4 / 2 / 21 / 19`. OI11R4C made zero provider calls, zero retries, and zero external egress.

## 4. Source/artifact boundary and next unit

The corrected source is committed as `5f306231da609e233354f6052c610cb5fb5a4c4a`, but the running production artifact was not rebuilt and therefore does not contain this correction. No V8 production acceptance record was created or modified. The next governed unit is **OI11R4D — corrected artifact build/acceptance and production deployment preparation**, followed by the separately governed V8 capability-acceptance operation and a new synthetic live-execution unit. OI11R4A must not be retried from this unit.
