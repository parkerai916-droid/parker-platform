import { readFile } from "node:fs/promises";
import { createStore } from "file:///C:/Projects/Parker/qmd/src/store.ts";

type Candidate = { path: string; content: string; vector: number[] };
type Request = {
  databasePath: string; query: string; queryVector: number[];
  candidates: Candidate[]; allowedPaths: string[]; limit: number;
};

async function main(): Promise<void> {
  const request = JSON.parse(await readFile(process.argv[2]!, "utf8")) as Request;
  const store = createStore(request.databasePath);
  try {
  const collection = "parker-canonical-memory";
  const now = "2026-08-17T00:00:00.000Z";
  store.ensureVecTable(request.queryVector.length);
  store.db.prepare(`INSERT INTO store_collections (name, path, pattern) VALUES (?, ?, ?)`)
    .run(collection, "/test-only/parker-canonical-memory", "**/*.md");

  for (const [index, candidate] of request.candidates.entries()) {
    const prefix = `qmd://${collection}/`;
    if (!candidate.path.startsWith(prefix)) throw new Error(`path outside experimental collection: ${candidate.path}`);
    const path = candidate.path.slice(prefix.length);
    const hash = `parker-memory-${String(index + 1).padStart(3, "0")}`;
    store.insertContent(hash, candidate.content, now);
    store.insertDocument(collection, path, path, hash, now, now);
    store.db.prepare(`INSERT INTO content_vectors (hash, seq, pos, model, embedded_at) VALUES (?, 0, 0, 'parker-precomputed-test', ?)`)
      .run(hash, now);
    store.db.prepare(`INSERT INTO vectors_vec (hash_seq, embedding) VALUES (?, ?)`)
      .run(`${hash}_0`, new Float32Array(candidate.vector));
  }

  const results = await store.searchVec(
    request.query, "parker-precomputed-test", request.limit, collection,
    undefined, request.queryVector, request.allowedPaths,
  );
    process.stdout.write(JSON.stringify(results.map(result => result.filepath)));
  } finally {
    store.close();
  }
}

void main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
