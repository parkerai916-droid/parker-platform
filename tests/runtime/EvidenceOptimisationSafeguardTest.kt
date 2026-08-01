package parker.core.runtime

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import parker.composition.ParkerRuntime
import parker.core.interfaces.EvidenceArtifactStorage
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceDeletionAudit
import parker.core.interfaces.OwnerEvidenceDeletionAuthority
import parker.core.interfaces.Tool

/**
 * Evidence Custodian, Implementation Plan Phase 8 ("Optimisation
 * Safeguard Enforcement"). Verification-only -- no production file is
 * changed by this Unit. Proves, structurally, what the Phase 8 Planning
 * Review found: the current architecture already satisfies the
 * Constitutional Optimisation Safeguard (Article IX, as amended; CDR-006;
 * Scope Lock Sections 4 and 8) because no code path outside the two
 * classes Phase 7 authorised can even *obtain a reference* capable of
 * destroying a preserved original -- not because no one has tried to
 * call `delete` outside those classes today, but because no other class
 * in the scanned packages can hold, receive, or return a value of a
 * deletion-capable type at all.
 *
 * ## Why this enumerates compiled classes rather than pattern-matching source text
 *
 * Structural verification is preferred over source-text scanning
 * wherever a structural mechanism can prove the same invariant.
 * [classesInPackage] discovers every top-level class the JVM classloader
 * can see under a given package by listing the compiled `.class` files
 * Gradle already produced for it -- never a hand-maintained list of
 * "classes we remembered to check." A future class added anywhere under
 * `parker.core.runtime` or `parker.composition` is automatically
 * included the next time this test runs; nothing here needs updating
 * when either package grows. [referencedTypes] then inspects that
 * class's own constructor parameters, declared function parameters, and
 * declared member properties via Kotlin reflection -- a structural
 * question ("can this class hold a reference to X at all?"), not a
 * textual one ("does this file's source contain the substring
 * `.delete(`?"). This subsumes source-text call-site scanning: a class
 * that cannot obtain a reference to a deletion-capable type cannot call
 * a method on it through any mechanism -- direct call, reflection, an
 * extension function, or a captured lambda -- because every one of those
 * still requires holding a reference first.
 *
 * ## One disclosed, accepted limitation
 *
 * This scan cannot see a class that bypasses dependency injection
 * entirely by *locally constructing* a new
 * [FileSystemEvidenceArtifactStorage]/[InMemoryEvidenceArtifactStorage]
 * instance inside a method body and calling `delete` on it immediately,
 * without ever storing the reference as a constructor parameter,
 * function parameter, or property -- reflection cannot see local
 * variable usage inside a method body, and closing that gap fully would
 * require bytecode or AST analysis, a new dependency "keep the
 * implementation minimal" does not authorise here. This is a narrow,
 * disclosed residual risk, not a silently accepted one:
 * `ParkerRuntimeStartupAndShutdownTest.kt`'s own existing structural test
 * already establishes, separately, that every construction site in the
 * production composition root supplies its dependencies from
 * already-existing local variables rather than ad hoc inline
 * construction -- the same discipline that would make this specific
 * bypass conspicuous, not the invisible one this Unit's own scan cannot
 * see on its own.
 *
 * ## Scope: `parker.core.runtime` and `parker.composition` only
 *
 * These are the two packages where concrete objects are actually
 * constructed and wired together; `parker.core.interfaces` and
 * `src/contracts` hold interface and data-class declarations with no
 * dependency-injection concern of their own. Narrowing the scan to where
 * the constitutional risk actually lives keeps this Unit minimal without
 * weakening what it proves.
 */
class EvidenceOptimisationSafeguardTest {

    private val scannedPackages = listOf("parker.core.runtime", "parker.composition")

    private val sensitiveTypes = setOf(EvidenceArtifactStorage::class, EvidenceDeletionAudit::class)

    /**
     * The only two (holder, dependency) pairs this Programme has ever
     * authorised -- frozen by Phase 3 (Unit 2) and Phase 7 respectively.
     * Any other class referencing a sensitive type is a violation this
     * test reports by name, not a silent pass.
     */
    private val allowedHolders: Map<KClass<*>, Set<KClass<*>>> = mapOf(
        DefaultEvidenceCustodian::class to setOf(EvidenceArtifactStorage::class),
        DefaultOwnerEvidenceDeletionAuthority::class to setOf(
            EvidenceArtifactStorage::class,
            EvidenceDeletionAudit::class,
        ),
    )

    // --- Excluded operations (CDR-006's illustrative Optimisation Safeguard list) ---

    @Test
    fun `no Evidence Custodian type declares a compact, optimise, prune, replace, or discard operation`() {
        val excludedNames = listOf("compact", "optimise", "optimize", "prune", "replace", "discard", "purge", "evict")
        val typesToCheck = listOf(
            EvidenceCustodian::class,
            EvidenceArtifactStorage::class,
            OwnerEvidenceDeletionAuthority::class,
        )

        typesToCheck.forEach { type ->
            val declaredNames = type.declaredFunctions.map { it.name.lowercase() }
            excludedNames.forEach { excluded ->
                assertTrue(
                    excluded !in declaredNames,
                    "${type.simpleName} must not declare a '$excluded' operation -- the Constitutional " +
                        "Optimisation Safeguard forecloses any operation that could justify destroying a " +
                        "preserved original on efficiency, quality, or convenience grounds",
                )
            }
        }
    }

    @Test
    fun `neither EvidenceArtifactStorage nor OwnerEvidenceDeletionAuthority implements Tool`() {
        assertFalse(
            Tool::class.java.isAssignableFrom(EvidenceArtifactStorage::class.java),
            "EvidenceArtifactStorage must never be reachable through the generic Tool/ExecutionPipeline path",
        )
        assertFalse(
            Tool::class.java.isAssignableFrom(OwnerEvidenceDeletionAuthority::class.java),
            "OwnerEvidenceDeletionAuthority must never be reachable through the generic Tool/ExecutionPipeline path",
        )
    }

    // --- Structural dependency-reachability scan ---

    @Test
    fun `classesInPackage actually discovers a substantial number of classes -- guards against a silently vacuous scan`() {
        // If the classloader resource lookup below ever returns zero classes (a wrong resource
        // path, a build-output layout change), every other test in this class would still pass --
        // trivially, having verified nothing. This test exists solely to make that failure mode
        // loud instead of silent.
        val runtimeClasses = classesInPackage("parker.core.runtime")
        val compositionClasses = classesInPackage("parker.composition")

        assertTrue(
            runtimeClasses.size > 20,
            "expected substantially more than 20 classes under parker.core.runtime -- found " +
                "${runtimeClasses.size}; the classloader-based scan may not be resolving correctly",
        )
        assertTrue(
            compositionClasses.isNotEmpty(),
            "expected at least one class under parker.composition -- found none; the classloader-based " +
                "scan may not be resolving correctly",
        )
        assertTrue(
            DefaultOwnerEvidenceDeletionAuthority::class in runtimeClasses,
            "the scan must discover DefaultOwnerEvidenceDeletionAuthority itself, or every other " +
                "assertion in this class is checking an incomplete class list",
        )
    }

    @Test
    fun `no class outside its one authorised holder can obtain EvidenceArtifactStorage or EvidenceDeletionAudit`() {
        val violations = mutableListOf<String>()

        scannedPackages.flatMap { classesInPackage(it) }.distinct().forEach { kClass ->
            val allowed = allowedHolders[kClass] ?: emptySet()
            referencedTypes(kClass).filter { it in sensitiveTypes }.forEach { referenced ->
                if (referenced !in allowed) {
                    violations += "${kClass.qualifiedName} references ${referenced.simpleName}"
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found unauthorised access to a deletion-capable dependency -- only DefaultEvidenceCustodian " +
                "(EvidenceArtifactStorage) and DefaultOwnerEvidenceDeletionAuthority (EvidenceArtifactStorage, " +
                "EvidenceDeletionAudit) may hold either: $violations",
        )
    }

    @Test
    fun `OwnerEvidenceDeletionAuthority is held by ParkerRuntime only, following Phase 10 Runtime Integration`() {
        // Prior to Phase 10, this test asserted OwnerEvidenceDeletionAuthority was held by no
        // class anywhere -- a true statement about this Unit's own scope at the time it was
        // written. Implementation Plan Phase 10 has since wired it into the production composition
        // root, deliberately and within its own explicitly authorised scope (Phase 7 Boundary
        // Clarification Section 5: "only whatever component that later, separately governed unit
        // designates as the owner-facing entry point may ever hold a reference to it"). A scope
        // guard that starts asserting something false is not extra safety, it is a stale test that
        // would block correctly-scoped, correctly-authorised work -- the same precedent
        // EvidenceCustodianScopeTest's own revision history already established for `retrieve`.
        // What this test now proves is the invariant Phase 10 actually requires: ParkerRuntime is
        // the *only* holder -- no coordinator, reasoning provider, or module constructed anywhere
        // in ParkerRuntime's own production graph ever receives this reference.
        val holders = scannedPackages.flatMap { classesInPackage(it) }.distinct()
            .filter { OwnerEvidenceDeletionAuthority::class in referencedTypes(it) }

        assertEquals(
            setOf(ParkerRuntime::class),
            holders.toSet(),
            "OwnerEvidenceDeletionAuthority must be held by ParkerRuntime alone -- found: " +
                holders.map { it.qualifiedName },
        )
    }

    // --- Reflection helpers ---

    /**
     * Lists every top-level (non-nested, non-synthetic -- filtered by
     * excluding any file name containing `$`) compiled class the JVM
     * classloader can see directly under [packageName]. Reads the
     * classloader's own resource listing for the package directory --
     * works against Gradle's real compiled test/main output, not a
     * hand-maintained inventory.
     */
    private fun classesInPackage(packageName: String): List<KClass<*>> {
        val resourcePath = packageName.replace('.', '/')
        val classLoader = Thread.currentThread().contextClassLoader
        val classNames = mutableSetOf<String>()

        classLoader.getResources(resourcePath).asSequence().forEach { url ->
            val dir = runCatching { File(url.toURI()) }.getOrNull() ?: return@forEach
            if (dir.isDirectory) {
                dir.listFiles { file -> file.isFile && file.extension == "class" }
                    ?.map { it.nameWithoutExtension }
                    ?.filter { '$' !in it }
                    ?.forEach { classNames.add("$packageName.$it") }
            }
        }

        return classNames.mapNotNull { name -> runCatching { Class.forName(name).kotlin }.getOrNull() }
    }

    /**
     * Every type [kClass] could hold a reference to -- via a constructor
     * parameter, a declared function parameter, or a declared member
     * property -- as a flat set of classifiers. Each reflective call is
     * individually defensive (`runCatching`, defaulting to empty) since
     * not every compiled class (file-level function containers, `object`
     * declarations, sealed hierarchies) is guaranteed to answer every one
     * of these queries the same way; a single unusual class must never
     * abort this scan for every other class.
     */
    private fun referencedTypes(kClass: KClass<*>): Set<KClass<*>> {
        val fromConstructors = runCatching {
            kClass.constructors.flatMap { it.parameters }.mapNotNull { it.type.classifier as? KClass<*> }
        }.getOrDefault(emptyList())

        val fromFunctions = runCatching {
            kClass.declaredFunctions.flatMap { it.parameters }.mapNotNull { it.type.classifier as? KClass<*> }
        }.getOrDefault(emptyList())

        val fromProperties = runCatching {
            kClass.declaredMemberProperties.mapNotNull { it.returnType.classifier as? KClass<*> }
        }.getOrDefault(emptyList())

        return (fromConstructors + fromFunctions + fromProperties).toSet()
    }
}
