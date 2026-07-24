package parker.composition

/**
 * Sprint 10, Unit 4 (Production Composition Root). Supplies
 * `LocalTextChannelDeliverTool`'s own `onOwnerNotified: suspend (text:
 * String) -> Unit` constructor parameter -- that class's own KDoc
 * documents this callback as "supplied entirely by the caller; a real,
 * human-visible display mechanism is a future, separately-scoped unit's
 * responsibility, not built or simulated here." This composition root is
 * that caller. Wrapping the plain function type in a named `fun interface`
 * (rather than passing a bare lambda directly at the construction site) is
 * a deliberate, small choice: it gives this Unit's own default
 * implementation ([LoggingOwnerNotificationSink]) and any future
 * production one (a real console, a real UI channel, a real push
 * notification) an explicit, constructor-injectable, testable seam,
 * consistent with this Unit's own governing instruction that every
 * composition-root-constructed object declare its dependencies through
 * constructor injection.
 */
fun interface OwnerNotificationSink {
    suspend fun notify(text: String)
}

/**
 * The default [OwnerNotificationSink] this composition root wires when no
 * other one is supplied. Deliberately logs only [text]'s length, never
 * [text] itself -- this is the one call site in the entire production
 * path that ever holds the owner's actual reply content in hand, and this
 * Unit's own logging discipline (see `ParkerLogger`'s KDoc) requires that
 * conversation content never reach a log line.
 */
class LoggingOwnerNotificationSink(private val logger: ParkerLogger) : OwnerNotificationSink {
    override suspend fun notify(text: String) {
        logger.info("Reply delivered to owner (${text.length} chars)")
    }
}
