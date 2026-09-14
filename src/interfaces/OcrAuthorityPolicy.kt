package parker.core.interfaces

/** The authority class of OCR text persisted alongside Parker evidence. */
enum class OcrAuthorityClassification {
    EXTERNAL_AUTHORITATIVE,
    LOCAL_PRELIMINARY,
}

/**
 * Central OCR authority policy. Local OCR remains permitted as an operational diagnostic, but
 * only an authorised external provider can produce the OCR representation used as governed
 * evidence for analysis and retrieval.
 */
object OcrAuthorityPolicy {
    const val AUTHORITATIVE_PROVIDER_CLASS = "EXTERNAL"
    const val LOCAL_OCR_AUTHORITATIVE = false
    const val LOCAL_OCR_PERMITTED_FOR_DIAGNOSTICS = true

    fun classify(result: OcrRecognitionResult): OcrAuthorityClassification =
        if (result.providerProvenance != null) OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE
        else OcrAuthorityClassification.LOCAL_PRELIMINARY

    fun isAuthoritative(classification: OcrAuthorityClassification?): Boolean =
        classification == OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE
}
