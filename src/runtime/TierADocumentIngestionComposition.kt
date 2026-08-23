package parker.core.runtime

import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.DocumentIngestionAudit
import parker.core.interfaces.TierADocumentIngestionRouter

object TierADocumentIngestionComposition {
    fun create(storage: DerivativeGenerationStorage, audit: DocumentIngestionAudit): TierADocumentIngestionRouter =
        GovernedTierADocumentIngestionRouter(
            CoordinatorTierAFormatRoutes(DerivativeGenerationCoordinator(
                csvExtractor = ApacheCommonsCsvExtractor(), storage = storage, audit = audit,
                emlExtractor = ApacheJamesMime4jExtractor(), docxExtractor = ApachePoiXwpfExtractor(),
                pdfExtractor = TikaPdfStructuralExtractor(),
            )),
        )
}
