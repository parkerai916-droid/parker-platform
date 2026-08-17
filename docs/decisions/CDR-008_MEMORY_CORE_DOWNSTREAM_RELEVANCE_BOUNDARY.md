**Status.** Accepted. Canonical. Frozen. It resolves one interpretive question only and does not itself authorise any implementation.

# CDR-008 — Memory Core / Downstream Relevance Authority Boundary

## Decision Question

> Does the Memory Core Scope Lock prohibition on ranked/scored/semantic retrieval (`MEMORY_CORE_SCOPE_LOCK.md` §4, §10) prohibit only Memory Core from exposing or performing such a retrieval operation, or does it also prohibit a separately-governed downstream component from computing relevance over a closed candidate set that Parker has already retrieved, through Memory Core's own unmodified, authorised interface, and independently determined eligible?

Two candidate answers, and only these two, are considered:

- **Decision A** — a prohibition on Memory Core's own retrieval interface, operations, implementations and authority.
- **Decision B** — a platform-wide prohibition preventing any downstream Parker component from performing separately-governed relevance computation over already-retrieved, already-eligible canonical knowledge.

This record does not decide whether any such downstream capability *should* be built, what it may do, or how it should be governed if authorised. Those are Programme 3's own questions, addressed by `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` (Status: PROPOSED — NOT ADOPTED), not by this record.

## Context

An Independent Constitutional Review of the above proposal (conducted this Programme, prior turn) reached **VERDICT C — REQUIRES ADDITIONAL CONSTITUTIONAL AMENDMENT**, on the finding that Knowledge Memory Scope Lock §4 cites `MEMORY_CORE_SCOPE_LOCK.md` §4/§10 as an independent, co-equal source of the semantic-retrieval exclusion the proposal seeks to lift at the Knowledge Retrieval layer, and that Programme 3's own "Out-of-Scope Change Policy" (Knowledge Memory Scope Lock §10) has no self-evident standing to authoritatively settle a question about where a *different*, separately-frozen document's boundary lies. That review identified this exact Decision Question as the blocking defect and recommended a dedicated CDR, structurally analogous to CDR-001, CDR-002, and CDR-004, each of which resolved an adjacent Memory-Core-boundary or comparison-boundary question through the CDR process rather than by informal assertion inside a lower-tier document.

This record exists to resolve that one question, and nothing else.

## Governing Provisions

`MEMORY_CORE_SCOPE_LOCK.md` §10, "Retrieval Scope" (frozen list, quoted in full):

> "**Frozen: exactly seven retrieval modes.**
> 1. Identifier lookup.
> 2. Entity lookup.
> 3. Document lookup.
> 4. Relationship traversal.
> 5. Metadata filtering.
> 6. Provenance-aware lookup.
> 7. Chronological lookup."

Immediately following, in the same section:

> "**Everything else is excluded** — most pointedly, semantic retrieval of any kind (already named in Section 4, restated here because it is the one exclusion most likely to be quietly reintroduced under a different name, such as 'relevance-ranked retrieval' or 'smart lookup,' during implementation). Every one of the seven modes above returns records matching **structural** criteria only — no scoring, no ranking beyond Section 8's own status/chronological ordering, and no relevance judgment of any kind. `MemoryRetrieval` performs no principal-based visibility filtering either (Section 6) — every one of the seven modes returns whatever structurally matches, unfiltered by who is asking, with visibility enforced entirely by Runtime before or around the call."

`MEMORY_CORE_SCOPE_LOCK.md` §14, "Acceptance Criteria":

> "Memory Core **SHALL** support exactly the seven retrieval modes named in Section 10, and **SHALL NOT** implement any ranked, scored, or semantic retrieval mode."

`MEMORY_CORE_SCOPE_LOCK.md` §4, Explicit Exclusions table, row "Semantic retrieval":

> "Explicitly and repeatedly excluded across all three frozen documents (Contract Design §9, Governance Review §11). No concrete need has been identified; it remains a future, separately-justified layer, not an extension of `MemoryRetrieval`."

`MEMORY_CORE_SCOPE_LOCK.md` §18.3, "Constitutional Boundaries" (Amendment 1 — Memory Record Comparison, formally adopted, additive-only):

> "rank, score, or order records for a caller (that capability, if it is ever authorised, is governed separately from and is not created by this section);"

and, in the same section's closing paragraph:

> "For the avoidance of doubt, the phrase 'unless separately authorised by future governance' does not reopen, narrow, or reinterpret Section 10's existing, unrelated exclusion of semantic retrieval from Memory Core's seven retrieval modes. That exclusion remains in force, unmodified, and governs a different, caller-facing capability."

`MEMORY_CORE_SCOPE_LOCK.md` §18.7, "Constitutional Consistency Check":

> "Section 10's exclusion is confined, per CDR-001 and CDR-002, to a differently-purposed capability this section does not touch..."

`MEMORY_CORE_SCOPE_LOCK.md` §19.3, "Constitutional Boundaries" (Amendment 2 — Provenance Lookup by Identifier):

> "**semantic retrieval** — Section 10's existing, unrelated exclusion of semantic retrieval is untouched; identifier lookup is exact-match, structural resolution, the polar opposite of ranked or meaning-based retrieval;"

`PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §4, Explicit Exclusions table, row "Embeddings, vector search, or any semantic/similarity-based retrieval":

> "Never designed anywhere in the frozen contracts; explicitly excluded at both the Memory Core layer (`MEMORY_CORE_SCOPE_LOCK.md` §4, §10) and the Knowledge Memory layer (Contract Design Version 2 §13). Knowledge Retrieval performs structural matching against caller-supplied criteria only."

`PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §10, "Out-of-Scope Change Policy" (quoted in full):

> "Any implementation proposal that: expands this Programme's scope beyond Section 3, weakens any constitutional guarantee named in Section 6, introduces a new subsystem responsibility not named in Section 3 or Section 5, or changes any public contract Contract Design Version 2 froze, **requires formal governance before implementation** — a Scope Lock revision, following the same review discipline this Programme itself just completed, never an implementation-level decision made under the belief that it is a small, load-bearing exception. This mirrors `MEMORY_CORE_SCOPE_LOCK.md` §6's own identical discipline, applied here without modification."

`PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §13, "Out of Scope":

> "Unchanged from Version 1: implementation, persistence, indexing, caching, optimisation, vector storage, embeddings..."

CDR-001, Question 4 (finding, quoted):

> "Memory Core Scope Lock's own use of 'semantic retrieval' is confined, consistently and without exception, to a differently-purposed, caller-facing retrieval capability."

CDR-004, Decision Question and reasoning (Provenance Identifier Resolution, classified as Model B — a specialised capability within an existing mode, without reopening Section 10's frozen text):

> "Rests on the distinction that Section 10's 'exactly seven... everything else excluded' language governs operation *shapes* (ruling out ranked/semantic retrieval as additional shapes), not a permanent freeze on which record kinds an existing, generically defined shape may address."

Production interfaces (`src/interfaces/MemoryCore.kt`, `src/runtime/DefaultReasoningKnowledgeSource.kt`, `src/runtime/InMemoryMemoryCore.kt`, `src/runtime/DurableMemoryCore.kt`), verified fresh: `DefaultReasoningKnowledgeSource.recall()` obtains its candidate set from `KnowledgeItemPersistence.findAll()` — a Knowledge Memory-owned store, not Memory Core — and touches Memory Core only through `MemoryRetrieval.getAssertion`/`getEntity`, two of the ten declared `MemoryRetrieval` methods, both bounded, single-record, identifier-based lookups; none of `MemoryRetrieval`'s six search/scan methods are called. The relevance test itself (`content.contains(query.relevance, ignoreCase = true)`) executes inside `DefaultReasoningKnowledgeSource.kt`, not inside any Memory Core file. Deleting that line requires no change to `MemoryCore.kt`, `InMemoryMemoryCore.kt`, or `DurableMemoryCore.kt`.

## Competing Interpretations

**Interpretation A (Decision A).** Section 10's rule is a bound on Memory Core's own read surface: "the only read operations Memory Core exposes." It governs what `MemoryRetrieval` may expose as Memory Core's own authorised interface — not what a separately-governed component may compute, downstream, over content already legitimately obtained through that unchanged interface. Once content is retrieved through one of the seven authorised modes exactly as they exist today, what happens to it afterward, in a different, independently-governed component, is a different constitutional question — one Section 10 does not purport to answer, and one §18.3/§18.7/§19.3 expressly describe as "governed separately."

**Interpretation B (Decision B).** Memory Core Scope Lock's purpose is to guarantee that Parker's canonical system of record is never surfaced to a caller via ranked, scored, or semantic matching, full stop — and that purpose does not care which file or component performs the ranking. Knowledge Memory Scope Lock §4's own citation of Memory Core Scope Lock as a joint source of the same practical exclusion is evidence the drafters read Memory Core Scope Lock's rule as reaching Knowledge Retrieval's behaviour, not just Memory Core's own. Memory Core Scope Lock §14 already imposes SHALL/SHALL NOT obligations on a downstream component (Knowledge Memory), proving the document is not textually confined to binding only Memory Core itself.

## Hostile Interpretation (Phase 4 — Steelmanning Decision B)

The strongest available case for Decision B, stated without straw-manning:

1. **The "quietly reintroduced" warning is evasion-focused, not location-focused.** Its evident purpose is to prevent Parker's canonical knowledge from ending up surfaced via ranked/scored matching under a euphemism. A capability named, deliberately, to avoid the words "vector search" or any specific library — as the reviewed proposal's own "Bounded Relevance Computation" is named — is close in kind to the exact evasion pattern the warning anticipates ("relevance-ranked retrieval," "smart lookup"). A hostile reading holds that if the warning's purpose is evasion-prevention, it should not turn on which component's source file the ranking code lives in.

2. **Memory Core is defined as "the authoritative system of record," full stop, for what Parker has observed, received, or derived.** A maximalist reading treats the seven-mode, structural-only retrieval contract as encoding a platform-wide design premise — that nothing in Parker ever surfaces canonical knowledge via ranked retrieval — not merely a rule about Memory Core's own interface shape.

3. **Knowledge Memory Scope Lock §4's own text says the exclusion is "never designed anywhere in the frozen contracts."** That phrase is broader than "not designed into Memory Core's interface" — a hostile reader treats "anywhere in the frozen contracts" as itself asserting platform-wide reach, not two narrow, independently-scoped rules that happen to overlap.

4. **Memory Core Scope Lock §14 already binds a downstream component.** "Memory Core SHALL NOT depend on Knowledge Memory. Knowledge Memory SHALL depend on Memory Core, never the reverse" and "Knowledge Memory SHALL NOT create, amend, or otherwise write any Memory Core record" are SHALL/SHALL NOT obligations Memory Core Scope Lock imposes on Knowledge Memory, a different, downstream governance unit. This proves the document's authors were willing and able to reach downstream when they intended to — undercutting any assumption that Memory Core Scope Lock can, by its nature, only ever govern Memory Core's own internals.

## Why the Hostile Interpretation Does Not Prevail

Argument 1 conflates the warning's *motivating concern* with its *stated scope*. The warning sentence is embedded, without qualification, inside a passage whose very first clause is "The seven frozen retrieval modes... are the only read operations Memory Core exposes" (§10). Its concern about evasion is real, but the text it actually modifies is a rule about what Memory Core exposes, not a free-floating platform-wide principle. A purposive reading that overrides the document's own explicit scoping clause is a weaker interpretive method than the textual scoping itself — and this Programme's own CDR practice (CDR-001 through CDR-005) has consistently favoured precise, textually-anchored readings over expansive purposive ones, reopening frozen text only on demonstrated, repository-grounded necessity (CDR-005's and CDR-006's "minimal-reopening discipline").

Argument 2 proves too much if taken at face value: if Memory Core Scope Lock's "authoritative system of record" framing by itself constitutionally froze *every* downstream component's treatment of Memory-Core-sourced content forever, Knowledge Memory's entire promotion/evaluation apparatus — which already applies substantive, non-structural judgment (candidate evaluation, promotion policy, revision, retirement) to Memory-Core-derived content — would itself be constitutionally suspect, which no governing document anywhere claims. The "authoritative system of record" language establishes Memory Core's role as the trusted origin of facts, not a constraint on every possible downstream computation performed on facts it has already, lawfully released.

Argument 3's "never designed anywhere in the frozen contracts" is Knowledge Memory Scope Lock's *own* phrasing, in Knowledge Memory Scope Lock's *own* table — it documents that no frozen contract (at either layer) has designed the capability, which is simply true, and does not itself establish that Memory Core Scope Lock's rule, read on its own terms, reaches beyond Memory Core's interface. This point is really an argument about Knowledge Memory Scope Lock's own text, addressed directly in the Knowledge Memory Scope Lock §4 Reconciliation section below, not an argument that Memory Core Scope Lock's rule is platform-wide.

Argument 4 is the strongest of the four, and is correct as far as it goes — Memory Core Scope Lock §14 does bind Knowledge Memory. But it binds Knowledge Memory to a specific, narrow class of obligation: the *write*/dependency relationship ("SHALL NOT... write any Memory Core record," "SHALL depend on Memory Core, never the reverse") — protecting Memory Core's integrity as the authoritative system of record against being altered by a downstream layer. It says nothing about what a downstream layer may *compute* over content it has already read through an unmodified, authorised channel. If the drafters had intended Section 14 to also bind downstream *relevance computation*, Section 14 is the demonstrated, already-used vehicle for saying so, and it does not.

Set against this, the affirmative case for Decision A does not need §18.3, §18.7, or §19.3 to carry more weight than they can bear. §18.7 and §19.3 are best read as confirming CDR-001's, CDR-002's, and CDR-004's finding that semantic/ranked retrieval is a *different capability* from the ones those amendments actually govern (Memory Record Comparison; Provenance Identifier Resolution) — valuable confirmation that Section 10's exclusion has not been quietly stretched to cover unrelated capabilities, but not itself a statement about which *component* may perform semantic/ranked retrieval. §18.3's "governed separately" language is genuinely supportive of Decision A but not, standing alone, decisive, given the two readings identified in the Rationale, above (governed by an instrument outside Memory Core Scope Lock's own family, or governed by a future amendment that remains within it). The decisive evidence is independent of all three: Section 10's own textual self-scoping to Memory Core's read surface; the reductio that a platform-wide reading would retroactively indict Knowledge Memory's own already-adopted, substantive promotion judgment over Memory-Core-derived content; and the direct, verified architectural fact that Memory Core's own interface, implementations, and behaviour are completely unaffected by, and unaware of, the downstream relevance step the reviewed proposal describes.

Finally, the nine-way coexistence test posed by the reviewing task — Memory Core's interface unchanged; no relevance scoring, semantic search, or semantic index in Memory Core; Memory Core unaware, non-dependent, and unaffected if the mechanism is removed; Parker retains canonical authority; permission/eligibility decided outside the mechanism — is not merely a hypothetical possibility. The reviewed proposal's Model A-Strict, verified against the actual production interfaces, satisfies all nine simultaneously today, as a structural fact about the code, not an aspiration. Decision B would require treating a component that Memory Core never calls, never depends on, is structurally unaware of, and would be entirely unaffected by removing, as nonetheless constitutionally *part of* Memory Core's own prohibited operations. That is not a natural reading of "the only read operations Memory Core exposes." This observation establishes only that the constitutional interpretation reached here is architecturally coherent — that a downstream mechanism satisfying Decision A's boundary can exist in principle, consistent with the actual code. It does not approve QMD, approve the reviewed proposal, validate any particular implementation's adequacy, or authorise semantic relevance in any form; those remain entirely separate, undecided questions for Programme 3's own governance process.

## Decision

**Decision A.**

The Memory Core Scope Lock prohibition on ranked/scored/semantic retrieval (`MEMORY_CORE_SCOPE_LOCK.md` §4, §10, §14) is a prohibition on Memory Core's own retrieval interface, operations, implementations, and authority. It does not, by itself, constitutionally prevent a separately-governed downstream Parker component from computing relevance over a closed candidate set that Parker has already retrieved, through Memory Core's own unmodified, authorised interface, and independently determined eligible through mechanisms entirely outside Memory Core.

This decision does not authorise any such downstream capability. See Constitutional Boundary and Explicit Non-Authorizations, below.

## Rationale

This record rests primarily on the four grounds below (1–4), which do not depend on resolving any ambiguity in the later amendments; §18.3, §18.7, and §19.3 (5–6) are treated as supportive but not decisive, for the reasons stated.

1. Section 10's own opening clause self-scopes the rule to "the only read operations Memory Core exposes" — a direct textual anchor for Decision A that stands on its own.
2. The reductio against a purely purposive reading is independently decisive: if "authoritative system of record" by itself froze every downstream component's treatment of Memory-Core-sourced content, Knowledge Memory's own already-adopted promotion/evaluation apparatus — which already applies substantive, non-structural judgment (candidate evaluation, promotion policy, revision, retirement) to Memory-Core-derived content — would itself be constitutionally impossible. No governing document anywhere claims that, and that machinery is demonstrably adopted, governing behaviour today.
3. The architectural/component-boundary facts are not hypothetical: verified fresh against the real, unmodified production interfaces, `DefaultReasoningKnowledgeSource` never calls any of `MemoryRetrieval`'s six search/scan methods, obtains its candidate set from Knowledge Memory's own persistence rather than Memory Core, and touches Memory Core only through two bounded, identifier-based lookups (`getAssertion`, `getEntity`). Memory Core's own files require no change if the downstream relevance step is removed. This is direct evidence that the component distinction Decision A relies on is architecturally real, not a relabelling of the same operation.
4. Section 14's demonstrated pattern of binding downstream components (Knowledge Memory) is confined to the write/dependency direction ("SHALL NOT... write any Memory Core record," "SHALL depend on Memory Core, never the reverse"); it does not extend, textually or by structural analogy, to downstream relevance computation over already-read content. If the drafters had intended Section 14 to reach that too, Section 14 is the demonstrated, already-used vehicle for saying so, and does not.
5. Section 18.3's later, formally-adopted language — describing a caller-facing ranking capability as something that, "if it is ever authorised, is governed separately from and is not created by this section" — is consistent with, and lends some further support to, Decision A. This record does not treat it as decisive on its own: "governed separately" is genuinely open to reading as either (a) governed by an instrument outside Memory Core Scope Lock's own governance family, or (b) governed by a future, separate amendment or section that remains within Memory Core Scope Lock's own governance family. Decision A does not depend on resolving that ambiguity in reading (a)'s favour; grounds 1–4 above are sufficient without it.
6. Sections 18.7 and 19.3 are correctly read as reinforcing CDR-001's, CDR-002's, and CDR-004's finding that semantic/ranked retrieval is a different *capability* from Memory Record Comparison and from Provenance Identifier Resolution, respectively. They are not treated here as evidence about which *component* may perform semantic/ranked retrieval — that is a different question those sections do not purport to answer, and this record does not rely on them for that purpose.
7. CDR-004 provides applicable but limited precedent (see Relationship to CDR-004, below): the methodology is shared, but CDR-008's substantive reach is greater, and the two records should not be read as constitutionally equivalent.

## Constitutional Boundary

This decision determines *only* that Memory Core Scope Lock does not, by itself, constitutionally forbid a separately-governed downstream relevance capability from existing. It does not determine that any such capability is authorised, wise, safe, or adopted. Actual authorisation, if it is ever granted, must come from Programme 3's own Knowledge Memory Scope Lock revision and Unit 9 Contract Design amendment process (or successor unit) — the process the reviewed proposal itself follows — never from this record alone.

Section 10's "quietly reintroduced under a different name" warning is **not weakened, narrowed, or neutralised by this record** for the purpose it actually governs: preventing Memory Core's own implementation from ever adding an eighth mode, or any ranked/scored/semantic operation, under a euphemism such as "relevance-ranked retrieval" or "smart lookup." That warning continues to bind Memory Core's own implementers with its full original force. This record narrows only the *reach* question — whether the warning's underlying prohibition also constitutes a platform-wide bar on separately-governed downstream components — and answers that question alone. A future implementer proposing to add any ranked or semantic capability to Memory Core itself, under any name, remains squarely and unambiguously prohibited by Section 10, exactly as before this record. That warning binds not only a differently-named eighth mode but also any internal delegation, wrapper, call-out, or dependency by which Memory Core's own seven modes come to rely on an external semantic, ranked, or scored mechanism while presenting an unchanged public interface — Mandatory Invariant 3, below, states this explicitly, precisely because a cosmetically unchanged seven-method interface that quietly delegates to an external ranking mechanism is the same evasion the warning was written to prevent, regardless of whether the delegating code sits inside `MemoryCore.kt` or merely calls out from it.

This record's holding is confined to the Memory Core / Knowledge Retrieval boundary question actually before it. It does not itself determine, and must not be cited as determining, any analogous constitutional boundary question for Evidence Custodian, World Model, conversations, document retrieval, or any other component and its own frozen Scope Lock. A future proposal touching any of those components may cite this record's reasoning as persuasive precedent for its own, separate, dedicated CDR — exactly as this record itself draws on CDR-004's method — but may not cite this record's holding as authorisation, or as a substitute for obtaining its own independent constitutional ruling.

## Mandatory Invariants

A downstream capability remains outside Memory Core's constitutional boundary, and this decision continues to apply to it, only while all of the following hold:

1. Memory Core's retrieval interface (`MemoryRetrieval`) is unchanged.
2. Memory Core's seven retrieval modes remain exactly seven, unchanged in count, name, or shape.
3. No semantic, ranked, or scored operation may be implemented by, performed by, delegated from, called by, or depended upon by Memory Core — whether directly, through an internal wrapper, or through any external service, index, dependency, library, process, or component — in `MemoryCore.kt`, any implementation of `MemoryRetrieval`, or any successor. This requirement is architectural, not physical-source-location based: Memory Core does not avoid performing semantic or ranked retrieval merely because the semantic computation is carried out by code that formally lives outside `MemoryCore.kt`, if Memory Core's own retrieval behaviour calls it, relies on it, or is shaped by it.
4. All candidate material the downstream mechanism ever sees was obtained exclusively through Memory Core's existing, authorised, unmodified retrieval pathways (today: `getAssertion`/`getEntity`-style bounded, identifier-based dereference; never a new or expanded Memory Core query).
5. Eligibility and permission are fully and finally decided, by Parker's existing mechanisms, before any candidate's content reaches the downstream mechanism.
6. The downstream mechanism cannot expand the candidate universe it was supplied; it receives a closed, Parker-supplied set and nothing else.
7. The downstream mechanism's output cannot itself create, amend, or constitute canonical knowledge; it returns, at most, an ordering or subset of Parker-issued identifiers.
8. Parker resolves every returned identifier against its own canonical state; the mechanism's own content, if any is returned, is never trusted or rendered.
9. Parker re-verifies permission and lifecycle eligibility, freshly, before any downstream result is used or disclosed.
10. The downstream mechanism remains removable at any time without requiring any change to Memory Core's interface or implementation.
11. Separate governance — a Programme 3 Scope Lock revision and Unit 9 Contract Design amendment (or successor unit), independently reviewed — is required before any such downstream mechanism may actually be authorised or implemented. This record, alone, authorises nothing.
12. Memory Core remains Parker's sole authoritative system of record for Memory Core records within its governed domain. No downstream relevance mechanism may create, become, constitute, or be treated as a persistent parallel or secondary source of canonical truth. This invariant does not itself prohibit temporary, request-scoped, disposable subordinate computational state, if such state is separately authorised by Programme 3's own governance process — it prevents authority transfer, not all computation, and this record authorises no such state itself.

Any implementation that violates any one of these twelve invariants falls outside this decision's boundary and must be separately, freshly evaluated against Memory Core Scope Lock §10 — Decision A does not extend to it.

## Explicit Non-Authorizations

This record does **not** authorise, and must never be cited as authorising, any of the following:

- semantic relevance computation of any kind;
- embeddings;
- vector search;
- QMD, or any other named library, model, or product;
- any particular model or library selection;
- persistent semantic indexes;
- remote or cloud-based relevance processing;
- relevance computation before permission or eligibility is decided;
- relevance computation, of any kind, inside Memory Core;
- an eighth Memory Core retrieval mode, or any modification to the seven frozen modes;
- modification of canonical `KnowledgeItem`s, `Assertion`s, `Entity`s, `Document`s, or `Relationship`s;
- semantic or relevance-based decisions by `PermissionEngine`;
- semantic retrieval over Evidence Custodian content;
- semantic retrieval over World Model content;
- semantic retrieval over conversations or arbitrary documents.

Every one of these remains exactly as unauthorised after this record as before it. This record settles an interpretive question about where Memory Core Scope Lock's boundary lies; it is not, and must never be read as, a grant of authority.

## Relationship to Knowledge Memory Scope Lock §4

Knowledge Memory Scope Lock §4 is itself Programme 3's own operative act of exclusion — its "Explicit Exclusions" table places "Embeddings, vector search, or any semantic/similarity-based retrieval" out of scope for Programme 3 directly, and that placement is revisable only through Knowledge Memory Scope Lock's own §10 "Out-of-Scope Change Policy." The row's stated reason cites two further, independent sources for why this exclusion is unsurprising, not derivative: `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §13, Knowledge Memory's own, separately and earlier frozen exclusion of "vector storage, embeddings" from its own contract; and `MEMORY_CORE_SCOPE_LOCK.md` §4/§10, Memory Core's own, separately frozen exclusion at a different architectural layer.

This is best read as **three things, not two**, correcting an earlier, less precise formulation of this finding: (1) Knowledge Memory Scope Lock §4's own current exclusion — an operative Programme 3 provision in its own right, not a mere pointer; (2) Contract Design V2 §13 — the deeper, Knowledge-Memory-layer root of that same exclusion, independently frozen within Knowledge Memory's own document family; and (3) Memory Core Scope Lock §4/§10/§14 — a separate, independently-standing prohibition on Memory Core itself, unaffected by anything Programme 3 does with (1) or (2).

The two-independent-prohibitions conclusion is preserved because the text supports it: (1)+(2) together form the Knowledge-Memory-layer prohibition, entirely within Programme 3's own governance and revisable through Knowledge Memory Scope Lock §10 alone; (3) is a wholly separate, Memory-Core-layer prohibition outside Programme 3's authority to revise at all. This finding is load-bearing for the same reason as before: a future Programme 3 Scope Lock revision — amending (1), grounded in also revising (2) — does not silently or automatically revise, weaken, or reinterpret (3), Memory Core Scope Lock's separate prohibition on Memory Core itself. The rules are severable. If Knowledge Memory Scope Lock §4's table row is later revised as part of an adopted proposal, its citation to `MEMORY_CORE_SCOPE_LOCK.md` §4/§10 should be corrected to reflect this record's finding — that Memory Core Scope Lock's own rule, properly read, was never itself a barrier to a downstream, separately-governed capability satisfying the Mandatory Invariants above — rather than silently dropped or left to imply Memory Core Scope Lock was reopened or weakened, which it was not.

## Relationship to CDR-004

The analogy to CDR-004 is methodologically strong but substantively limited, and the two records should not be read as constitutionally equivalent in weight or consequence. CDR-004 resolved whether Provenance Identifier Resolution was an existing retrieval mode, a specialised capability within one, or a constitutionally distinct new mode — and found it a specialised capability within the existing "identifier lookup" shape, without introducing any operation shape Section 10 excludes. Section 10's "exactly seven... everything else excluded" freeze was left fully intact in both directions: the shape-count remained seven, and no excluded shape (ranked, scored, or semantic) came into existence anywhere as a result of that record.

This record resolves a different, larger question: not which record kinds an existing, authorised shape may address, but whether an operation shape Section 10 explicitly excludes — ranked/scored/semantic retrieval — may exist at all, anywhere in Parker, subject to entirely separate authorisation, once relocated outside Memory Core's own interface. That is a materially broader interpretive move than CDR-004's, even though Memory Core's own frozen text is, in both cases, left completely untouched. The two records share method — resolve by careful reading of what the frozen text actually fixes, without touching the frozen text itself, and without inflating an omission or an adjacent silence into an unstated, sweeping prohibition — but CDR-008 should not be cited as CDR-004's constitutional equivalent; it carries greater consequence and has been reviewed accordingly (see Relative Constitutional Reach, below).

## Relative Constitutional Reach

This record's substantive reach is greater than CDR-001's, CDR-002's, or CDR-004's: each of those resolved a narrower classification or definitional question without permitting any previously-excluded operation shape to exist anywhere in Parker; this record does not change what Memory Core is permitted to do, but it does determine that an operation shape Section 10 explicitly excludes may, subject to entirely separate authorisation, exist outside Memory Core. A CDR remains the appropriate instrument for this larger question on constitutional grounds, not practical convenience: Section 10 supplies an explicit, component-facing textual anchor ("the only read operations Memory Core exposes") that this record interprets rather than amends; Memory Core itself — its interface, its seven modes, its SHALL/SHALL NOT obligations — remains completely unchanged under either Decision A or Decision B, so no frozen text requires amendment regardless of outcome; and this record creates no authorisation of its own — actual authorisation of any downstream capability remains delegated entirely to Programme 3's own separate governance process (Mandatory Invariant 11). A question of this kind — interpreting the reach of an existing prohibition without changing what the prohibited party may do, and without itself authorising anything — is squarely within a CDR's established role in this repository, even though its answer, once relied upon, carries more consequence than CDR-004's did.

## Consequences

- The blocking defect identified by the Independent Constitutional Review (governing-mechanism insufficiency: Programme 3's own Scope Lock revision, alone, could not settle a cross-document interpretive question about Memory Core Scope Lock's reach) is removed once this record is independently reviewed and adopted through the same discipline CDR-001 through CDR-007 followed.
- `MEMORY_CORE_SCOPE_LOCK.md` is not amended, reopened, or textually altered by this record in any way. Its seven modes, its SHALL/SHALL NOT language, and its explicit exclusions remain frozen and unchanged.
- The reviewed proposal (`PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md`) is not adopted, modified, or otherwise advanced by this record. Its own, separate governance process (Independent Constitutional Review → Defect Confirmation Review → Programme 3 adoption) still applies in full, including the further, non-blocking corrections that review identified.

## What Remains Separately Governed

This record settles the interpretive question and nothing more. Still required, entirely untouched by this record: whether the proposed Bounded Relevance Computation capability should actually be authorised; the Programme 3 Knowledge Memory Scope Lock revision and Unit 9 Contract Design amendment (or successor unit) themselves; the nine non-blocking corrections the Independent Constitutional Review identified (determinism, local-only restriction, evidential-eligibility precondition, fallback-trigger precision, fail-closed table completeness, explicit non-authorization bullets, query-disclosure permissioning, QMD-independence terminology cleanup); any future decision about Models B or C; and any eventual implementation unit, which independently requires its own review before any code is written.

## Adoption Requirements

This record has been adopted through the same discipline CDR-001 through CDR-007 established: (a) it was independently reviewed, the defects identified were confirmed and corrected through a Defect Confirmation and Correction Review, and (b) it was then formally accepted through an Adoption Review that reached ADOPTION VERDICT — ACCEPT, upon which its status was changed to Accepted. Canonical. Frozen. Decision A is accordingly governing constitutional interpretation for the Memory Core / Knowledge Retrieval boundary question this record decides, and the Independent Constitutional Review's VERDICT C blocking defect — the finding that no existing mechanism could authoritatively settle where Memory Core Scope Lock's boundary lies — is resolved at that interpretation layer.

CDR-008's own adoption does not itself authorise semantic relevance, or any downstream capability described in this record; see Explicit Non-Authorizations, above. It is also distinct from, and has no effect on, the separate adoption status of `PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md`, which remains PROPOSED — NOT ADOPTED and subject to its own, entirely separate governance process. Requirement (c) above remains an outstanding, forward-looking obligation rather than a condition on CDR-008's own adoption: any future Knowledge Memory Scope Lock revision, Unit 9 Contract Design amendment, or other Programme 3 governance instrument that relies on Decision A must expressly cite this record, without modification to Memory Core Scope Lock's own frozen text, and must independently satisfy Programme 3's own amendment and adoption requirements in full. Until Programme 3's own separate governance process adopts such an instrument, semantic relevance remains unauthorised.
