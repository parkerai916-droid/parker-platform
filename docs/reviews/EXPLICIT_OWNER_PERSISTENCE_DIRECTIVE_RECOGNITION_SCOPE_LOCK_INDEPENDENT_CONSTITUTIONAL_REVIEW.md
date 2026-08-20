# Explicit Owner Persistence Directive Recognition — Independent Constitutional Review

**Status:** Genuine Independent Constitutional Review.

**Verdict:** **ACCEPTED**

## 1. Review posture and baseline

This review was performed independently against the proposed Scope Lock's actual
text, the current repository source, and the governing documents re-read fresh.
It does not amend or implement the Scope Lock.

```text
branch=main
HEAD=61ef6078c72bafd1e059dea8a8f42bb230d66b36
origin/main=61ef6078c72bafd1e059dea8a8f42bb230d66b36
pre-review-status=only the proposed Scope Lock was untracked
```

## 2. Authorities and implementation reality checked

The review checked the adopted Programme 3 Explicit Owner Instruction Promotion
Exception Scope Lock Clarification and its accepted ICR, the Conversational
Memory Admission Implementation Plan and accepted reviews, the Reasoning
Provider contract and amendment, the Communication and Conversation contracts,
and the present reasoning, conversation, admission, and composition source.

The demonstrated implementation premise is accurate. A conversational turn is
wrapped in `ReasoningSubject.OfTurn`; production currently composes
`LoggingReasoningProvider(ModelReasoningProvider(...))`; the model/parser
therefore selects `Remember` probabilistically. The existing
`ConversationReplyCoordinator` already routes a `Remember` through
`MemoryAdmissionCoordinator` and the separately governed permission,
provenance, knowledge-evaluation, persistence, and durability path.

## 3. Constitutional challenges

### 3.1 Does deterministic recognition invent persistence authority?

No. The proposed decorator produces only the already-governed
`ReasoningProviderResponse.Remember`. It cannot write, authorize, evaluate,
promote, or persist. The existing admission and permission gates remain the
only route to durable state, and denial remains authoritative.

### 3.2 Does it conflict with the adopted promotion exception?

No. The adopted clarification requires direct, unambiguous owner instruction,
fail-closed recognition, no inference of importance or evidential weight, and
ordinary downstream governance. This Scope Lock narrows that freedom by freezing
a deterministic Version 1 grammar and mechanical extraction. That is compatible
with, and more restrictive than, the constitutional authorization.

### 3.3 Is the insertion point lawful?

Yes. A `ReasoningProvider` decorator is the narrowest existing seam capable of
establishing intent before prompt construction and inference. Restricting it to
`ReasoningSubject.OfTurn`, returning `Remember` on a match, and delegating
all other cases unchanged exactly once preserves the public request/response
contract and the Evidence Intelligence subject case. Placing logging outside the
decorator also preserves one production reasoning-outcome log for either path.

### 3.4 Is owner provenance overstated?

No. The instrument expressly limits Version 1 to trusted local owner adapters
that construct messages using the configured owner principal and disclaims
general cryptographic, session, remote-channel, or universal authentication.
It correctly treats principal equality as eligibility for recognition, not
permission to write. This is a bounded provenance claim, not a false security
claim. Expansion to a less trusted channel would require further authority.

### 3.5 Is the grammar deterministic and fail closed?

Yes. The positive language is finite and memory-anchored. Unanchored
`Save X`, `Store X`, `Commit X`, and `Keep X` are expressly excluded,
closing the operational-command ambiguity identified by prior diagnosis.
Negation precedes matching; questions, statements, idioms, blanks, unsupported
syntax, and uncertainty delegate. Proposition extraction may remove only frozen
wrapper material, outer whitespace, and at most one terminal period. It cannot
use a model or semantically rewrite the proposition.

The lighthouse fixture is consequently well-defined:

```text
Remember the test lighthouse is painted orange.
→ Remember("the test lighthouse is painted orange")
→ model delegate calls: 0
```

### 3.6 Are failures converted into persistence or success?

No. The Scope Lock requires classifier and delegate exceptions to retain their
failure character. It forbids conversion into persistence or false success.
Downstream admission decline and denial remain truthful and authoritative.

### 3.7 Is the authorized surface proportionate?

Yes. Two new runtime files and one production-composition edit are sufficient
for the classifier/decorator/wiring shape, with two focused test files and one
composition test modification. The stop condition prevents opportunistic
changes if that surface proves insufficient. Hard boundaries exclude the
coordinator, admission, parser, prompt, model client/configuration, Memory Core,
Knowledge Submission, evaluator, durability, QMD, UI, Docker, and persistence
architecture.

## 4. Required guarantees for completion review

Completion review must verify directly that:

1. only owner-matching `OfTurn` requests can take the deterministic path;
2. every frozen positive form and mandatory exclusion is covered;
3. model delegation is zero on a qualifying directive and exactly one otherwise;
4. extracted text is mechanically preserved under Section 5;
5. the classifier and decorator have none of the forbidden dependencies;
6. classifier/delegate exceptions propagate without admission or false success;
7. the existing `Remember` admission path and its truthful outcome mapping are
   reused unchanged; and
8. ordinary model-produced `Remember` remains possible through delegation.

These are completion-review obligations, not defects in the Scope Lock.

## 5. Findings

No contradiction, hidden persistence authority, public-contract expansion,
authentication overclaim, grammar ambiguity requiring correction, or unlawful
dependency was found. The proposal is bounded, technologically modest,
fail-closed, testable, and consistent with the adopted constitutional exception.
