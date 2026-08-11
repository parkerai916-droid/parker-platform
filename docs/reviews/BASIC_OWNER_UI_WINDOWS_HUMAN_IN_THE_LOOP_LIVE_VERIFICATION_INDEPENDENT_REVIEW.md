# Basic Owner UI Windows Human-in-the-Loop Live Verification Independent Review

Date: 2026-08-11 (Pacific/Auckland)

## Independent verdict

**ACCEPTED WITH QUALIFICATIONS** as an accurate **C — PARTIALLY VERIFIED** record. It is not accepted as full end-to-end verification or merge-readiness evidence.

## Adversarial review

1. **Human versus technical evidence:** The review uses Steve's report only for window visibility, visible Runtime/Ready wording, and control availability. Runtime startup/shutdown, fixture identity, process state, RAM, and call counts come from technical evidence. Neither evidence class is substituted for the other.
2. **Real UI:** The repository's actual `:ui-desktop:runOwnerUi` task launched, not `OfflineOwnerUiMain` or a test harness. Human observation confirms an actual visible Parker window with Parker Runtime/Ready wording.
3. **Real runtime:** `owner-ui` logs independently prove `ParkerRuntime` started and later stopped. Static source inspection proves the launcher's composition, but no claim is made that the submission path ran.
4. **Submission:** Zero owner submissions occurred. Typing text into the draft field is not misrepresented as submission.
5. **Model path:** Zero `/api/generate` calls occurred, no `llama-server.exe` loaded, and no Parker/model output exists. The review correctly withholds all inference/parser/outcome claims.
6. **Bypass:** No direct HTTP call, direct runtime call, synthetic event, shell automation, accessibility automation, or alternate UI was used.
7. **Notification provenance:** `OwnerNotificationSink` and `OwnerUiNotificationBridge` were not exercised; no Parker speech is claimed.
8. **Presentation boundary:** Steve observed truthful Runtime/Ready presentation and reported no forbidden diagnostics before submission. Post-outcome safety is untested because no outcome occurred.
9. **Resource halt:** The 1.308 GiB measurement is objective and materially below the 2.0 GiB safety floor. Withholding submission was correct and prevents unsafe completion pressure from being reframed as test success.
10. **Semantic interpretation:** No model response occurred, so neither architectural success nor failure, semantic quality, model qualification, or Reasoning Protocol conformance is inferred.
11. **Shutdown:** Steve used the normal window close. Runtime logs and JVM exit prove the intended shutdown path executed; no forced UI termination was required.
12. **Isolation:** Parker server, Unit 3-C, paused remedies, production reasoning, and remote/cloud inference remained untouched.

## Evidence sufficiency and remaining blocker

The evidence now supports:

- real human-visible Basic Owner UI launch;
- truthful Parker Runtime/Ready pre-submission presentation;
- available owner controls;
- real ParkerRuntime startup;
- normal owner-driven runtime/UI shutdown;
- correct local fixture identity and isolation.

It does not support:

- `OwnerUiController` submission traversal;
- `OwnerUiRuntimeAdapter`/`submitOwnerMessage` traversal;
- a Parker-originated `/api/generate` call;
- tagged parsing or governed outcome handling;
- `OwnerNotificationSink` provenance;
- visible Parker/System result safety.

The missing links are essential, so Classification A or B would overclaim. Classification D would understate the newly demonstrated visible startup/shutdown portions. **C — PARTIALLY VERIFIED** is calibrated.

## Merge-readiness determination

**NOT READY FOR FINAL MERGE REVIEW.** A final human-in-the-loop attempt still needs enough pre-submission RAM to execute exactly one message through the real path. The next attempt should add an explicit 2.0 GiB gate immediately before submission, preserve all existing boundaries, and make no production change.
