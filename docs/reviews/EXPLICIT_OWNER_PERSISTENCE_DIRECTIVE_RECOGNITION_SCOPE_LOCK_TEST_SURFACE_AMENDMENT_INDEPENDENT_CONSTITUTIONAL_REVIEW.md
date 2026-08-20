# Explicit Owner Persistence Directive Recognition Test Surface Amendment — Independent Constitutional Review

**Status:** Independent Constitutional Review.

**Verdict:** **ACCEPTED**

## 1. Review boundary

This review independently examined the amendment, the adopted parent Scope Lock,
the actual implementation diff, and the affected existing reasoning-context tests.
It authorizes no production behavior and changes no prior governance.

## 2. Necessity

The parent Scope Lock requires zero delegate calls for a qualifying explicit owner
directive. Three existing integration scenarios use explicit `Remember` turns to
create genuine promoted memories, but their stub fixtures and assertions count
those former model calls. Under the governed decorator those setup turns correctly
bypass the model, so the old request counts and response positions are necessarily
stale. This is a real test-surface dependency, not a production defect.

## 3. Constitutional challenge

The amendment does not weaken the tests' substantive guarantees. It preserves:

- real `ParkerRuntime.submitOwnerMessage` entry;
- real `ConversationReplyCoordinator` and `MemoryAdmissionCoordinator` reuse;
- real permission, Memory Core, Knowledge Submission, promotion, and durability;
- real later-query model invocation and assembled-prompt inspection;
- literal retrieval success and paraphrase/QMD-control expectations; and
- the absence of synthetic Knowledge Items or direct persistence seeding.

It changes only the obsolete assumption that deterministic setup directives still
consume sequential stub responses. Requiring those calls would directly conflict
with the accepted zero-delegate guarantee and would permit the model back into the
intent decision the Scope Lock removes from it.

## 4. Confinement

The amendment names exactly one additional test file and prohibits changes to
unrelated continuity/history cases in that file. It authorizes no production,
QMD, persistence, parser, prompt, model-client, UI, Docker, or durability change.
The acceptance conditions require the later non-directive query to reach the model
exactly once, so the amendment cannot mask an accidental general model bypass.

## 5. Findings

No retroactive production authority, weakened evidential/admission assertion,
test-only persistence bypass, or scope expansion was found. The amendment is the
narrowest lawful correction for the discovered full-suite dependency.

```text
VERDICT=ACCEPTED
REQUIRED_CORRECTIONS=NONE
```

The amendment may be adopted and committed independently of implementation. Only
after that adoption may the named expectation-only test changes be reapplied.

No implementation file was modified by this review. Nothing was staged, committed,
or pushed.
