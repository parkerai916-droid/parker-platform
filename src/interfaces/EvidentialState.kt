package parker.core.interfaces

/**
 * Programme 3, Knowledge Memory, Implementation Unit 2 (Evidential-state
 * representation). Introduces the single value type expressing
 * `docs/architecture/epistemic-integrity.md`'s Article IV ("Evidential
 * Representation") evidential-state classification --
 * `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md`'s
 * own Unit 2 objective, "structurally capable of an
 * insufficiently-supported/unresolved outcome"
 * (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §5, item 4;
 * `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §4,
 * "Mandatory expressiveness"). This Unit implements [EvidentialState] and
 * nothing else -- no promotion logic, no Knowledge Candidate change, no
 * lifecycle behaviour, no confidence-sourcing rule, no wiring into
 * [KnowledgeStore] or any other production path. Those remain later Units'
 * own, separately authorised responsibility (Units 4 through 9).
 */

/**
 * The fourteen evidential states Article IV "recognises, without
 * limitation" (Article IV, second paragraph), listed here in exactly the
 * order and grouping the Article itself specifies -- descending evidential
 * strength, save that [COMPETING_EXPLANATIONS] and [WORKING_HYPOTHESIS] are
 * not commensurable with a single rank, since Article IV expressly allows
 * more than one to properly co-exist across different propositions. No
 * state beyond these fourteen is introduced, and none of the fourteen is
 * omitted, merged, or renamed beyond a direct, literal rendering of
 * Article IV's own wording into an enum constant -- this Unit's own
 * explicit instruction not to invent, simplify, or extend the ratified
 * taxonomy.
 *
 * A [KnowledgeStore]-descended type assigns exactly one of these fourteen
 * values to a proposition; it never carries more than one simultaneously.
 * Article IV's "more than one may properly co-exist" language describes
 * two *different* propositions each independently classified as
 * [COMPETING_EXPLANATIONS] or [WORKING_HYPOTHESIS], not a single
 * classification holding multiple states at once -- this enum's closed,
 * single-valued shape is therefore a faithful, not a diminished,
 * rendering of that language.
 *
 * ## Mandatory expressiveness, satisfied structurally, not by convention
 *
 * [UNKNOWN] and [INDETERMINATE] are Article IV's own two lowest states,
 * carried into this enum unchanged, not added by this Unit as an
 * extension of the Article. Their presence is the concrete mechanism
 * satisfying the Scope Lock's and Contract Design Version 2's binding
 * "must be capable of expressing an insufficiently-supported/unresolved
 * outcome, not only graduated confidence" requirement (Scope Lock §5 item
 * 4; Contract Design Version 2 §4) -- a caller reading an [EvidentialState]
 * value can already distinguish "not yet knowable, but might become so"
 * ([UNKNOWN]) from "not presently possible to say whether it is even
 * knowable" ([INDETERMINATE]) from any of the twelve stronger, positively
 * supported states, without needing a separate, second field to express
 * that distinction. No confidence scalar of any kind is introduced by
 * this type -- consistent with the Contract Design's own "not only
 * graduated confidence" wording, a bare number could not, by itself,
 * satisfy this requirement; a closed set of named, qualitatively distinct
 * states can and does.
 *
 * ## What this Unit deliberately does not decide
 *
 * Whether, and how, a confidence figure (`Double?`) travels alongside a
 * value of this type on some later record -- and, if so, how that figure
 * is sourced -- is Contract Design Version 2 §4's own separate question
 * ("who assigns it," "confidence... sourced exclusively from Memory
 * Core's own recorded evidence or from Knowledge Memory's own independent
 * evaluation"), assigned to later Units (Unit 4, which adds this type as a
 * field on Knowledge Item; Unit 6, which implements the sourcing rule
 * itself). This type is the discrete classification alone -- it is not
 * combined with a confidence field, a timestamp, or any other value here,
 * so that later Units remain free to compose it as each one's own,
 * separately authorised design requires.
 *
 * ## Ordering
 *
 * Declaration order mirrors Article IV's own descending-strength list
 * exactly, for readability and to keep this file a direct, checkable
 * transcription of the Article rather than a reorganised paraphrase of
 * it. Kotlin's own default `enum class` ordinal ordering is not, and must
 * never be, relied upon by any future Unit as a substitute for an
 * explicit strength comparison -- [COMPETING_EXPLANATIONS] and
 * [WORKING_HYPOTHESIS]'s own non-commensurability with a single rank
 * (immediately above) already makes a total ordinal ordering an incorrect
 * model of Article IV's actual, only-partially-ordered strength
 * relation.
 */
enum class EvidentialState {
    /** Article IV: information captured at or during the event itself, without an intervening inferential step. */
    DIRECT_OBSERVATION,

    /** Article IV: information captured at or during the event itself, without an intervening inferential step. */
    CONTEMPORANEOUS_RECORD,

    /** Article IV: information confirmed by independent authentication. */
    VERIFIED_EVIDENCE,

    /** Article IV: information confirmed by more than one independent source. */
    CORROBORATED_EVIDENCE,

    /** Article IV: a proposition that follows from the available evidence under Article VI without requiring an assumption the evidence does not itself supply. */
    EVIDENTIALLY_SUPPORTED_CONCLUSION,

    /** Article IV: a proposition that follows from the available evidence but requires at least one assumption, inferential step, or interpretive judgment beyond what the evidence directly supplies -- weaker than [EVIDENTIALLY_SUPPORTED_CONCLUSION] notwithstanding its ordinary-language use of the word "conclusion." */
    REASONED_CONCLUSION,

    /** Article IV: weaker still than [REASONED_CONCLUSION], resting on a pattern or indicator rather than a chain of evidence-supported steps. */
    REASONED_INFERENCE,

    /**
     * Article IV: a proposition for which the evidence does not yet
     * justify a single account. Not commensurable with [WORKING_HYPOTHESIS]
     * on a single strength rank -- more than one proposition may properly
     * carry this state at once (Article IV, second paragraph; Article IV's
     * closing paragraph on Article V-driven reformulations).
     */
    COMPETING_EXPLANATIONS,

    /**
     * Article IV: a proposition for which the evidence does not yet
     * justify a single account. Not commensurable with
     * [COMPETING_EXPLANATIONS] on a single strength rank, for the same
     * reason.
     */
    WORKING_HYPOTHESIS,

    /** Article IV: rests on memory after the fact rather than on evidence created at the time. */
    RETROSPECTIVE_RECOLLECTION,

    /** Article IV: rests on reassembly after the fact rather than on evidence created at the time. */
    RECONSTRUCTED_ACCOUNT,

    /** Article IV: rests on no evidentiary chain capable of being traced. */
    SPECULATION,

    /**
     * Article IV: the available evidence is presently insufficient to
     * assign a stronger state, but the matter may become knowable with
     * further evidence. One of this type's two mandatory
     * insufficiently-supported/unresolved states (see this file's own
     * header KDoc, "Mandatory expressiveness").
     */
    UNKNOWN,

    /**
     * Article IV: it cannot presently be established whether the matter is
     * knowable at all. The second of this type's two mandatory
     * insufficiently-supported/unresolved states, strictly weaker than
     * [UNKNOWN] -- [UNKNOWN] presumes the matter is knowable in principle;
     * this state does not even presume that much.
     */
    INDETERMINATE,
}
