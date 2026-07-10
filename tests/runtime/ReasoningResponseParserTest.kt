package parker.core.runtime

import parker.core.interfaces.ReasoningProviderResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Proves `TaggedReasoningResponseParser`'s exact classification convention
 * (`docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`
 * Section 12, Decision 3;
 * `docs/implementation/MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`
 * Section 6, Decision B).
 */
class ReasoningResponseParserTest {

    private val parser = TaggedReasoningResponseParser()

    @Test
    fun `a GOAL colon prefix produces a Goal with the trimmed remainder as text`() {
        val result = parser.parse("GOAL: book a flight")

        val goal = assertIs<ReasoningProviderResponse.Goal>(result)
        assertEquals("book a flight", goal.text)
    }

    @Test
    fun `a REPLY colon prefix produces a Reply with the trimmed remainder as text`() {
        val result = parser.parse("REPLY:  sure, on it  ")

        val reply = assertIs<ReasoningProviderResponse.Reply>(result)
        assertEquals("sure, on it", reply.text)
    }

    @Test
    fun `outer whitespace around the whole raw string is trimmed before matching`() {
        val result = parser.parse("  GOAL:hi  \n")

        val goal = assertIs<ReasoningProviderResponse.Goal>(result)
        assertEquals("hi", goal.text)
    }

    @Test
    fun `NOACTION alone, with nothing else, produces NoAction`() {
        val result = parser.parse("NOACTION")

        assertIs<ReasoningProviderResponse.NoAction>(result)
    }

    @Test
    fun `NOACTION surrounded only by outer whitespace still produces NoAction`() {
        val result = parser.parse("  NOACTION  ")

        assertIs<ReasoningProviderResponse.NoAction>(result)
    }

    @Test
    fun `a blank remainder after GOAL colon throws IllegalArgumentException, not UnclassifiableModelResponseException`() {
        assertFailsWith<IllegalArgumentException> { parser.parse("GOAL:   ") }
    }

    @Test
    fun `a blank remainder after REPLY colon throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { parser.parse("REPLY:") }
    }

    @Test
    fun `NOACTION with trailing text is unclassifiable, not silently accepted as NoAction`() {
        val exception = assertFailsWith<UnclassifiableModelResponseException> {
            parser.parse("NOACTION please")
        }
        assertEquals("NOACTION please", exception.raw)
    }

    @Test
    fun `an unrecognised leading token throws UnclassifiableModelResponseException carrying the raw text`() {
        val exception = assertFailsWith<UnclassifiableModelResponseException> {
            parser.parse("MAYBE: something")
        }
        assertEquals("MAYBE: something", exception.raw)
    }

    @Test
    fun `an empty string is unclassifiable`() {
        assertFailsWith<UnclassifiableModelResponseException> { parser.parse("") }
    }

    @Test
    fun `matching is case-sensitive -- a lowercase tag is unclassifiable`() {
        assertFailsWith<UnclassifiableModelResponseException> { parser.parse("goal:hi") }
    }

    @Test
    fun `the exception carries the original, untrimmed raw text, not the trimmed version`() {
        val exception = assertFailsWith<UnclassifiableModelResponseException> {
            parser.parse("  nonsense  ")
        }
        assertEquals("  nonsense  ", exception.raw)
    }
}
