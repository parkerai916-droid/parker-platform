package parker.composition

import java.time.Instant

/**
 * Sprint 10, Unit 4 (Production Composition Root). A minimal,
 * dependency-free logging seam -- no external logging library is added
 * (`build.gradle.kts` gains no new dependency for this Unit), mirroring
 * `LocalHttpModelInferenceClient`'s own established precedent of
 * hand-rolling a small mechanism rather than introducing a new Gradle
 * dependency for one Unit's own use (`docs/implementation/SPRINT_9_HANDOVER.md`).
 *
 * **Constructor injection only.** Every composition-root component that
 * logs takes a [ParkerLogger] as an explicit constructor parameter. There
 * is no static `LoggerFactory`, no companion-object logger, and no global
 * lookup anywhere in this package -- per this Unit's own governing
 * instruction that no runtime component may retrieve a dependency
 * globally or construct its own collaborator.
 *
 * **What is logged, and what is deliberately never logged.** Every call
 * site in this package logs structural facts only -- event types,
 * correlation IDs, outcome kinds, exception types/messages, character
 * counts -- never `InboundOwnerMessage.text`, `OutboundParkerResponse.text`,
 * or any other owner-authored conversation content. This is a deliberate,
 * disclosed constraint (task instruction: "Logging should aid diagnosis
 * without exposing sensitive conversation contents"), not an oversight.
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

fun interface ParkerLogger {
    fun log(level: LogLevel, message: String, throwable: Throwable?)
}

fun ParkerLogger.debug(message: String) = log(LogLevel.DEBUG, message, null)
fun ParkerLogger.info(message: String) = log(LogLevel.INFO, message, null)
fun ParkerLogger.warn(message: String, throwable: Throwable? = null) = log(LogLevel.WARN, message, throwable)
fun ParkerLogger.error(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)

/**
 * The default, real [ParkerLogger]: writes a timestamped, leveled,
 * component-tagged line to `System.out` (`DEBUG`/`INFO`) or `System.err`
 * (`WARN`/`ERROR`), with the throwable's own stack trace appended when
 * present. [component] is supplied once per instance, at construction --
 * every log line this instance ever writes carries it, so a composition
 * root wiring several decorators can tell their output apart without
 * either decorator needing to know its own class name reflectively.
 *
 * [clock] defaults to [Instant.now] but is constructor-overridable so a
 * test can assert exact log content without depending on wall-clock time
 * -- the same "supplied, not hardcoded" discipline this repository
 * already applies to `Instant.now()` call sites elsewhere.
 */
class ConsoleParkerLogger(
    private val component: String,
    private val clock: () -> Instant = Instant::now,
) : ParkerLogger {

    override fun log(level: LogLevel, message: String, throwable: Throwable?) {
        val stream = if (level == LogLevel.WARN || level == LogLevel.ERROR) System.err else System.out
        stream.println("${clock()} [$level] [$component] $message")
        throwable?.printStackTrace(stream)
    }
}
