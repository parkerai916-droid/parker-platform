// PROVENANCE SCRIPT -- NOT executed by the automated Parker/Gradle test suite.
//
// This is a one-off, manually-run generator. It performs real live inference
// against QMD's actual embedding model (via QMD's own `llm.js` -- the same
// code path `store.searchVec` uses in production) to embed the six
// canonical-memory records and the emergency-vet paraphrase used by
// `QmdCanonicalMemoryRetrievalExperimentTest.kt`. Its only output is
// `qmd-real-embedding-vectors.json`, sitting alongside it in this directory.
//
// The automated Kotlin test never runs this script and never performs live
// embedding inference itself -- it only reads the fixed numeric vectors this
// script already captured and wrote to that JSON file (baked into
// `QmdRealEmbeddingFixtures.kt` as literal constants). This script exists
// purely for provenance/reproducibility: anyone auditing Property 1's fixture
// data can see exactly how it was produced and re-run it if the six
// canonical-memory contents or the paraphrase text below ever change.
//
// Run manually, once, from a Windows development environment where QMD's
// embedding model is already downloaded/cached locally (this script requires
// live model inference and must never be invoked by CI):
//
//   cd C:\Projects\Parker\parker-qmd-experiment
//   <node-executable> C:\Projects\Parker\qmd\node_modules\tsx\dist\cli.mjs tests\composition\fixtures\generate-qmd-property1-embeddings.mts
//
// Re-run only if QMD's default embed model or the strings below change --
// the JSON fixture must always match this script's literal input strings
// exactly, or the fixture is stale and must be regenerated.

import { writeFile } from "node:fs/promises";
import {
  getDefaultLlamaCpp,
  formatQueryForEmbedding,
  formatDocForEmbedding,
  DEFAULT_EMBED_MODEL_URI,
} from "file:///C:/Projects/Parker/qmd/src/llm.ts";

// Must exactly match QmdCanonicalMemoryRetrievalExperimentTest.kt's `memories` list
// (content strings only -- the synthetic 3-D vectors there are superseded, for the
// Property 1 test only, by the real vectors this script produces).
const CANONICAL_MEMORIES: { knowledgeId: string; content: string }[] = [
  { knowledgeId: "memory-1", content: "the owner's synthetic emergency vet is Harbour Animal Clinic" },
  { knowledgeId: "memory-2", content: "the owner's synthetic regular vet is Riverside Veterinary Centre" },
  { knowledgeId: "memory-3", content: "the owner's synthetic dog groomer is Central City Grooming" },
  { knowledgeId: "memory-4", content: "the owner's synthetic emergency plumber is Wellington Rapid Plumbing" },
  { knowledgeId: "memory-5", content: "the owner's synthetic preferred pharmacy is Harbour Pharmacy" },
  { knowledgeId: "memory-6", content: "the owner's synthetic favourite hiking trail is Widow's Peak Ridge" },
];

// Must exactly match the paraphrase used in both QmdCanonicalMemoryRetrievalExperimentTest.kt
// and ParkerRuntimeReasoningContextIntegrationTest.kt.
const PARAPHRASE_QUERY = "Which animal clinic did I tell you to use in an emergency?";

async function main(): Promise<void> {
  const llm = getDefaultLlamaCpp();
  const model = DEFAULT_EMBED_MODEL_URI;

  const documentVectors: Record<string, number[]> = {};
  for (const memory of CANONICAL_MEMORIES) {
    // isQuery:false + formatDocForEmbedding mirrors exactly how QMD's own
    // production document-embedding path (store.ts getEmbedding, isQuery=false)
    // formats and embeds indexed content.
    const formatted = formatDocForEmbedding(memory.content, undefined, model);
    const result = await llm.embed(formatted, { model, isQuery: false });
    if (!result?.embedding) throw new Error(`no embedding returned for ${memory.knowledgeId}`);
    documentVectors[memory.knowledgeId] = result.embedding;
  }

  // isQuery:true + formatQueryForEmbedding mirrors exactly how QMD's own
  // production query-embedding path (store.ts getEmbedding, isQuery=true)
  // formats and embeds a search query.
  const formattedQuery = formatQueryForEmbedding(PARAPHRASE_QUERY, model);
  const queryResult = await llm.embed(formattedQuery, { model, isQuery: true });
  if (!queryResult?.embedding) throw new Error("no embedding returned for paraphrase query");

  const output = {
    provenance: {
      generatedBy: "tests/composition/fixtures/generate-qmd-property1-embeddings.mts",
      generatedAtIso: new Date().toISOString(),
      model,
      note:
        "Real QMD embedding model output (live inference), captured once on the " +
        "Windows development machine via QMD's own llm.js embed() path. Consumed " +
        "only as fixed constants by the automated Parker Kotlin test -- no live " +
        "inference occurs during automated test execution.",
    },
    paraphraseQuery: PARAPHRASE_QUERY,
    paraphraseQueryVector: queryResult.embedding,
    canonicalMemories: CANONICAL_MEMORIES.map(m => ({
      knowledgeId: m.knowledgeId,
      content: m.content,
      vector: documentVectors[m.knowledgeId],
    })),
  };

  const outPath = new URL("./qmd-real-embedding-vectors.json", import.meta.url);
  await writeFile(outPath, JSON.stringify(output, null, 2) + "\n", "utf8");
  console.log(
    `wrote ${CANONICAL_MEMORIES.length + 1} real embedding vectors ` +
      `(model=${model}, dimension=${queryResult.embedding.length}) to ${outPath.pathname}`,
  );
}

void main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
