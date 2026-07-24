package parker.composition

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * Focused regression coverage for the top-level `stage()` helper
 * (`src/composition/ParkerRuntime.kt`), added while resolving Android
 * Studio's "redundant suspend modifier" compiler warning at `stage`'s
 * previous location (a private member of `ParkerRuntime`).
 *
 * That review (Sprint 10, Unit 4 diagnostic pass) surfaced a second,
 * genuine defect independent of the warning itself: `stage`'s exception
 * handling caught `Exception` before checking for `CancellationException`
 * (a subtype of `Exception`), so a real coroutine cancellation occurring
 * mid-construction would have been wrapped as
 * `ParkerRuntimeException.DependencyConstructionFailed` instead of
 * propagating -- silently suppressing genuine cancellation semantics.
 * `stage` was extracted to a top-level `internal` function specifically
 * so this fix could be exercised directly, deterministically, without
 * needing to orchestrate a real mid-`ParkerRuntime.start()` cancellation
 * race. (Gradle's Kotlin plugin makes `tests/composition` a friend of
 * `src/composition`, so the `internal` top-level `stage` is visible here.)
 *
 * These tests use `runTest`: `stage` itself performs no real I/O and
 * schedules no delayed work, so there is nothing for `runTest`'s
 * virtual-time scheduler to race ahead of.
 */
class StageCancellationTest {

    @Test
    fun `a CancellationException thrown inside stage propagates unchanged, not wrapped as DependencyConstructionFailed`() = runTest {
        assertFailsWith<CancellationException> {
            stage("test stage") { throw CancellationException("simulated real cancellation") }
        }
    }

    @Test
    fun `an ordinary exception thrown inside stage is wrapped as DependencyConstructionFailed naming the stage`() = runTest {
        val thrown = assertFailsWith<ParkerRuntimeException.DependencyConstructionFailed> {
            stage("test stage") { throw IllegalStateException("boom") }
        }
        assertEquals("test stage", thrown.component)
    }

    @Test
    fun `a ParkerRuntimeException thrown inside stage propagates unchanged, not double-wrapped`() = runTest {
        val original = ParkerRuntimeException.MissingConfiguration("SOME_KEY")
        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            stage("test stage") { throw original }
        }
        assertSame(original, thrown)
    }

    @Test
    fun `a successful block returns its value unchanged`() = runTest {
        val result = stage("test stage") { 42 }
        assertEquals(42, result)
    }
}
