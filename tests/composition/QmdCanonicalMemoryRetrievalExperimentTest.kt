package parker.composition



import java.nio.file.Files

import java.nio.file.Path

import kotlin.io.path.absolutePathString

import kotlin.test.Test

import kotlin.test.assertEquals

import kotlin.test.assertFalse

import kotlin.test.assertSame

import kotlin.test.assertTrue



/**

 * Experimental composition seam only. Parker supplies the canonical, already-authorized

 * candidates; QMD is permitted to rank their derived virtual paths and nothing else.

 */

private class ExperimentalQmdCanonicalMemoryAdapter(

    private val ranker: RealQmdAuthorizedVectorProcessBridge,

) {

    fun rank(

        query: String,

        authorizedCandidates: List<ExperimentalParkerCanonicalMemory>,

        maximumResults: Int,

    ): List<ExperimentalParkerCanonicalMemory> {

        val canonicalByPath = authorizedCandidates.associateBy { it.qmdVirtualPath() }

        val rankedPaths = ranker.searchVector(

            query = query,

            allowedPaths = canonicalByPath.keys.toList(),

            limit = maximumResults,

        )



        return rankedPaths.mapNotNull(canonicalByPath::get).take(maximumResults)

    }

}



private data class ExperimentalParkerCanonicalMemory(

    val knowledgeId: String,

    val content: String,

    val precomputedVector: List<Double>,

) {

    fun qmdVirtualPath(): String = "qmd://parker-canonical-memory/$knowledgeId.md"

}



/**

 * Test-only process boundary around QMD fba2e37's real Store.searchVec implementation.

 * Its disposable database lives in Parker's test temp directory; the QMD checkout is read-only.

 */

private class RealQmdAuthorizedVectorProcessBridge(

    private val indexedMemories: List<ExperimentalParkerCanonicalMemory>,

    private val queryVector: List<Double>,

) {

    val allowedPathCalls = mutableListOf<List<String>>()



    fun searchVector(query: String, allowedPaths: List<String>, limit: Int): List<String> {

        allowedPathCalls += allowedPaths

        val tempDirectory = Files.createTempDirectory("parker-qmd-experiment-")

        try {

            val request = tempDirectory.resolve("request.json")

            Files.writeString(request, requestJson(tempDirectory.resolve("index.sqlite"), query, allowedPaths, limit))

            val process = ProcessBuilder(

                nodeExecutable(),

                "C:\\Projects\\Parker\\qmd\\node_modules\\tsx\\dist\\cli.mjs",

                Path.of("tests", "composition", "qmd-authorized-vector-bridge.mts").toAbsolutePath().toString(),

                request.toString(),

            ).start()

            val stdout = process.inputStream.bufferedReader().readText()

            val stderr = process.errorStream.bufferedReader().readText()

            val exitCode = process.waitFor()

            check(exitCode == 0) { "QMD test bridge failed ($exitCode): $stderr" }

            return parseStringArray(stdout)

        } finally {

            tempDirectory.toFile().deleteRecursively()

        }

    }



    private fun requestJson(databasePath: Path, query: String, allowedPaths: List<String>, limit: Int): String =

        """{"databasePath":${jsonString(databasePath.absolutePathString())},"query":${jsonString(query)},"queryVector":${numberArray(queryVector)},"candidates":[${indexedMemories.joinToString(",") { memory -> """{"path":${jsonString(memory.qmdVirtualPath())},"content":${jsonString(memory.content)},"vector":${numberArray(memory.precomputedVector)}}""" }}],"allowedPaths":${stringArray(allowedPaths)},"limit":$limit}"""



    private fun nodeExecutable(): String {

        val configured = System.getenv("QMD_TEST_NODE")

        if (!configured.isNullOrBlank()) return configured

        val bundled = Path.of("C:\\Users\\steve\\.cache\\codex-runtimes\\codex-primary-runtime\\dependencies\\node\\bin\\node.exe")

        return if (Files.isRegularFile(bundled)) bundled.toString() else "node"

    }



    private fun jsonString(value: String): String = buildString {

        append('"')

        value.forEach { character ->

            when (character) {

                '"' -> append("\\\"")

                '\\' -> append("\\\\")

                '\n' -> append("\\n")

                '\r' -> append("\\r")

                '\t' -> append("\\t")

                else -> append(character)

            }

        }

        append('"')

    }



    private fun numberArray(values: List<Double>) = values.joinToString(",", "[", "]")

    private fun stringArray(values: List<String>) = values.joinToString(",", "[", "]", transform = ::jsonString)

    private fun parseStringArray(json: String): List<String> =

        Regex("\\\"((?:\\\\.|[^\\\"])*)\\\"").findAll(json.trim()).map { it.groupValues[1] }.toList()

}



class QmdCanonicalMemoryRetrievalExperimentTest {

    private val memories = listOf(

        ExperimentalParkerCanonicalMemory("memory-1", "the owner's synthetic emergency vet is Harbour Animal Clinic", listOf(1.0, 0.0, 0.0)),

        ExperimentalParkerCanonicalMemory("memory-2", "the owner's synthetic regular vet is Riverside Veterinary Centre", listOf(0.8, 0.6, 0.0)),

        ExperimentalParkerCanonicalMemory("memory-3", "the owner's synthetic dog groomer is Central City Grooming", listOf(0.0, 1.0, 0.0)),

        ExperimentalParkerCanonicalMemory("memory-4", "the owner's synthetic emergency plumber is Wellington Rapid Plumbing", listOf(0.7, 0.0, 0.7141428429)),

        ExperimentalParkerCanonicalMemory("memory-5", "the owner's synthetic preferred pharmacy is Harbour Pharmacy", listOf(0.0, 0.0, 1.0)),

        ExperimentalParkerCanonicalMemory("memory-6", "the owner's synthetic favourite hiking trail is Widow's Peak Ridge", listOf(0.0, 0.7071067812, 0.7071067812)),

    )



    private val paraphrase = "Which animal clinic did I tell you to use in an emergency?"



    // memories' `precomputedVector`s above are placeholder/orthogonal-toy vectors used by the

    // denial test below, where only exclusion (not ranking quality) is under test. Property 1's

    // own proof, immediately following, does not use these -- it substitutes QMD's real captured

    // embedding-model output for all seven texts (see QmdRealEmbeddingFixtures.kt) so that the

    // ranking it asserts is produced by QMD's real model, not stipulated by this test file.

    private val realEmbeddingMemories = memories.map { memory ->

        memory.copy(precomputedVector = QmdRealEmbeddingFixtures.memoryVectors.getValue(memory.knowledgeId))

    }



    @Test

    fun `real QMD, scoring QMD's own real embedding-model output for the paraphrase and all six memories, ranks the emergency-vet memory first among six genuine distractors`() {

        // Property 1 proof. Every vector here -- the paraphrase's and all six memories' -- is

        // QMD's own real embedding-model output for that exact English text (model and

        // dimension recorded in QmdRealEmbeddingFixtures.kt), captured once by hand on the

        // Windows development machine via `generate-qmd-property1-embeddings.mts` and persisted

        // as fixed constants in QmdRealEmbeddingFixtures.kt / qmd-real-embedding-vectors.json.

        // The relationship between the paraphrase text and its vector -- and each memory's text

        // and its vector -- was produced by QMD's real embedding model, not stipulated by this

        // test: unlike a hand-picked or perturbed vector, nothing here asserts a relationship the

        // test author chose.

        //

        // Execution of THIS test performs no live model inference: every vector below is a fixed,

        // precomputed constant read at compile time. Only QMD's real cosine vector-search/ranking

        // logic (exactVecScanByHashSeq in qmd/src/store.ts) runs live, against these already-fixed

        // numbers, through the same real fba2e37 process bridge the other tests in this file use.

        assertEquals(

            QmdRealEmbeddingFixtures.PARAPHRASE_QUERY,

            paraphrase,

            "the real embedding fixture must have been captured for the exact same paraphrase text this test uses",

        )

        val bridge = RealQmdAuthorizedVectorProcessBridge(realEmbeddingMemories, QmdRealEmbeddingFixtures.paraphraseQueryVector)

        val adapter = ExperimentalQmdCanonicalMemoryAdapter(bridge)



        val result = adapter.rank(paraphrase, realEmbeddingMemories, maximumResults = 1)



        assertEquals(listOf(realEmbeddingMemories[0].qmdVirtualPath()), result.map { it.qmdVirtualPath() })

        assertSame(realEmbeddingMemories[0], result.single())

        assertEquals(realEmbeddingMemories.map { it.qmdVirtualPath() }, bridge.allowedPathCalls.single())

    }



    @Test

    fun `Parker-denied emergency vet is indexed but is not eligible for QMD scoring or return`() {

        val denied = memories[0]

        val parkerAuthorized = memories.filterNot { it === denied }

        val bridge = RealQmdAuthorizedVectorProcessBridge(memories, listOf(1.0, 0.0, 0.0))

        val adapter = ExperimentalQmdCanonicalMemoryAdapter(bridge)



        val result = adapter.rank(paraphrase, parkerAuthorized, maximumResults = 1)



        assertFalse(denied.qmdVirtualPath() in bridge.allowedPathCalls.single())

        assertTrue(result.none { it === denied })

        assertSame(memories[1], result.single())

    }



    @Test

    fun `an authorized weaker candidate is still returned when far stronger unauthorized candidates dominate global similarity, proving authorization is applied before scoring rather than filtered afterward`() {

        // Regression guard for Property 3's structural requirement: authorization must

        // exclude candidates from QMD's similarity scan itself, not merely from the

        // final result list. A "global top-K vector search, then filter by

        // allowedPaths" implementation would starve here -- the 40 unauthorized noise

        // candidates below are all near-perfect matches to the query (cosine distance

        // ~0) and would fill any plausible top-K window (QMD's own over-fetch factor

        // for a global ANN scan tops out at limit * 30; with limit = 1 that is 30, and

        // 40 noise candidates exceeds it with margin), leaving zero authorized

        // survivors after a post-hoc filter. Only a true pre-scoring restriction to

        // allowedPaths -- QMD's real fba2e37 behaviour, confirmed by

        // exactVecScanByHashSeq scoring solely the SQL-resolved allowed hash_seq set

        // in qmd/src/store.ts -- can correctly surface the weaker authorized

        // candidate here.

        val query = listOf(1.0, 0.0, 0.0)

        val authorized = ExperimentalParkerCanonicalMemory(

            "weak-but-authorized",

            "the owner's synthetic emergency vet is Harbour Animal Clinic",

            listOf(0.6, 0.8, 0.0),

        )

        val noiseCandidates = (1..40).map { index ->

            ExperimentalParkerCanonicalMemory(

                "noise-$index",

                "irrelevant unauthorized filler content $index",

                listOf(1.0, 0.0, index * 1e-6),

            )

        }

        val bridge = RealQmdAuthorizedVectorProcessBridge(noiseCandidates + authorized, query)

        val adapter = ExperimentalQmdCanonicalMemoryAdapter(bridge)



        val result = adapter.rank(paraphrase, listOf(authorized), maximumResults = 1)



        assertEquals(listOf(authorized.qmdVirtualPath()), result.map { it.qmdVirtualPath() })

        assertSame(authorized, result.single())

        assertEquals(listOf(authorized.qmdVirtualPath()), bridge.allowedPathCalls.single())

    }

}

