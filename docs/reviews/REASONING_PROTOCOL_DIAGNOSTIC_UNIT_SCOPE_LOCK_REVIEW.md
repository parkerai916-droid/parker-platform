**Status:** Independent Review of the Unit 2-D Diagnostic Characterisation Scope Lock — **PASS, WITH TWO NOTED LIMITATIONS CARRIED FORWARD RATHER THAN CORRECTED (Section 5).** Governance review only, against committed baseline `fd7f221`. No live HTTP call, no campaign creation, and no repository mutation beyond this document and the Scope Lock it reviews occurred.

# Reasoning Protocol Diagnostic Unit Scope Lock — Review

## 1. Scope of this review

This document independently tests `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2D_DIAGNOSTIC_CHARACTERISATION_SCOPE_LOCK.md` against the eight criteria required of it, and separately runs the ten-point adversarial challenge required before finalizing the design. It treats the Scope Lock as a proposal to be tested, not as authority to be deferred to.

## 2. Criterion-by-criterion test

**Is it genuinely diagnostic?** Yes. All six frozen questions (Scope Lock §5) are framed as "does X vary / differ / occur," never "does remedy Y work." Section 6's explicit non-goals separately bar remedy-effectiveness questions, population-level rate claims, larger-model superiority claims, and configuration-tuning claims. The remedy firewall (§23) is a closed, named list, not a vague aspiration.

**Is it finite?** Yes. Exactly five fixture classes (§7), exactly six diagnostic questions (§5), a closed twenty-four-call schedule with every cell traced to a specific question (§18). No open-ended sweep, no "and more as needed" language appears anywhere in the document. §20 explicitly forbids mid-campaign expansion.

**Is it proportionate?** Yes, and deliberately conservative relative to Unit 2: 24 calls against Unit 2's 3,911, sized directly from the six questions rather than inherited from the existing corpus size. The repeatability count (10, and 5 for the secondary decision-only variant) is reasoned from what a triage-grade (not statistical-grade) distinction requires, not chosen for round-number convenience (§8).

**Does it reopen Unit 2?** No. §17 requires a new campaign identity carrying an explicit `diagnostic` marker, a new artifact root outside `qwen25coder7b-baseline-20260809`'s directory, and forbids describing the unit as a Unit 2 resumption. §3 states plainly that Unit 2's Stage 0 failure, its Independent Constitutional Review, and the Post-Stage-0 Governance Determination are not reopened, amended, or qualified. Independently re-verified during this review: `qwen25coder7b-baseline-20260809`'s campaign artifacts remain untouched by this task (Section 6 below).

**Does it select a remedy?** No. §23's firewall names twelve candidate remedy families and prohibits selecting, recommending, prototyping, or implementing any of them regardless of what future results show. §15 goes further than merely deferring structured output — it gives a substantive reason (representation was not PF01's problem) for why even *discussing* it as live in-scope work would be a category error, and still lands on "nowhere yet," not "later in this unit."

**Does it authorize live execution?** No. §1, §25, and §26 state this three times in different words: Independent Constitutional Review, an Implementation/Execution Plan and its own review, an Implementation Readiness Review and its own review, and explicit execution approval must all occur, in that order, before any call. This document's own review does not change that gate sequence.

**Does it preserve fail-closed governance?** Yes, but with a deliberate, well-argued departure from Unit 2's specific mechanism, not from the principle. §20 keeps a hard stop for the things fail-closed governance actually exists to protect against here — harness defects, identity/configuration mismatch, artifact-integrity failure, and any accidental consequential downstream action — while explicitly declining to treat a semantic miss as a stop condition. This review finds that reasoning sound: Unit 2's Stage 0 gate protects entry into an expensive, integrity-dependent statistical commitment; this unit has no such downstream commitment to protect, and its entire purpose is to observe the very outcome a semantic-miss auto-halt would suppress. Treating "fail-closed" as requiring an identical trigger condition in every context, rather than the same discipline applied to what is actually at stake in each context, would be a category error the Scope Lock correctly avoids.

**Does it provide enough evidence to support later remedy selection?** Provisionally yes, per the exit criteria in §22, which set a completeness-and-integrity bar rather than a particular-outcome bar. This review notes (Section 5 below) that the honest answer to "enough" cannot be fully verified until results exist — the Scope Lock itself acknowledges this in §8's explicit repetition-limit disclosure and in §22's own admission that a too-thin result is a valid, recordable outcome rather than grounds to quietly expand scope after the fact. That self-awareness is treated here as a strength, not a gap.

## 3. Independent verification of Unit 2 non-interference

Read-only, during this review: `/var/lib/parker/reasoning-protocol-live-model/qwen25coder7b-baseline-20260809/` was inspected but not modified. `stage-0.failed`, `manifest.txt`, and `raw.jsonl` hashes match every prior review in this session exactly; `stage-0.sealed` remains absent; no `stage-1/`, `stage-2/`, or new campaign directory exists. No file under `src/`, `tests/`, or `build.gradle.kts` changed. `git status` shows only the two new governance documents this task produced.

## 4. Adversarial challenge

Each of the ten required attacks, tested directly against the Scope Lock as written:

1. **Too large.** The weakest link under this attack is §10's context comparison: two profiles, one fixture, cheap, but the least decision-critical of the six questions per §6 of the prior Diagnostic Planning Review. The Scope Lock already identifies this itself and states it as the first-cuttable element if resource discipline demanded shrinking further, while retaining it in the base design because its marginal cost (2 of 24 calls) is trivial. This review finds that honest, not evasive, and does not require removing it.
2. **Too small.** Correct, and conceded rather than denied: §8 states plainly that ten repeats cannot rule out a moderate true divergence rate landing at an extreme by chance, and §22 states that an ambiguous result is itself a valid finding rather than license to expand mid-campaign. The design accepts a real risk of an inconclusive DQ1 result in exchange for staying two orders of magnitude smaller than a statistical campaign. This review accepts that trade-off as intentional and stated, not hidden.
3. **Llama comparison adds no useful information.** Partly correct: §9 already discloses that model size and specialization are simultaneously confounded between `qwen2.5-coder:7b` and `llama3.2:3b`, so a clean causal attribution is not available from this comparison alone. The Scope Lock does not overclaim past that limitation anywhere in §9 or §21.
4. **Llama comparison is essential.** Also correct, and the stronger of the two competing claims at the margin: without any cross-model point, DQ4's model-specific-versus-protocol-general distinction is entirely unanswerable, at a marginal cost of one call. The Scope Lock keeps it in the base design; this review agrees that omitting it would be the greater error.
5. **Repetitions cannot meaningfully distinguish capability from stochasticity.** Already addressed directly in §8's own explicit-limitation paragraph, in language nearly identical to the strongest form of this attack. Nothing left to add; the Scope Lock does not claim more than the sample size supports.
6. **Context testing creates unnecessary combinatorial growth.** Addressed structurally, not just rhetorically: §10 crosses exactly one fixture with exactly two profiles and explicitly forbids crossing with the other four fixture classes or with `llama3.2:3b`. The attack would land against a design that let context testing scale with the fixture/model matrix; this one does not.
7. **Semantic and representation testing cannot practically be separated.** Correct as a claim about the underlying generative process, and the Scope Lock is precise about this rather than overselling it: §§12–14 separate independently-recorded classification fields on a single trial (already true, already demonstrated by PF01), while §16 and §21 rule 6 are careful to describe DQ5 as suggestive evidence about coupling, not a clean causal ablation of "representation" from "semantics."
8. **Secretly benchmarking models instead of diagnosing Parker.** The genuine risk is real and the Scope Lock's mitigation is interpretive, not structural: §21 rules 3–4 explicitly forbid concluding "Llama is better" or "switch models" from DQ4, and §23 firewalls "model replacement" and "a larger model" as remedies regardless of DQ4's outcome. This review flags that this protection depends on the interpretation worksheet (§19) actually being held to §21's rules at review time — a documentation discipline, not something the Scope Lock's text alone can enforce mechanically. Noted as a limitation, not a defect requiring redesign (Section 5).
9. **Already biased toward structured output.** Tested directly: §15 does not describe any benefit of structured output, does not place it in a "future track," and gives a specific, negative reason (representation was not the problem PF01 showed) rather than generic caution. DQ6 is described as continuing Unit 1/2's already-existing measurement practice, not as new instrumentation built toward a structured-output argument. This attack does not land.
10. **Optimizing Parker specifically for Qwen.** Correct and explicitly conceded: §9's DQ5 scope is Qwen-only, and §21 rule 6 states this limitation directly rather than allowing a Qwen-specific coupling finding to be read as protocol-general. The Scope Lock chose not to extend DQ5 to Llama, trading a known scope gap against avoiding further combinatorial growth (interacting with attacks 1 and 6). This review agrees with that trade-off given the marginal-cost reasoning already applied throughout, but records it explicitly below as a residual limitation rather than treating the disclosure alone as sufficient.

## 5. Limitations carried forward, not corrected

Two points from Section 4 are genuine, acknowledged gaps rather than defects requiring the Scope Lock to be rewritten:

- **DQ4's benchmarking risk (attack 8) is guarded by interpretation discipline, not by the schedule itself.** Nothing in §18's cell count prevents a future interpretation worksheet from overclaiming; only §21's rules and whatever Independent Constitutional Review examines that worksheet at exit (§22) actually prevent it. This is the same discipline Unit 2 already relied on throughout its own interpretation rules and reviews, so it is not a novel risk — but it is worth stating plainly rather than treating §21's existence as self-enforcing.
- **DQ5's Qwen-only scope (attack 10) means any coupling finding is not yet known to generalize.** The Scope Lock discloses this (§21 rule 6) rather than hiding it, and this review agrees the alternative (extending DQ5 to Llama now) would reintroduce the combinatorial growth attacks 1 and 6 already argue against. Accepted as a stated boundary of this unit's evidence, to be revisited only if a future unit's scope explicitly requires it.

Neither limitation changes this review's verdict below; both are already disclosed in the Scope Lock's own text, which this review confirms rather than merely trusts.

## 6. Verdict

```text
PASS
```

The Scope Lock is accepted as proposed governance, subject to the gate sequence its own §25 already requires (Independent Constitutional Review, Implementation/Execution Plan and its review, Implementation Readiness Review and its review, explicit execution approval) before any implementation or live call. This review is not that Independent Constitutional Review; it is the first read-only check the Scope Lock's own §25 anticipates, and does not substitute for it.

## 7. Confirmation

No model or HTTP call occurred during this review. No Unit 2 campaign artifact changed. No production, test, or Gradle file changed. Nothing was staged, committed, or pushed.
