# Basic Owner UI Unit 3 Planning Review

## Decision

Unit 3 is a verification-and-closure unit. Units 1 and 2 already implement the required presentation behaviour and outcome completeness. No production implementation gap was found.

## Gap matrix

| Concern | Classification | Evidence or Unit 3 action |
|---|---|---|
| Ready, Processing, Stopped, Error | Complete | `OwnerUiController` has explicit structural states and terminal cleanup. |
| Delivered | Complete | Remains transcript-silent; Parker text cannot be inferred from a disposition. |
| NotAccepted, Failed, Planned, Unavailable | Complete | Each is rendered as typed System status; Failed and Unavailable select Error and Stopped. |
| Owner, Parker, System authorship | Complete | Sealed transcript entries preserve authorship; Parker entries enter only through the reply callback. |
| Safe unexpected errors | Complete | A fixed presentation-safe message replaces exception content. |
| Initial and outcome-driven availability | Complete | Initial STOPPED and Unavailable outcomes are supported. |
| Proactive arbitrary availability observation | Not required | `OwnerInteraction` deliberately exposes a snapshot property, not an event stream. Adding one would expand the approved boundary. |
| Reply-before-disposition ordering | Partially complete | Implemented by the offline script; add direct transcript-order regression evidence. |
| Disposition-before-later-reply | Not required | The suspend contract completes after callback delivery; no deferred reply contract exists. |
| Sequential repeated submissions | Partially complete | Controller cleanup permits them; add direct regression evidence. |
| Error recovery and stale-state clearing | Partially complete | A new accepted submission selects Processing and successful completion selects Ready; add direct evidence. |
| Stop persistence and rejected later submission | Partially complete | Implemented; add direct evidence that no later Owner entry or adapter call occurs. |
| Cancellation and shutdown | Complete | Owned work is cancelled and state remains structurally Stopped. |
| Disabled visual/input state | Complete | Unit 2 presentation derives submission eligibility from structural state. |
| Scrolling and transcript order | Complete | Scrolling is presentation-only and never mutates the controller transcript. |

## Scope determination

Only focused tests and Unit 3 governance/review records are warranted. Production Kotlin, Compose structure, dependency versions, application entry points, runtime composition, and the existing Ubuntu testing environment are out of scope.

## Verification plan

1. Add focused offline tests for sequential submissions, Error recovery, initial stop, reply ordering, and stop persistence.
2. Run those tests alone.
3. Run the complete offline root and isolated desktop verification set.
4. Run repository hygiene and forbidden-boundary scans.

