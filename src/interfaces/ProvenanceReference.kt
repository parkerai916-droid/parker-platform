package parker.core.interfaces

/**
 * Programme 3, Knowledge Memory, Implementation Unit 3 (Provenance-
 * reference type). Introduces the single, minimal pointer type by which
 * Knowledge Memory addresses a Memory Core [Provenance] record without
 * ever copying, owning, or modifying it
 * (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
 * §6, "Provenance reference -- minimum immutable characteristics"
 * (Amendment 6); restated without change by
 * `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §9's own
 * Article VIII/IX obligation). This Unit implements [ProvenanceReference]
 * and nothing else -- no [KnowledgeStore] change, no Memory Core change,
 * no retrieval behaviour, no permission behaviour, no lifecycle
 * behaviour, and no use of [EvidentialState] (Unit 2's own, separate,
 * already-complete type). Those remain later Units' own, separately
 * authorised responsibility.
 */

/**
 * A minimal, immutable pointer to exactly one Memory Core [Provenance]
 * record, and nothing more -- Contract Design Version 2 §6's own literal
 * requirement: "A provenance reference contains only the identifier
 * necessary to address the underlying Memory Core provenance record, and
 * nothing else." [provenanceId] is the only field this type carries.
 *
 * ## No provenance field content, ever
 *
 * No provenance attribute -- source type, acquisition time, creator, or
 * any other field [Provenance] itself carries -- is duplicated,
 * embedded, or cached here under any circumstance (Contract Design
 * Version 2 §6). A caller holding a [ProvenanceReference] learns nothing
 * about the underlying [Provenance] record's own content from the
 * reference itself; it learns only which record to ask Memory Core about
 * next.
 *
 * ## Immutable, and never repointed
 *
 * Once constructed, a [ProvenanceReference] addresses the same
 * [Provenance] record for as long as it exists (Contract Design Version
 * 2 §6, "may never be silently repointed to a different provenance
 * record"). [provenanceId] is declared `val`, exactly as every other
 * immutable field in this package is; nothing on this type reassigns it
 * after construction, and this type defines no method of any kind that
 * could.
 *
 * ## No ownership transfer
 *
 * Ownership of the referenced [Provenance] record remains with Memory
 * Core, regardless of how often a [ProvenanceReference] is the practical
 * path by which a caller encounters it (Contract Design Version 2 §6;
 * Scope Lock §9, Article VIII/IX). This type implies no ownership,
 * authority, or write access of any kind over the record it addresses --
 * it is a pointer a caller may follow to Memory Core's own retrieval
 * surface, never a substitute for going there, and never a second,
 * competing source of provenance.
 *
 * ## No validation beyond ordinary type safety
 *
 * This type performs no construction-time validation of its own.
 * [ProvenanceId] already rejects a blank value at its own construction
 * (`src/interfaces/MemoryCore.kt`); requiring that again here would be a
 * redundant, unauthorised business rule, not "ordinary type safety."
 * Consistently, this type carries no `init` block, no helper method, no
 * companion object, and no behaviour of any kind beyond what Kotlin's
 * `data class` construct supplies automatically for any single-field
 * immutable value holder in this codebase.
 */
data class ProvenanceReference(
    val provenanceId: ProvenanceId,
)
