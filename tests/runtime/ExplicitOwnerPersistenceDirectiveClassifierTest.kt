package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExplicitOwnerPersistenceDirectiveClassifierTest {
    private val classifier = DefaultExplicitOwnerPersistenceDirectiveClassifier()
    private val proposition = "the test lighthouse is painted orange"

    @Test
    fun governedPositiveFormsExtractOnePreservedProposition() {
        val inputs = listOf(
            "Remember $proposition.",
            "Remember that $proposition.",
            "Please remember $proposition.",
            "Please remember that $proposition.",
            "Store in memory: $proposition.",
            "Please store in memory: $proposition.",
            "Save to memory: $proposition.",
            "Please save to memory: $proposition.",
            "Commit to memory: $proposition.",
            "Please commit to memory: $proposition.",
            "Commit $proposition to memory",
            "Please commit $proposition to memory",
            "Keep in memory: $proposition.",
            "Please keep in memory: $proposition.",
            "Keep this in memory: $proposition.",
            "Please keep this in memory: $proposition.",
            "Keep $proposition in memory",
            "Please keep $proposition in memory",
        )

        inputs.forEach { input ->
            assertEquals(
                ExplicitOwnerPersistenceDirective(proposition),
                classifier.classify(input),
                input,
            )
        }
    }

    @Test
    fun negationQuestionsStatementsAndAmbiguityAlwaysFallBack() {
        val inputs = listOf(
            "Don't remember $proposition.",
            "Do not remember $proposition.",
            "Never remember $proposition.",
            "Don't store $proposition.",
            "Do not save $proposition.",
            "Don't commit $proposition to memory.",
            "Don't keep $proposition in memory.",
            "Do you remember the test lighthouse?",
            "Can you remember the test lighthouse?",
            "Could you remember the test lighthouse?",
            "What do you remember about the test lighthouse?",
            "What did you save about the test lighthouse?",
            "I remember the test lighthouse is orange.",
            "I saved the test lighthouse yesterday.",
            "The application stores the test lighthouse.",
            "We should keep the test lighthouse in memory.",
            "Remember when the lighthouse was orange?",
            "Save the file.",
            "Store the file.",
            "Commit the code.",
            "Keep going.",
            "Save me from the lighthouse.",
        )

        inputs.forEach { input -> assertNull(classifier.classify(input), input) }
    }

    @Test
    fun blankUnsupportedAndQuestionShapedPropositionsFallBack() {
        listOf(
            "Remember ",
            "Please remember that .",
            "Store in memory:",
            "Save to memory: ",
            "Commit to memory:",
            "Keep this in memory:",
            "Remember this?",
            "Archive in memory: $proposition",
        ).forEach { input -> assertNull(classifier.classify(input), input) }
    }

    @Test
    fun wrapperMatchingIsCaseInsensitiveButPropositionCaseAndInternalPunctuationArePreserved() {
        assertEquals(
            ExplicitOwnerPersistenceDirective("Lighthouse Alpha: Orange, with WHITE trim"),
            classifier.classify("pLeAsE ReMeMbEr ThAt Lighthouse Alpha: Orange, with WHITE trim."),
        )
    }
}
