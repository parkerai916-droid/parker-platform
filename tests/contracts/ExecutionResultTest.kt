package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExecutionResultTest {

    private fun result(
        status: ExecutionResultStatus = ExecutionResultStatus.SUCCESS,
        errors: List<String> = emptyList(),
        completedAt: Instant? = Instant.parse("2026-01-01T00:00:05Z"),
    ) = ExecutionResult(
        resultId = ResultId("res-1"),
        requestId = RequestId("req-1"),
        status = status,
        startedAt = Instant.parse("2026-01-01T00:00:00Z"),
        completedAt = completedAt,
        errors = errors,
    )

    @Test
    fun `a successful result can be constructed`() {
        val r = result()

        assertEquals(ExecutionResultStatus.SUCCESS, r.status)
    }

    @Test
    fun `every ExecutionRequest status from the schema is representable`() {
        setOf(
            ExecutionResultStatus.SUCCESS,
            ExecutionResultStatus.PARTIAL_SUCCESS,
            ExecutionResultStatus.FAILED,
            ExecutionResultStatus.CANCELLED,
            ExecutionResultStatus.DENIED,
            ExecutionResultStatus.EXPIRED,
            ExecutionResultStatus.DEFERRED,
        ).forEach {
            if (it == ExecutionResultStatus.FAILED) {
                result(status = it, errors = listOf("boom"))
            } else {
                result(status = it)
            }
        }
    }

    @Test
    fun `a FAILED result must include a machine-readable error, per the ExecutionResult contract`() {
        assertFailsWith<IllegalArgumentException> {
            result(status = ExecutionResultStatus.FAILED, errors = emptyList())
        }
    }

    @Test
    fun `completedAt cannot precede startedAt`() {
        assertFailsWith<IllegalArgumentException> {
            result(completedAt = Instant.parse("2025-12-31T23:59:59Z"))
        }
    }

    @Test
    fun `results with identical fields are equal`() {
        assertEquals(result(), result())
    }
}
