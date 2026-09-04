package parker.composition

import java.nio.file.Path

/**
 * Sprint 10, Unit 4 (Production Composition Root). The runtime's own
 * configuration, field-shaped. Every field here is either required with
 * no invented default (a missing value is a startup failure, not a
 * silently-guessed one) or optional with an explicitly-stated default,
 * never both.
 *
 * @param modelEndpointUrl The real model server's HTTP endpoint, passed
 *   unchanged to `LocalHttpModelInferenceClient`. Required -- this
 *   composition root does not invent a default endpoint, since guessing
 *   one would mean silently pointing production traffic at an address no
 *   one configured.
 * @param modelName Passed unchanged to `LocalHttpModelInferenceClient`.
 *   Required, for the same reason as [modelEndpointUrl].
 * @param modelTimeoutMs Passed unchanged to `ModelReasoningProvider`'s own
 *   `timeoutMs` constructor parameter. Optional -- defaults to
 *   `ModelReasoningProvider`'s own existing default (`30_000L`), restated
 *   explicitly here rather than left to that class's default so this
 *   config's own shape is self-describing.
 * @param ownerPrincipalId The household owner's own [parker.core.interfaces.PrincipalId]
 *   value, registered with `IdentityService` at startup as a `USER`
 *   Principal. Required -- this composition root does not invent an
 *   owner identity.
 * @param ownerDisplayName Optional, defaults to `"Owner"` -- display-only,
 *   carries no trust or routing meaning.
 * @param localTextChannelModuleId The [parker.core.interfaces.ModuleId] value
 *   the Local Text Channel module is registered under. Optional, defaults
 *   to `"channel.local-text"`, matching the value already used throughout
 *   this repository's own existing Local Text Channel tests
 *   (`ResponseDeliveryTest.kt`, `LocalTextChannelDeliverToolTest.kt`).
 * @param evidenceStorageRootPath Evidence Custodian Runtime Integration
 *   (Implementation Plan Phase 10). An already-existing, writable
 *   directory, passed unchanged to `FileSystemEvidenceArtifactStorage`.
 *   Required -- this composition root does not invent a default evidence
 *   storage location, for the same reason [modelEndpointUrl] and
 *   [ownerPrincipalId] are required: guessing one would mean silently
 *   pointing production evidence storage at a location no one configured.
 * @param evidenceDeletionAuditLogPath Evidence Custodian Runtime
 *   Integration (Implementation Plan Phase 10). The exact file path
 *   (not a directory) passed unchanged to `FileSystemEvidenceDeletionAudit`
 *   -- its parent directory must already exist and be writable; the file
 *   itself is created if missing. Required, for the same reason
 *   [evidenceStorageRootPath] is required.
 * @param evidenceSourceManifestStorageRootPath Document Ingestion,
 *   Authoritative Source Manifest Foundation Implementation. An
 *   already-existing, writable directory, passed unchanged to
 *   `FileSystemEvidenceSourceManifestStorage`, mirroring
 *   [evidenceStorageRootPath]'s own exact shape and requirement -- a
 *   sibling storage root, not nested inside [evidenceStorageRootPath],
 *   since a manifest is never itself an `EvidenceArtifact`. Required,
 *   for the same reason [evidenceStorageRootPath] is required.
 * @param derivativeGenerationStorageRootPath Document Ingestion, Owner-Facing
 *   Tier A Runtime Invocation Boundary. An already-existing, writable
 *   directory, passed unchanged to `FileSystemDerivativeGenerationStorage`
 *   -- the already-governed Tier A derivative store this Programme's own
 *   Implementation Unit already built, only now wired into this
 *   composition root. Required, for the same reason
 *   [evidenceStorageRootPath] is required.
 * @param derivativeContentStorageRootPath Document Ingestion — Derivative
 *   Content Persistence and Retrieval
 *   (`DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`).
 *   An already-existing, writable directory, passed unchanged to
 *   `FileSystemDerivativeContentStorage` -- a sibling storage root to
 *   [derivativeGenerationStorageRootPath], never nested inside it, since
 *   content is a wholly separate, subordinate store from the Record
 *   (Scope Lock §4). Required, for the same reason
 *   [evidenceStorageRootPath] is required.
 * @param savedAnalysisStorageRootPath Reviewed Analysis Result — Explicit
 *   Owner Save. An already-existing, writable directory, passed unchanged
 *   to `FileSystemSavedAnalysisStorage` -- a wholly separate storage root
 *   from every other store above, never nested inside any of them, since a
 *   saved analysis is never Evidence, a derivative, Memory, or Knowledge.
 *   Required, for the same reason [evidenceStorageRootPath] is required.
 * @param documentIngestionAuditLogPath Document Ingestion, Owner-Facing
 *   Tier A Runtime Invocation Boundary. The exact file path (not a
 *   directory) passed unchanged to `FileSystemDocumentIngestionAudit` --
 *   its parent directory must already exist and be writable; the file
 *   itself is created if missing, mirroring [evidenceDeletionAuditLogPath]'s
 *   own exact shape. Required, for the same reason
 *   [evidenceStorageRootPath] is required.
 * @param memoryCoreDurabilityLogPath Memory Core Durability, Unit 8
 *   (Runtime Composition). The exact file path (not a directory) passed
 *   unchanged to `FileSystemMemoryCoreDurabilityLog` -- its parent
 *   directory must already exist and be writable; the file itself is
 *   created if missing, mirroring [evidenceDeletionAuditLogPath]'s own
 *   exact shape. Required, for the same reason [evidenceStorageRootPath]
 *   is required -- this composition root does not invent a default
 *   location for durable Memory Core records.
 * @param logLevel Console display threshold, eventually passed to
 *   `ConsoleParkerLogger`'s own `minLevel` constructor parameter -- a
 *   presentation-layer filter only, never a decision about which events
 *   `RuntimeEventLogger` itself produces (those are unconditional; see
 *   `ConsoleParkerLogger`'s own KDoc). Optional, defaults to
 *   [LogLevel.INFO] here -- this loader has no notion of "interactive"
 *   (it only ever sees an environment map), so this is its own single,
 *   mode-agnostic default. `Main.kt`'s own `resolveEffectiveLogLevel`
 *   resolves the actual, mode-sensitive default (`WARN` for
 *   `--interactive`, `INFO` headless) separately, using this value only
 *   when `PARKER_LOG_LEVEL` was genuinely absent/blank *and* an
 *   explicitly-set value (this field, when the key was present) always
 *   wins regardless of mode.
 * @param qmdNodeExecutablePath Programme 3, Unit 9.7.5 (Runtime Composition
 *   Wiring). The local `node` executable `ParkerRuntime` launches
 *   `tools/qmd-relevance-bridge.mts` with, passed unchanged to
 *   [parker.core.runtime.QmdRelevanceMechanismConfiguration.nodeExecutablePath].
 *   Optional, defaults to `"node"` -- a portable, machine-agnostic
 *   convention (rely on `PATH`), not a hard-coded developer path, mirroring
 *   `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own established
 *   `QMD_TEST_NODE ?: "node"` fallback exactly. Deliberately optional
 *   (rather than required with no default, unlike [modelEndpointUrl])
 *   because widening this composition root's own required-constructor-
 *   argument surface would force every one of this repository's other,
 *   unrelated `ParkerRuntimeConfig` construction sites (composition tests
 *   for Evidence Intelligence, Memory Core durability, the conversation
 *   pipeline, and so on -- none of which exercise Knowledge Retrieval's
 *   semantic fallback branch at all) to also supply a QMD-specific value
 *   they have no reason to care about -- exactly the "narrowest lawful
 *   composition diff" this Unit's own governing task requires, not a
 *   general configuration framework.
 * @param qmdBridgeScriptPath The absolute path to
 *   `tools/qmd-relevance-bridge.mts`, passed unchanged to
 *   [parker.core.runtime.QmdRelevanceMechanismConfiguration.bridgeScriptPath].
 *   Optional, defaults to that file's own well-known, portable,
 *   repository-relative location resolved to an absolute path at
 *   construction time -- again mirroring
 *   `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own established
 *   fallback, and again not a hard-coded developer-machine path (no `C:\`
 *   drive letter or username appears here).
 * @param qmdTsxCliPath Programme 3, Unit 9.7.5. The absolute path to a
 *   TypeScript-capable loader's CLI entry point (`tsx`'s own
 *   `dist/cli.mjs`), inserted as the sole entry of
 *   [parker.core.runtime.QmdRelevanceMechanismConfiguration.additionalNodeArguments]
 *   -- required because `tools/qmd-relevance-bridge.mts` imports QMD's own
 *   TypeScript source directly, which plain `node` cannot execute (see
 *   that script's own header comment, and `QmdRelevanceMechanism.kt`'s own
 *   KDoc on [additionalNodeArguments]). Nullable, defaults to `null` --
 *   this one genuinely has no portable, machine-agnostic default (unlike
 *   [qmdNodeExecutablePath] and [qmdBridgeScriptPath] above): where `tsx`
 *   lives is intrinsically tied to where a given deployment's own QMD
 *   checkout installed its `node_modules`, which this composition root
 *   does not, and must not, guess or hard-code (this Unit's own governing
 *   task, Phase 2: "Do not hardcode Steve's Windows paths into the
 *   reusable constitutional contract"). When left unset, the composed
 *   mechanism still constructs successfully (so every unrelated
 *   composition test above is unaffected), but the real bridge subprocess
 *   fails loudly and diagnosably -- a non-zero exit reporting node's own
 *   "unsupported file type" error -- the first time `RelevanceMechanism.rank`
 *   is genuinely invoked, never a silent empty result and never an
 *   on-demand fallback to a different mechanism (this Unit's own Phase 6).
 * @param qmdModelCacheDir Programme 3, Unit 9.7.5. Passed unchanged to
 *   [parker.core.runtime.QmdRelevanceMechanismConfiguration.modelCacheDir].
 *   Nullable, defaults to `null` -- deployment-specific, for the identical
 *   reason [qmdTsxCliPath] is nullable; `QmdRelevanceMechanismConfiguration`
 *   itself already treats `null` here as fully lawful (QMD's own internal
 *   default cache-directory resolution applies), not as a missing-required-
 *   value error, so no invented default is needed for this composition
 *   root to remain correct.
 * @param qmdTimeoutMillis Programme 3, Unit 9.7.5. Passed unchanged to
 *   [parker.core.runtime.QmdRelevanceMechanismConfiguration.timeoutMillis].
 *   Optional, defaults to `120_000L` -- restated explicitly here rather
 *   than left to that class's own shorter default (`30_000L`), because
 *   `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own real Windows
 *   evidence required `120_000` to reliably accommodate a fresh embedding-
 *   model subprocess cold-load plus per-candidate embedding computation
 *   (Frozen Boundary #10's disposable-state requirement means no model
 *   stays warm across calls) -- mirroring [modelTimeoutMs]'s own
 *   "restate the evidenced-correct value explicitly" precedent.
 * @param qmdSourceRoot Main-Promotion Gate / Production QMD Bridge
 *   Portability Correction (follow-on to Programme 3, Unit 9.7.6). The
 *   local QMD installation/checkout root -- e.g. `C:\Projects\Parker\qmd`
 *   on Steve's own Windows development machine -- `tools/qmd-relevance-bridge.mts`
 *   resolves its own `src/llm.ts` and `node_modules/node-llama-cpp/dist/index.js`
 *   imports from, dynamically, at that script's own runtime, replacing what
 *   were previously two hard-coded, Steve-specific, absolute Windows import
 *   paths baked directly into that script (a genuine production portability
 *   defect, discovered and corrected after Unit 9.7's own closure). Passed
 *   unchanged to
 *   [parker.core.runtime.QmdRelevanceMechanismConfiguration.qmdSourceRoot].
 *   Nullable, defaults to `null`, for the identical reason [qmdTsxCliPath]
 *   is nullable: this composition root does not, and must not, guess or
 *   hard-code a QMD installation location. When left unset, the composed
 *   mechanism still constructs successfully, but the real bridge subprocess
 *   fails loudly and diagnosably the first time `RelevanceMechanism.rank`
 *   is genuinely invoked -- never a silent empty result and never an
 *   on-demand fallback to any default location (this Unit's own Phase 6).
 * @param doclingPythonExecutablePath OCR Mechanism, Unit 12 ("Runtime
 *   Composition"). The local Python interpreter `ParkerRuntime` launches
 *   `tools/docling-ocr-bridge.py` with, passed unchanged to
 *   [parker.core.runtime.DoclingOcrProviderAdapterConfiguration.pythonExecutablePath].
 *   Optional, defaults to `"python3"` -- a portable, machine-agnostic
 *   convention (rely on `PATH`), mirroring [qmdNodeExecutablePath]'s own
 *   identical "portable default, never a hard-coded developer path"
 *   discipline. Deliberately never defaults to a specific virtual
 *   environment path (for example, this development machine's own
 *   `~/docling-venv`) -- a real deployment's own provisioning decides where
 *   its own Docling virtual environment lives; this composition root does
 *   not, and must not, guess it.
 * @param doclingBridgeScriptPath The absolute path to
 *   `tools/docling-ocr-bridge.py`, passed unchanged to
 *   [parker.core.runtime.DoclingOcrProviderAdapterConfiguration.bridgeScriptPath].
 *   Optional, defaults to that file's own well-known, portable,
 *   repository-relative location resolved to an absolute path at
 *   construction time, mirroring [qmdBridgeScriptPath] exactly.
 * @param doclingModelCacheDir OCR Mechanism, Unit 12. Passed unchanged to
 *   [parker.core.runtime.DoclingOcrProviderAdapterConfiguration.modelCacheDir].
 *   Nullable, defaults to `null` -- deployment-specific, mirroring
 *   [qmdModelCacheDir] exactly: `DoclingOcrProviderAdapterConfiguration`
 *   itself already treats `null` here as fully lawful (the bridge script's
 *   own default model-cache-location resolution applies), not as a
 *   missing-required-value error.
 * @param doclingTimeoutMillis OCR Mechanism, Unit 12. Passed unchanged to
 *   [parker.core.runtime.DoclingOcrProviderAdapterConfiguration.timeoutMillis].
 *   Optional, defaults to `900_000L` (15 minutes) -- the Docling
 *   Authorization's own frozen wall-clock bound
 *   (`OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md` Section
 *   6), restated explicitly here rather than left to
 *   `DoclingOcrProviderAdapterConfiguration`'s own identical default,
 *   mirroring [qmdTimeoutMillis]'s own "restate the governed value
 *   explicitly" precedent. Every other `DoclingOcrProviderAdapterConfiguration`
 *   field (resource bounds: max source bytes, image dimensions, output
 *   size, stdout/stderr caps) is a frozen governance constant, not a
 *   deployment-specific value -- this composition root does not expose
 *   those as configuration, mirroring how no prior runtime-composition
 *   Unit in this repository exposes an already-frozen numeric bound as a
 *   per-deployment override either.
 * @param ownerHttpBindAddress Owner LAN Evidence Upload. The network
 *   interface [parker.composition.OwnerEvidenceHttpServer] binds. Optional,
 *   defaults to `"0.0.0.0"` (all interfaces) -- a headless server has no
 *   way to know its own LAN-facing interface address in advance, so this
 *   mirrors how self-hosted LAN tools (for example, Ollama) already
 *   default; reachability from beyond the trusted LAN is a host
 *   firewall/Docker-port-publishing decision, entirely outside this
 *   process's own control, never widened by this default.
 * @param ownerHttpPort Owner LAN Evidence Upload. Optional; `null` (the
 *   default) means the feature is disabled entirely -- no port is opened,
 *   [Main.kt]'s own composition never constructs
 *   [parker.composition.OwnerEvidenceHttpServer] at all. Every existing
 *   deployment, test, and the Compose Desktop owner UI are completely
 *   unaffected unless this is explicitly set.
 * @param ownerHttpToken Owner LAN Evidence Upload. The single-owner bearer
 *   token every request to [parker.composition.OwnerEvidenceHttpServer]'s
 *   endpoints must present. Optional at the type level; [ParkerRuntimeConfigLoader.load]
 *   enforces that [ownerHttpPort] and this field are set together or not at
 *   all -- a port opened with no token, or a token configured with no port,
 *   is a startup configuration error, never a silently-anonymous server.
 *   Never logged, never returned in any response, never committed to this
 *   repository -- supplied only via environment/runtime configuration.
 */
data class ParkerRuntimeConfig(
    val modelEndpointUrl: String,
    val modelName: String,
    val modelTimeoutMs: Long = 30_000L,
    val ownerPrincipalId: String,
    val ownerDisplayName: String = "Owner",
    val localTextChannelModuleId: String = "channel.local-text",
    val evidenceStorageRootPath: String,
    val evidenceDeletionAuditLogPath: String,
    val evidenceSourceManifestStorageRootPath: String,
    val derivativeGenerationStorageRootPath: String,
    val derivativeContentStorageRootPath: String,
    val savedAnalysisStorageRootPath: String,
    val documentIngestionAuditLogPath: String,
    val memoryCoreDurabilityLogPath: String,
    val knowledgeItemDurabilityLogPath: String,
    val logLevel: LogLevel = LogLevel.INFO,
    val qmdNodeExecutablePath: String = "node",
    val qmdBridgeScriptPath: String = Path.of("tools", "qmd-relevance-bridge.mts").toAbsolutePath().toString(),
    val qmdTsxCliPath: String? = null,
    val qmdModelCacheDir: String? = null,
    val qmdTimeoutMillis: Long = 120_000L,
    val qmdSourceRoot: String? = null,
    val doclingPythonExecutablePath: String = "python3",
    val doclingBridgeScriptPath: String = Path.of("tools", "docling-ocr-bridge.py").toAbsolutePath().toString(),
    val doclingModelCacheDir: String? = null,
    val doclingTimeoutMillis: Long = 900_000L,
    val ownerHttpBindAddress: String = "0.0.0.0",
    val ownerHttpPort: Int? = null,
    val ownerHttpToken: String? = null,
    val openAiExternalTranscriptionEnabled: Boolean = false,
    val openAiExternalTranscriptionProviderProfilePath: String? = null,
    val openAiApiCredential: OpenAiApiCredential? = null,
    val fidelityFirstAcceptanceAuthorityStorageRootPath: String? = null,
    val fidelityFirstAttemptStorageRootPath: String? = null,
    val regionProviderStateStorageRootPath: String? = null,
    val regionAcceptanceAuthorityStorageRootPath: String? = null,
    val ordinaryRegionIngestionEnabled: Boolean = false,
    val ordinaryRegionCapabilityAcceptanceStorageRootPath: String? = null,
    val ordinaryRegionOwnerAuthorizationStorageRootPath: String? = null,
    val correctedPreparationStorageRootPath: String? = null,
    val deployedImmutableImageId: String? = null,
    val sourceCommit: String? = null,
    val productionCommit: String? = null,
    val humanFidelityReviewStorageRootPath: String? = null,
    val humanFidelityGovernanceAuditStorageRootPath: String? = null,
    val humanCorrectedRepresentationStorageRootPath: String? = null,
    val humanCorrectionAuditStorageRootPath: String? = null,
)

/**
 * Loads a [ParkerRuntimeConfig] from a supplied key/value environment map
 * -- **not** from `System.getenv()` directly inside this function, so a
 * caller (production `main`, or a test) supplies the source explicitly
 * (constructor/parameter injection, not a global lookup, per this Unit's
 * own governing instruction). A real production `main` passes
 * `System.getenv()` itself; this object never reads it on its own
 * initiative.
 *
 * Every required key missing, or present but blank, is reported as one
 * [ParkerRuntimeException.MissingConfiguration] -- "missing configuration"
 * is a named, first-class startup failure this Unit is required to handle
 * (task instruction), not a `NullPointerException` surfacing from deep
 * inside dependency construction.
 */
object ParkerRuntimeConfigLoader {

    const val KEY_MODEL_ENDPOINT_URL = "PARKER_MODEL_ENDPOINT_URL"
    const val KEY_MODEL_NAME = "PARKER_MODEL_NAME"
    const val KEY_MODEL_TIMEOUT_MS = "PARKER_MODEL_TIMEOUT_MS"
    const val KEY_OWNER_PRINCIPAL_ID = "PARKER_OWNER_PRINCIPAL_ID"
    const val KEY_OWNER_DISPLAY_NAME = "PARKER_OWNER_DISPLAY_NAME"
    const val KEY_LOCAL_TEXT_CHANNEL_MODULE_ID = "PARKER_LOCAL_TEXT_CHANNEL_MODULE_ID"
    const val KEY_EVIDENCE_STORAGE_ROOT = "PARKER_EVIDENCE_STORAGE_ROOT"
    const val KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH = "PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH"
    const val KEY_EVIDENCE_SOURCE_MANIFEST_STORAGE_ROOT = "PARKER_EVIDENCE_SOURCE_MANIFEST_STORAGE_ROOT"
    const val KEY_DERIVATIVE_GENERATION_STORAGE_ROOT = "PARKER_DERIVATIVE_GENERATION_STORAGE_ROOT"
    const val KEY_DERIVATIVE_CONTENT_STORAGE_ROOT = "PARKER_DERIVATIVE_CONTENT_STORAGE_ROOT"
    const val KEY_SAVED_ANALYSIS_STORAGE_ROOT = "PARKER_SAVED_ANALYSIS_STORAGE_ROOT"
    const val KEY_DOCUMENT_INGESTION_AUDIT_LOG_PATH = "PARKER_DOCUMENT_INGESTION_AUDIT_LOG_PATH"
    const val KEY_MEMORY_CORE_DURABILITY_LOG_PATH = "PARKER_MEMORY_CORE_DURABILITY_LOG_PATH"
    const val KEY_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH = "PARKER_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH"
    const val KEY_LOG_LEVEL = "PARKER_LOG_LEVEL"
    const val KEY_QMD_NODE_EXECUTABLE_PATH = "PARKER_QMD_NODE_EXECUTABLE_PATH"
    const val KEY_QMD_BRIDGE_SCRIPT_PATH = "PARKER_QMD_BRIDGE_SCRIPT_PATH"
    const val KEY_QMD_TSX_CLI_PATH = "PARKER_QMD_TSX_CLI_PATH"
    const val KEY_QMD_MODEL_CACHE_DIR = "PARKER_QMD_MODEL_CACHE_DIR"
    const val KEY_QMD_TIMEOUT_MILLIS = "PARKER_QMD_TIMEOUT_MILLIS"
    const val KEY_QMD_SOURCE_ROOT = "PARKER_QMD_SOURCE_ROOT"
    const val KEY_DOCLING_PYTHON_EXECUTABLE_PATH = "PARKER_DOCLING_PYTHON_EXECUTABLE_PATH"
    const val KEY_DOCLING_BRIDGE_SCRIPT_PATH = "PARKER_DOCLING_BRIDGE_SCRIPT_PATH"
    const val KEY_DOCLING_MODEL_CACHE_DIR = "PARKER_DOCLING_MODEL_CACHE_DIR"
    const val KEY_DOCLING_TIMEOUT_MILLIS = "PARKER_DOCLING_TIMEOUT_MILLIS"
    const val KEY_OWNER_HTTP_BIND_ADDRESS = "PARKER_OWNER_HTTP_BIND_ADDRESS"
    const val KEY_OWNER_HTTP_PORT = "PARKER_OWNER_HTTP_PORT"
    const val KEY_OWNER_HTTP_TOKEN = "PARKER_OWNER_HTTP_TOKEN"
    const val KEY_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED = "PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED"
    const val KEY_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH = "PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH"
    const val KEY_OPENAI_API_KEY = "PARKER_OPENAI_API_KEY"
    const val KEY_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT = "PARKER_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT"
    const val KEY_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT = "PARKER_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT"
    const val KEY_REGION_PROVIDER_STATE_STORAGE_ROOT = "PARKER_REGION_PROVIDER_STATE_STORAGE_ROOT"
    const val KEY_REGION_ACCEPTANCE_AUTHORITY_STORAGE_ROOT = "PARKER_REGION_ACCEPTANCE_AUTHORITY_STORAGE_ROOT"
    const val KEY_ORDINARY_REGION_INGESTION_ENABLED = "PARKER_ORDINARY_REGION_INGESTION_ENABLED"
    const val KEY_ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORAGE_ROOT = "PARKER_ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORAGE_ROOT"
    const val KEY_ORDINARY_REGION_OWNER_AUTHORIZATION_STORAGE_ROOT = "PARKER_ORDINARY_REGION_OWNER_AUTHORIZATION_STORAGE_ROOT"
    const val KEY_CORRECTED_PREPARATION_STORAGE_ROOT = "PARKER_CORRECTED_PREPARATION_STORAGE_ROOT"
    const val KEY_DEPLOYED_IMMUTABLE_IMAGE_ID = "PARKER_DEPLOYED_IMMUTABLE_IMAGE_ID"
    const val KEY_SOURCE_COMMIT = "PARKER_SOURCE_COMMIT"
    const val KEY_PRODUCTION_COMMIT = "PARKER_PRODUCTION_COMMIT"
    const val KEY_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT = "PARKER_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT"
    const val KEY_HUMAN_FIDELITY_GOVERNANCE_AUDIT_STORAGE_ROOT = "PARKER_HUMAN_FIDELITY_GOVERNANCE_AUDIT_STORAGE_ROOT"
    const val KEY_HUMAN_CORRECTED_REPRESENTATION_STORAGE_ROOT = "PARKER_HUMAN_CORRECTED_REPRESENTATION_STORAGE_ROOT"
    const val KEY_HUMAN_CORRECTION_AUDIT_STORAGE_ROOT = "PARKER_HUMAN_CORRECTION_AUDIT_STORAGE_ROOT"

    fun load(environment: Map<String, String>): ParkerRuntimeConfig {
        val modelTimeoutMsRaw = environment[KEY_MODEL_TIMEOUT_MS]?.takeIf { it.isNotBlank() }
        val modelTimeoutMs = if (modelTimeoutMsRaw == null) {
            30_000L
        } else {
            modelTimeoutMsRaw.toLongOrNull()
                ?: throw ParkerRuntimeException.InvalidConfiguration(
                    KEY_MODEL_TIMEOUT_MS,
                    "must be a positive integer number of milliseconds; was '$modelTimeoutMsRaw'",
                )
        }
        if (modelTimeoutMs <= 0) {
            throw ParkerRuntimeException.InvalidConfiguration(
                KEY_MODEL_TIMEOUT_MS,
                "must be a positive integer number of milliseconds; was '$modelTimeoutMsRaw'",
            )
        }

        val logLevelRaw = environment[KEY_LOG_LEVEL]?.takeIf { it.isNotBlank() }
        val logLevel = if (logLevelRaw == null) {
            LogLevel.INFO
        } else {
            try {
                LogLevel.valueOf(logLevelRaw.trim().uppercase())
            } catch (e: IllegalArgumentException) {
                throw ParkerRuntimeException.InvalidConfiguration(
                    KEY_LOG_LEVEL,
                    "must be one of DEBUG, INFO, WARN, ERROR, OFF; was '$logLevelRaw'",
                )
            }
        }

        // Programme 3, Unit 9.7.5 (Runtime Composition Wiring). Deployment-specific QMD paths only
        // -- never the frozen mechanism identity/version/configuration itself (mechanismName,
        // qmdVersion, embeddingModelUri, vectorDimension, similarityMetric, bridgeProtocolVersion),
        // which `ParkerRuntime.kt`'s own composition code supplies as fixed literals, never read
        // from this environment map, per this Unit's own governing task (Phase 2: "Do not silently
        // derive mutable retrieval-relevant values from environment state"). All six keys below
        // (including PARKER_QMD_SOURCE_ROOT, added by the Main-Promotion Gate / Production QMD
        // Bridge Portability Correction follow-on) are optional, mirroring [ParkerRuntimeConfig]'s
        // own documented reason on each field: widening this loader's required-key surface would
        // force every unrelated production/test caller to also supply QMD-specific configuration it
        // has no reason to care about.
        val qmdTimeoutMillisRaw = environment[KEY_QMD_TIMEOUT_MILLIS]?.takeIf { it.isNotBlank() }
        val qmdTimeoutMillis = if (qmdTimeoutMillisRaw == null) {
            120_000L
        } else {
            qmdTimeoutMillisRaw.toLongOrNull()
                ?: throw ParkerRuntimeException.InvalidConfiguration(
                    KEY_QMD_TIMEOUT_MILLIS,
                    "must be a positive integer number of milliseconds; was '$qmdTimeoutMillisRaw'",
                )
        }
        if (qmdTimeoutMillis <= 0) {
            throw ParkerRuntimeException.InvalidConfiguration(
                KEY_QMD_TIMEOUT_MILLIS,
                "must be a positive integer number of milliseconds; was '$qmdTimeoutMillisRaw'",
            )
        }

        // OCR Mechanism, Unit 12 ("Runtime Composition"). Deployment-specific Docling paths only --
        // never the frozen resource-bound governance constants (max source bytes, image dimensions,
        // output size, stdout/stderr caps), which DoclingOcrProviderAdapterConfiguration's own
        // already-accepted defaults already supply, never read from this environment map. All four
        // keys below are optional, mirroring the QMD keys' own identical reasoning: widening this
        // loader's required-key surface would force every unrelated production/test caller to also
        // supply Docling-specific configuration it has no reason to care about.
        val doclingTimeoutMillisRaw = environment[KEY_DOCLING_TIMEOUT_MILLIS]?.takeIf { it.isNotBlank() }
        val doclingTimeoutMillis = if (doclingTimeoutMillisRaw == null) {
            900_000L
        } else {
            doclingTimeoutMillisRaw.toLongOrNull()
                ?: throw ParkerRuntimeException.InvalidConfiguration(
                    KEY_DOCLING_TIMEOUT_MILLIS,
                    "must be a positive integer number of milliseconds; was '$doclingTimeoutMillisRaw'",
                )
        }
        if (doclingTimeoutMillis <= 0) {
            throw ParkerRuntimeException.InvalidConfiguration(
                KEY_DOCLING_TIMEOUT_MILLIS,
                "must be a positive integer number of milliseconds; was '$doclingTimeoutMillisRaw'",
            )
        }

        // Owner LAN Evidence Upload. Optional and opt-in: with no
        // PARKER_OWNER_HTTP_PORT set, ownerHttpPort stays null and
        // Main.kt never constructs OwnerEvidenceHttpServer at all --
        // every existing deployment/test is unaffected. Once a port is
        // set, a token becomes mandatory (and vice versa) so the
        // feature can never come up as an anonymous LAN file drop by
        // accident -- fail closed at startup rather than silently
        // serving unauthenticated.
        val ownerHttpBindAddress = environment[KEY_OWNER_HTTP_BIND_ADDRESS]?.takeIf { it.isNotBlank() } ?: "0.0.0.0"
        val ownerHttpPortRaw = environment[KEY_OWNER_HTTP_PORT]?.takeIf { it.isNotBlank() }
        val ownerHttpPort = ownerHttpPortRaw?.let {
            it.toIntOrNull()?.takeIf { port -> port in 1..65535 }
                ?: throw ParkerRuntimeException.InvalidConfiguration(
                    KEY_OWNER_HTTP_PORT,
                    "must be an integer TCP port in 1..65535; was '$ownerHttpPortRaw'",
                )
        }
        val ownerHttpToken = environment[KEY_OWNER_HTTP_TOKEN]?.takeIf { it.isNotBlank() }
        if ((ownerHttpPort == null) != (ownerHttpToken == null)) {
            throw ParkerRuntimeException.InvalidConfiguration(
                if (ownerHttpPort == null) KEY_OWNER_HTTP_PORT else KEY_OWNER_HTTP_TOKEN,
                "$KEY_OWNER_HTTP_PORT and $KEY_OWNER_HTTP_TOKEN must be set together or not at all " +
                    "-- an owner HTTP port with no token would be an anonymous LAN file drop",
            )
        }

        val externalTranscriptionEnabledRaw = environment[KEY_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED]?.takeIf { it.isNotBlank() }
        val externalTranscriptionEnabled = externalTranscriptionEnabledRaw?.trim()?.toBooleanStrictOrNull()
            ?: if (externalTranscriptionEnabledRaw == null) false else throw ParkerRuntimeException.InvalidConfiguration(
                KEY_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED,
                "must be true or false; was '$externalTranscriptionEnabledRaw'",
            )

        val ordinaryRegionEnabledRaw = environment[KEY_ORDINARY_REGION_INGESTION_ENABLED]?.takeIf { it.isNotBlank() }
        val ordinaryRegionEnabled = ordinaryRegionEnabledRaw?.trim()?.toBooleanStrictOrNull()
            ?: if (ordinaryRegionEnabledRaw == null) false else throw ParkerRuntimeException.InvalidConfiguration(
                KEY_ORDINARY_REGION_INGESTION_ENABLED, "must be true or false; was '$ordinaryRegionEnabledRaw'",
            )

        val acceptanceAuthorityRoot = environment[KEY_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT]?.takeIf { it.isNotBlank() }
        val acceptanceAttemptRoot = environment[KEY_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT]?.takeIf { it.isNotBlank() }
        val regionProviderStateRoot = environment[KEY_REGION_PROVIDER_STATE_STORAGE_ROOT]?.takeIf { it.isNotBlank() }
        val regionAcceptanceAuthorityRoot = environment[KEY_REGION_ACCEPTANCE_AUTHORITY_STORAGE_ROOT]?.takeIf { it.isNotBlank() }
        val ordinaryRegionCapabilityAcceptanceRoot = environment[KEY_ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORAGE_ROOT]?.takeIf { it.isNotBlank() }
        val ordinaryRegionOwnerAuthorizationRoot = environment[KEY_ORDINARY_REGION_OWNER_AUTHORIZATION_STORAGE_ROOT]?.takeIf { it.isNotBlank() }
        val correctedPreparationRoot = environment[KEY_CORRECTED_PREPARATION_STORAGE_ROOT]?.takeIf { it.isNotBlank() }
        val deployedImmutableImageId = environment[KEY_DEPLOYED_IMMUTABLE_IMAGE_ID]?.takeIf { it.isNotBlank() }
        val sourceCommit = environment[KEY_SOURCE_COMMIT]?.takeIf { it.isNotBlank() }
        val productionCommit = environment[KEY_PRODUCTION_COMMIT]?.takeIf { it.isNotBlank() }
        if (listOf(acceptanceAuthorityRoot, acceptanceAttemptRoot, productionCommit).count { it != null } !in setOf(0, 3)) {
            throw ParkerRuntimeException.InvalidConfiguration(
                KEY_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT,
                "acceptance authority root, attempt root, and production commit must be configured together",
            )
        }
        if (productionCommit != null && !Regex("^[0-9a-f]{40}$").matches(productionCommit)) {
            throw ParkerRuntimeException.InvalidConfiguration(KEY_PRODUCTION_COMMIT, "must be an exact 40-character lowercase Git commit")
        }
        if (sourceCommit != null && !Regex("^[0-9a-f]{40}$").matches(sourceCommit)) {
            throw ParkerRuntimeException.InvalidConfiguration(KEY_SOURCE_COMMIT, "must be an exact 40-character lowercase Git commit")
        }
        if (deployedImmutableImageId != null && !Regex("^sha256:[0-9a-f]{64}$").matches(deployedImmutableImageId)) {
            throw ParkerRuntimeException.InvalidConfiguration(KEY_DEPLOYED_IMMUTABLE_IMAGE_ID, "must be an immutable sha256 image id")
        }
        if (regionAcceptanceAuthorityRoot != null && listOf(deployedImmutableImageId, sourceCommit, productionCommit, acceptanceAttemptRoot, regionProviderStateRoot).any { it == null }) {
            throw ParkerRuntimeException.InvalidConfiguration(
                KEY_REGION_ACCEPTANCE_AUTHORITY_STORAGE_ROOT,
                "region acceptance authority requires immutable image, source/build/runtime commit, attempt ledger, and provider-state roots",
            )
        }
        if (regionProviderStateRoot != null && acceptanceAttemptRoot == null) {
            throw ParkerRuntimeException.InvalidConfiguration(
                KEY_REGION_PROVIDER_STATE_STORAGE_ROOT,
                "region provider state requires the governed acceptance authority root, attempt root, and production commit",
            )
        }
        if (ordinaryRegionEnabled && (!externalTranscriptionEnabled || ordinaryRegionCapabilityAcceptanceRoot == null ||
                ordinaryRegionOwnerAuthorizationRoot == null || acceptanceAttemptRoot == null ||
                regionProviderStateRoot == null || correctedPreparationRoot == null || sourceCommit == null || environment[KEY_OPENAI_API_KEY].isNullOrBlank())) {
            throw ParkerRuntimeException.InvalidConfiguration(
                KEY_ORDINARY_REGION_INGESTION_ENABLED,
                "ordinary region ingestion requires external provider readiness, exact source commit, capability-acceptance/owner-authorization/attempt/provider-state/corrected-preparation roots, and deployment-local credential",
            )
        }

        return ParkerRuntimeConfig(
            modelEndpointUrl = requireKey(environment, KEY_MODEL_ENDPOINT_URL),
            modelName = requireKey(environment, KEY_MODEL_NAME),
            modelTimeoutMs = modelTimeoutMs,
            ownerPrincipalId = requireKey(environment, KEY_OWNER_PRINCIPAL_ID),
            ownerDisplayName = environment[KEY_OWNER_DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: "Owner",
            localTextChannelModuleId = environment[KEY_LOCAL_TEXT_CHANNEL_MODULE_ID]?.takeIf { it.isNotBlank() }
                ?: "channel.local-text",
            evidenceStorageRootPath = requireKey(environment, KEY_EVIDENCE_STORAGE_ROOT),
            evidenceDeletionAuditLogPath = requireKey(environment, KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH),
            evidenceSourceManifestStorageRootPath = requireKey(environment, KEY_EVIDENCE_SOURCE_MANIFEST_STORAGE_ROOT),
            derivativeGenerationStorageRootPath = requireKey(environment, KEY_DERIVATIVE_GENERATION_STORAGE_ROOT),
            derivativeContentStorageRootPath = requireKey(environment, KEY_DERIVATIVE_CONTENT_STORAGE_ROOT),
            savedAnalysisStorageRootPath = requireKey(environment, KEY_SAVED_ANALYSIS_STORAGE_ROOT),
            documentIngestionAuditLogPath = requireKey(environment, KEY_DOCUMENT_INGESTION_AUDIT_LOG_PATH),
            memoryCoreDurabilityLogPath = requireKey(environment, KEY_MEMORY_CORE_DURABILITY_LOG_PATH),
            knowledgeItemDurabilityLogPath = requireKey(environment, KEY_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH),
            logLevel = logLevel,
            qmdNodeExecutablePath = environment[KEY_QMD_NODE_EXECUTABLE_PATH]?.takeIf { it.isNotBlank() } ?: "node",
            qmdBridgeScriptPath = environment[KEY_QMD_BRIDGE_SCRIPT_PATH]?.takeIf { it.isNotBlank() }
                ?: java.nio.file.Path.of("tools", "qmd-relevance-bridge.mts").toAbsolutePath().toString(),
            qmdTsxCliPath = environment[KEY_QMD_TSX_CLI_PATH]?.takeIf { it.isNotBlank() },
            qmdModelCacheDir = environment[KEY_QMD_MODEL_CACHE_DIR]?.takeIf { it.isNotBlank() },
            qmdTimeoutMillis = qmdTimeoutMillis,
            qmdSourceRoot = environment[KEY_QMD_SOURCE_ROOT]?.takeIf { it.isNotBlank() },
            doclingPythonExecutablePath = environment[KEY_DOCLING_PYTHON_EXECUTABLE_PATH]?.takeIf { it.isNotBlank() } ?: "python3",
            doclingBridgeScriptPath = environment[KEY_DOCLING_BRIDGE_SCRIPT_PATH]?.takeIf { it.isNotBlank() }
                ?: java.nio.file.Path.of("tools", "docling-ocr-bridge.py").toAbsolutePath().toString(),
            doclingModelCacheDir = environment[KEY_DOCLING_MODEL_CACHE_DIR]?.takeIf { it.isNotBlank() },
            doclingTimeoutMillis = doclingTimeoutMillis,
            ownerHttpBindAddress = ownerHttpBindAddress,
            ownerHttpPort = ownerHttpPort,
            ownerHttpToken = ownerHttpToken,
            openAiExternalTranscriptionEnabled = externalTranscriptionEnabled,
            openAiExternalTranscriptionProviderProfilePath =
                environment[KEY_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH]?.takeIf { it.isNotBlank() },
            openAiApiCredential = OpenAiApiCredential.fromEnvironment(environment[KEY_OPENAI_API_KEY]),
            fidelityFirstAcceptanceAuthorityStorageRootPath = acceptanceAuthorityRoot,
            fidelityFirstAttemptStorageRootPath = acceptanceAttemptRoot,
            regionProviderStateStorageRootPath = regionProviderStateRoot,
            regionAcceptanceAuthorityStorageRootPath = regionAcceptanceAuthorityRoot,
            ordinaryRegionIngestionEnabled = ordinaryRegionEnabled,
            ordinaryRegionCapabilityAcceptanceStorageRootPath = ordinaryRegionCapabilityAcceptanceRoot,
            ordinaryRegionOwnerAuthorizationStorageRootPath = ordinaryRegionOwnerAuthorizationRoot,
            correctedPreparationStorageRootPath = correctedPreparationRoot,
            deployedImmutableImageId = deployedImmutableImageId,
            sourceCommit = sourceCommit,
            productionCommit = productionCommit,
            humanFidelityReviewStorageRootPath = requireKey(environment, KEY_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT),
            humanFidelityGovernanceAuditStorageRootPath =
                requireKey(environment, KEY_HUMAN_FIDELITY_GOVERNANCE_AUDIT_STORAGE_ROOT),
            humanCorrectedRepresentationStorageRootPath =
                environment[KEY_HUMAN_CORRECTED_REPRESENTATION_STORAGE_ROOT]?.takeIf { it.isNotBlank() },
            humanCorrectionAuditStorageRootPath =
                environment[KEY_HUMAN_CORRECTION_AUDIT_STORAGE_ROOT]?.takeIf { it.isNotBlank() },
        )
    }

    private fun requireKey(environment: Map<String, String>, key: String): String =
        environment[key]?.takeIf { it.isNotBlank() } ?: throw ParkerRuntimeException.MissingConfiguration(key)
}
