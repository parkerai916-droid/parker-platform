# OI11R4B — V8 Capability Acceptance Reconciliation

**Status: Investigation complete — owner acceptance of the capability is not requested in this unit.**

## 1. Failure lineage and boundary

OI11R4A registered synthetic evidence `evidence-84d85f99-3a94-4101-86b2-8b8aa9aef0ae` (SHA-256 `f9ce327166b00c28d0ce50334bad256c03f2ff9c74e6b045592f53f8dce03a89`) and stopped before egress when the governed authorization endpoint returned HTTP 409 `CAPABILITY_NOT_ACCEPTED`. The one-call budget was not consumed. No provider attempt, retry, raw provider state, parse, derivative, or external egress occurred.

## 2. Authority chain

Production configuration mounts `PARKER_ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORAGE_ROOT` as `/data/region-transcription-capability-acceptances`. `ParkerRuntime` constructs `OrdinaryRequestRegionV8CapabilityAcceptanceCoordinator`, `FileSystemOrdinaryRequestRegionV8CapabilityAcceptanceStore`, and `OrdinaryRequestRegionV8AcceptanceEvaluator`. The owner authorization workflow calls that evaluator before creating an authorization reservation. The authenticated administrative route is `/owner/admin/region-capability-acceptance`.

The V8 store looks only for files ending `.request-region-v8-capability-acceptance-v1`, decodes the V8 record, and requires exact capability digest and exact 40-character runtime implementation commit. The mounted store inventory contained six legacy `.region-capability-acceptance-v2` records and no V8 acceptance record.

## 3. Legacy V2 semantics

The legacy records are `OrdinaryRegionCapabilityAcceptanceRecord` values carrying the earlier region capability's R6.9 evidence chain, legacy capability digest, legacy promoting-build commit, owner identity, and timestamp. Their filename/schema and digest are valid for that historical capability only. They are not V8 records and are not accepted by the V8 evaluator. Coexistence is intentional; the runtime preserves historical V5/V6 readback but does not use legacy acceptance to authorize V8.

## 4. V8 governance boundary

The programme accepted the V8 specification, implementation, artifact, deployment, and provider profile. That did not itself create a concrete V8 execution-acceptance record. The V8 implementation defines an explicit record type (`request-region-v8-capability-acceptance-v1`) and a coordinator that binds capability ID, capability digest, implementation commit, fixed acceptance evidence ID/digest, owner, and timestamp. Thus execution authority is a distinct acceptance layer.

## 5. Exact identity and lookup semantics

Required V8 identity is `ordinary-external-request-region-transcription-v8` with digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`. The evaluator obtains the embedded runtime commit, requires exactly 40 lowercase hexadecimal characters, then performs an exact V8-store lookup on `(implementationCommit, capabilityDigest)`. Missing, malformed, duplicate, corrupt, or mismatched records evaluate to `NotAccepted`. The authorization workflow maps that result to `CAPABILITY_NOT_ACCEPTED` before authorization reservation or provider transport.

## 6. Canonical promotion defect

The endpoint handler has a canonical promotion route and delegates to `ParkerRuntime.createOrdinaryRegionCapabilityAcceptanceAsOwner`, but `parseCapabilityPromotionRequest` currently validates the request against the legacy `ORDINARY_REGION_CAPABILITY_ID`. It therefore cannot accept a request whose capability ID is the required V8 ID. The V8 coordinator is present but unreachable through the canonical HTTP promotion request shape. This is a record-format/endpoint codec defect (classification D), with the observable runtime consequence of a missing V8 acceptance record. It is not evidence that fail-closed validation is wrong.

## 7. Fail-closed proof

Because no exact V8 record exists, the evaluator returns `NotAccepted`; the owner authorization endpoint returns `UNAVAILABLE`/`CAPABILITY_NOT_ACCEPTED`; no reservation or attempt is created; and no provider call can occur. Pending, missing, corrupt, wrong-digest, wrong-build, and legacy-only state remain rejected. No bypass or manual state mutation was performed.

## 8. Authority and smallest remedy

The existing authenticated owner administrative promotion mechanism is the intended authority boundary. The smallest next unit is **OI11R4C — V8 Capability Promotion Endpoint/Codec Correction**: update only the request parser/route contract so the existing authenticated mechanism can accept the V8 capability ID and exact runtime commit, then exercise the existing V8 coordinator provider-free, create one deterministic V8 acceptance record, and verify the evaluator and owner authorization path. That unit must not execute a provider call. It must preserve legacy V2 handling and retain fail-closed behavior. Any owner acceptance required by the existing promotion authority must be obtained there; none is inferred here.

## 9. Synthetic source and integrity

The registered synthetic source remains preserved and may be reused by a separately authorized retry if its manifest and SHA-256 continue to match the frozen Scope Lock. Production remains running on the accepted image with restart count 0. Programme store counts remain `4 / 2 / 1 / 21 / 19 / 6 / 5`. OI11R4B made zero provider calls, zero retries, and zero external egress.

**OI11R3 and OI11R4A remain unauthorized for retry until OI11R4C repairs the bounded promotion endpoint/codec defect and a V8 acceptance record is created through the governed mechanism.**
