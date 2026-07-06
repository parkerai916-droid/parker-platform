# Build-Scope Exclusion Checklist

## Status
Process document. Not an Architecture Decision, not a specification --
a reusable checklist for an already-established engineering process.

## Purpose

`build.gradle.kts`'s `sourceSets { main { kotlin { exclude(...) } } }`
block keeps interface stubs whose supporting types do not yet exist out
of compilation, per ADR-022 (Kotlin Interface Stubs). Three units have
each independently brought one excluded file back into compilation once
its supporting types existed: Phase 2 (`EventBus.kt`), Sprint 4 Track A
Unit A3 (`MemoryStore.kt`), and Sprint 4 Track B Unit B3
(`WorldModel.kt`). Each did the same seven or eight things, in the same
order, without a written checklist to follow. This document writes that
process down so a fourth, fifth, or sixth occurrence follows it
deliberately rather than re-deriving it from reading the prior three's
commit history.

This checklist governs *removing* a file from the exclusion list. It
does not govern *adding* one -- a file is added to the exclusion list
only when it is first introduced as a stub with unspecified supporting
types, per ADR-022, which remains a one-time event per file, not a
recurring process.

## When to Use This Checklist

Use this checklist when an implementation unit's own scope has just
supplied every supporting type an already-excluded interface stub
depends on, and that stub is now ready to compile as part of the main
build -- exactly the situation Unit A3 and Unit B3 were each already in
when they did this.

Do not use this checklist to remove a file from the exclusion list
speculatively, ahead of its supporting types actually being specified
and implemented. `Agent.kt`, `AuditService.kt`, `ModelManager.kt`,
`NotificationService.kt`, and `Plugin.kt` remain excluded as of this
writing precisely because their supporting types are not yet specified
-- removing any of them before that work exists would violate ADR-022,
not extend it.

## Checklist

1. **Identify the excluded file in `build.gradle.kts`.** Confirm the
   file's name appears in the `exclude(...)` list inside
   `sourceSets { main { kotlin { ... } } }`, and read the block's own
   explanatory comment to confirm why it was excluded in the first
   place -- the comment should already name which supporting types were
   missing.

2. **Confirm the supporting types are now specified.** The relevant
   Architecture and Contract Design documents (per PES-001 Stage 1 and
   Stage 2A) must already name and shape every type the stub's
   interface depends on. Do not proceed on the assumption that
   implementation alone can settle a still-open design question --
   Stage 2A exists precisely so this step is already done by the time
   this checklist runs.

3. **Confirm the supporting types are now implemented.** The
   corresponding Kotlin (in `src/contracts` and/or `src/interfaces`)
   must already exist, matching the approved Contract Design
   field-for-field, before the stub file itself is touched.

4. **Remove only the relevant file from the exclude list.** Edit the
   `exclude(...)` block to remove the one file whose supporting types
   are now ready. Do not remove, reorder, or otherwise touch any other
   excluded file's entry -- each remaining exclusion is a separate,
   independent decision governed by its own future unit.

5. **Update the build comment.** The explanatory comment above
   `sourceSets { ... }` narrates, file by file, why each currently-
   excluded file remains excluded and why each previously-excluded file
   was brought back in. Add this occurrence to that narration in the
   same style as the existing entries for `EventBus.kt`,
   `MemoryStore.kt`, and `WorldModel.kt`; remove the file's name from
   the comment's "remain excluded" list if it names it there.

6. **Run the full test suite.** Per PES-001 Stage 7, Android Studio
   verification is the authoritative check. Bringing a previously-
   excluded file into compilation can surface compile errors or test
   failures the sandbox alone may not catch -- this step is not
   optional just because the change looks small.

7. **Update `docs/implementation/IMPLEMENTATION_HISTORY.md`.** Record
   the build-scope change as part of the implementation unit's own
   history entry -- it is part of what that unit did, not a separate,
   undocumented side effect.

8. **Update `docs/architecture/IMPLEMENTATION_GAPS.md`, if relevant.**
   If bringing the file into scope closes a previously-open gap (for
   example, a gap describing the stub's own incompleteness), close it
   explicitly. If it surfaces a new gap (as Unit A3's and Unit B3's own
   work each did), log it following the existing numbered-gap
   convention rather than leaving it undocumented.

9. **The unit's Post-Implementation Review must mention the build-scope
   change.** A Post-Implementation Review that implements a stub's
   supporting types without noting that the stub itself was also
   brought back into compilation is incomplete -- the build-scope
   change is part of what the unit did and part of what the review
   must confirm was done correctly.

## Why This Exists

The same nine steps were independently re-derived correctly three times
in a row (Phase 2, Unit A3, Unit B3), which is itself evidence the
process was ready to be written down rather than continuing to be
re-invented on each occurrence. Writing it down does not change what
any future unit does -- it removes the need for that unit to
reconstruct the process from precedent alone.
