package parker.core.runtime

import parker.core.interfaces.DerivativeContentStorage
import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.DocumentIngestionAudit
import parker.core.interfaces.TierADocumentIngestionRouter

object TierADocumentIngestionComposition {
    fun create(
        storage: DerivativeGenerationStorage,
        audit: DocumentIngestionAudit,
        contentStorage: DerivativeContentStorage? = null,
    ): TierADocumentIngestionRouter =
        GovernedTierADocumentIngestionRouter(
            CoordinatorTierAFormatRoutes(DerivativeGenerationCoordinator(
                csvExtractor = ApacheCommonsCsvExtractor(), storage = storage, audit = audit,
                emlExtractor = ApacheJamesMime4jExtractor(), docxExtractor = ApachePoiXwpfExtractor(),
                pdfExtractor = TikaPdfStructuralExtractor(), contentStorage = contentStorage,
            )),
        )
}
