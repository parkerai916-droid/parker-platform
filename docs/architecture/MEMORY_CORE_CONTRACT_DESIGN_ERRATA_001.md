# Memory Core Contract Design — Errata 001

## Status

**Documentation only.** This document amends no architecture, no
scope, no field, no event, and no behaviour anywhere in
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` (the Contract
Design), `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, or
`docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md`. It records two
internal inconsistencies between the Contract Design's own detailed,
normative sections and its own summary wording — discovered during
Implementation Unit 6 (`Relationship`), not introduced by it. Neither
`src/` nor `tests/` is touched by this document.

---

## Corrections

1. **Contract Design §8 defines eight recognised relationship types, not
   seven.** §8's own body names `SUPPORTS`, `CONTRADICTS`, `AMENDS`,
   `SUPERSEDES`, `DISPUTES`, `SAME_AS`, `EXTRACTED_FROM`, and
   `REFERENCES` — eight distinct values, presented across seven bulleted
   lines because `SUPPORTS`/`CONTRADICTS` share one line. §18's summary
   table states "seven recognised values," undercounting by miscounting
   lines rather than values.
2. **`Relationship` does not include a metadata field.** §8's own
   detailed, required-field list for `Relationship` names exactly:
   identifier, relationship type, from/to endpoint, directional flag,
   provenance reference, creation metadata (a timestamp, not a key/value
   map), and status. No `metadata: Map<String, String>` field appears
   anywhere in that list.
3. **The summary statement that metadata is present on every record type
   is incorrect.** §18 states a non-authoritative metadata key/value map
   is "present on every record type (Sections 4–8)." This is wrong on
   two counts, not one: neither `Relationship` (§8, per Correction 2
   above) nor `Provenance` (§7) carries a metadata field. §7's own
   detailed field list for `Provenance` never named one either.
4. **The detailed normative contract sections prevail over the
   conflicting summary wording.** Where §18 (Contract Summary) states
   something a preceding, detailed section (§4–§8, each defining one
   contract's own required fields field-by-field) does not support, the
   detailed section is authoritative. §18 is a summary of what those
   sections already decided, not an independent source of new
   requirements — it cannot expand a contract's field list beyond what
   its own governing section defined.
5. **The Unit 6 implementation is therefore conformant and requires no
   code change.** `Relationship` and `RelationshipEndpoint`
   (`src/interfaces/MemoryCore.kt`) implement exactly §8's own detailed
   field list (no metadata field) and exactly the eight relationship-type
   values §8's own body names (`Relationship.SUPPORTS`,
   `Relationship.CONTRADICTS`, `Relationship.AMENDS`,
   `Relationship.SUPERSEDES`, `Relationship.DISPUTES`,
   `Relationship.SAME_AS`, `Relationship.EXTRACTED_FROM`,
   `Relationship.REFERENCES`). Nothing in `tests/contracts/RelationshipTest.kt`
   needs revision either — its own structural test already asserts
   `Relationship` exposes exactly eight fields (no metadata) and its own
   constant-membership test already asserts exactly eight recognised
   relationship-type values.

---

## What this document does not do

It does not add, remove, or rename any contract, field, event, retrieval
mode, lifecycle transition, or permission rule. It does not authorise
any change to `MEMORY_CORE_SCOPE_LOCK.md` or
`MEMORY_CORE_IMPLEMENTATION_PLAN.md`, both of which remain frozen and
normative, unaffected by this errata. It does not reopen Implementation
Unit 6, and it does not block, delay, or modify Implementation Unit 7.

---

```
ERRATA ACCEPTED — IMPLEMENTATION MAY PROCEED
```
