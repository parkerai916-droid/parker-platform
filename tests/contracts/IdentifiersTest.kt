package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class IdentifiersTest {

    @Test
    fun `identifiers with equal values are equal`() {
        assertEquals(PrincipalId("p-1"), PrincipalId("p-1"))
        assertEquals(ResourceId("r-1"), ResourceId("r-1"))
        assertEquals(RequestId("req-1"), RequestId("req-1"))
    }

    @Test
    fun `identifiers with different values are not equal`() {
        assertNotEquals(PrincipalId("p-1"), PrincipalId("p-2"))
    }

    @Test
    fun `blank identifiers are rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { PrincipalId("") }
        assertFailsWith<IllegalArgumentException> { ResourceId("   ") }
        assertFailsWith<IllegalArgumentException> { RequestId("") }
        assertFailsWith<IllegalArgumentException> { DecisionId("") }
        assertFailsWith<IllegalArgumentException> { ResultId("") }
    }
}
