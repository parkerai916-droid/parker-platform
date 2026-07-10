package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Proves the two pure, named default formatting functions
 * ([defaultOllamaRequestBody], [defaultOllamaResponseBody]) --
 * `docs/implementation/MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`
 * Section 6, Decision J. [LocalHttpModelInferenceClient]'s own live HTTP
 * path is not exercised here -- no real model server exists in this
 * sandbox (Review Risk 1).
 */
class ModelInferenceClientTest {

    @Test
    fun `defaultOllamaRequestBody produces the Ollama-shaped request body`() {
        val body = defaultOllamaRequestBody("what's the weather?", "llama3")

        assertEquals(
            "{\"model\":\"llama3\",\"prompt\":\"what's the weather?\",\"stream\":false}",
            body,
        )
    }

    @Test
    fun `defaultOllamaRequestBody escapes a double quote in the prompt`() {
        val body = defaultOllamaRequestBody("a\"b", "llama3")

        assertEquals(
            "{\"model\":\"llama3\",\"prompt\":\"a\\\"b\",\"stream\":false}",
            body,
        )
    }

    @Test
    fun `defaultOllamaRequestBody escapes a backslash in the prompt`() {
        val body = defaultOllamaRequestBody("a\\b", "llama3")

        assertEquals(
            "{\"model\":\"llama3\",\"prompt\":\"a\\\\b\",\"stream\":false}",
            body,
        )
    }

    @Test
    fun `defaultOllamaRequestBody escapes a newline in the prompt`() {
        val body = defaultOllamaRequestBody("a\nb", "llama3")

        assertEquals(
            "{\"model\":\"llama3\",\"prompt\":\"a\\nb\",\"stream\":false}",
            body,
        )
    }

    @Test
    fun `defaultOllamaRequestBody escapes the model name identically to the prompt`() {
        val body = defaultOllamaRequestBody("hi", "a\"b")

        assertEquals(
            "{\"model\":\"a\\\"b\",\"prompt\":\"hi\",\"stream\":false}",
            body,
        )
    }

    @Test
    fun `defaultOllamaResponseBody extracts the response field's value from a realistic payload`() {
        val raw = """{"model":"llama3","response":"REPLY:sure, on it","done":true}"""

        assertEquals("REPLY:sure, on it", defaultOllamaResponseBody(raw))
    }

    @Test
    fun `defaultOllamaResponseBody un-escapes a double quote`() {
        val raw = "{\"response\":\"a\\\"b\"}"

        assertEquals("a\"b", defaultOllamaResponseBody(raw))
    }

    @Test
    fun `defaultOllamaResponseBody un-escapes a backslash`() {
        val raw = "{\"response\":\"a\\\\b\"}"

        assertEquals("a\\b", defaultOllamaResponseBody(raw))
    }

    @Test
    fun `defaultOllamaResponseBody un-escapes a newline`() {
        val raw = "{\"response\":\"a\\nb\"}"

        assertEquals("a\nb", defaultOllamaResponseBody(raw))
    }

    @Test
    fun `defaultOllamaResponseBody throws IllegalArgumentException when the response field is missing`() {
        val raw = """{"model":"llama3","done":true}"""

        assertFailsWith<IllegalArgumentException> { defaultOllamaResponseBody(raw) }
    }

    @Test
    fun `defaultOllamaResponseBody throws IllegalArgumentException on an unterminated response string`() {
        val raw = "{\"response\":\"no closing quote"

        assertFailsWith<IllegalArgumentException> { defaultOllamaResponseBody(raw) }
    }

    @Test
    fun `a round trip through both default functions recovers the original text`() {
        val original = "quotes \" and \\ backslashes and\nnewlines\tand tabs\rand returns"
        val requestJson = defaultOllamaRequestBody(original, "llama3")
        val simulatedServerResponse = requestJson.replace("\"prompt\":", "\"response\":")

        assertEquals(original, defaultOllamaResponseBody(simulatedServerResponse))
    }
}
