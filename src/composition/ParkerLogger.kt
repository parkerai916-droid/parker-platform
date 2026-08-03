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

    /**
     * A threshold-only value -- never the level of an actual log call (no
     * `ParkerLogger.off(...)` extension exists, deliberately). Declared
     * last so its ordinal is the highest of all five: `minLevel = OFF`
     * suppresses every real level via the exact same
     * `level.ordinal < minLevel.ordinal` comparison [ConsoleParkerLogger]
     * already uses, no special-cased branch required.
     */
    OFF,
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
 * component-tagged line to `System.err`, **always** -- every level, not
 * only `WARN`/`ERROR` -- with the throwable's own stack trace appended
 * when present. [component] is supplied once per instance, at
 * construction -- every log line this instance ever writes carries it, so
 * a composition root wiring several decorators can tell their output
 * apart without either decorator needing to know its own class name
 * reflectively.
 *
 * **Stdout is reserved for the owner-facing terminal (interactive-console
 * refinement task).** Previously `DEBUG`/`INFO` wrote to `System.out` and
 * only `WARN`/`ERROR` wrote to `System.err`. All four now write to
 * `System.err` unconditionally, so `System.out` is never written by this
 * class at all -- `runInteractiveConsole`'s own banner, `You:` prompts,
 * spinner, and `Parker: <reply>` line are the *only* things that ever
 * reach `System.out`. This alone does not prevent visual collision on a
 * shared terminal (both streams still render onto the same rows) -- see
 * [SpinnerLineGuard] for the other half of that.
 *
 * [minLevel] is a console display threshold, not an audit-retention
 * decision: [log] is still called, and still runs, for every level
 * regardless of [minLevel] -- audit/execution/permission events
 * (`RuntimeEventLogger`) are unconditionally produced exactly as before.
 * This class alone decides whether a given call's *line* is actually
 * written to `System.err`, by comparing [LogLevel] ordinals (`DEBUG <
 * INFO < WARN < ERROR < OFF`, the enum's own declared order); nothing
 * upstream of this class changes, and no event is dropped, only its
 * console line is suppressed. Defaults to [LogLevel.INFO].
 *
 * [clock] defaults to [Instant.now] but is constructor-overridable so a
 * test can assert exact log content without depending on wall-clock time
 * -- the same "supplied, not hardcoded" discipline this repository
 * already applies to `Instant.now()` call sites elsewhere.
 *
 * [spinnerLineGuard], when supplied, has its own
 * [SpinnerLineGuard.clearIfActive] called immediately before a line that
 * passes the [minLevel] filter is actually written -- see that class's
 * own KDoc. `null` (the default) means exactly what it did before this
 * parameter existed: no guard consulted, plain unconditional writes.
 */
class ConsoleParkerLogger(
    private val component: String,
    private val minLevel: LogLevel = LogLevel.INFO,
    private val clock: () -> Instant = Instant::now,
    private val spinnerLineGuard: SpinnerLineGuard? = null,
) : ParkerLogger {

    override fun log(level: LogLevel, message: String, throwable: Throwable?) {
        if (level.ordinal < minLevel.ordinal) return
        spinnerLineGuard?.clearIfActive()
        System.err.println("${clock()} [$level] [$component] $message")
        throwable?.printStackTrace(System.err)
    }
}

/**
 * Cross-thread signal [ConsoleParkerLogger] consults, via [clearIfActive],
 * immediately before writing a log line that has already passed its own
 * `minLevel` filter -- so a `WARN`/`ERROR` log emitted while
 * `runInteractiveConsole`'s own spinner is animating clears the spinner's
 * current stdout line first, rather than the two writes visually
 * corrupting one another on a shared terminal (stdout and stderr are
 * separate streams, but a terminal renders both onto the same rows; see
 * [ConsoleParkerLogger]'s own KDoc). [activate]/[deactivate] are called
 * only by `withSpinner`'s own lifecycle (`InteractiveConsole.kt`);
 * [ConsoleParkerLogger] never calls either -- only [clearIfActive].
 *
 * **Genuinely cross-thread, not merely cross-coroutine.** The JVM
 * shutdown hook (`Main.kt`) logs from a real [Thread], not the coroutine
 * this spinner runs on -- a `kotlinx.coroutines.sync.Mutex` would work but
 * is more than this needs. `@Volatile` alone is the entire synchronization mechanism:
 * [activate]/[deactivate]/[clearIfActive] each touch exactly one
 * reference, once. A benign, rare race exactly at the
 * activation/deactivation boundary (one log line printing without a
 * clear) is an acceptable cosmetic edge case, not a correctness bug --
 * the narrowest-mechanism discipline this composition layer's other
 * additions ([BufferingOwnerNotificationSink]) already follow.
 */
class SpinnerLineGuard {
    @Volatile
    private var clearAction: (() -> Unit)? = null

    /** Registers [clearAction] as what [clearIfActive] invokes until the matching [deactivate]. */
    fun activate(clearAction: () -> Unit) {
        this.clearAction = clearAction
    }

    /** Un-registers whatever [activate] last registered; [clearIfActive] becomes a no-op again. */
    fun deactivate() {
        clearAction = null
    }

    /** Invokes the currently-registered clear action, if any; a no-op when never activated or already deactivated. */
    fun clearIfActive() {
        clearAction?.invoke()
    }
}
