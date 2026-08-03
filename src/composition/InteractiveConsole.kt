package parker.composition

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.runtime.GoalPlanningHandoffOutcome

/**
 * Development/debug stdin adapter: `readLine -> ParkerRuntime.submitOwnerMessage
 * -> print`. Opt-in only ([Main.kt]'s `--interactive` flag) -- the normal,
 * detached production launcher path is unaffected and does not construct or
 * call this function.
 *
 * **Decoupled from [ParkerRuntime] on purpose.** [submit] is
 * `runtime::submitOwnerMessage` in production, but this function never
 * references [ParkerRuntime] itself -- so a test can exercise every branch
 * below (blank input, EOF, each [ParkerRuntimeOutcome] variant,
 * [ParkerRuntimeException.NotRunning]) against a fake, without constructing
 * a real runtime graph. [readLine] and [writeLine] are likewise injected,
 * not read from `kotlin.io.readlnOrNull`/`println` directly.
 *
 * **Reply text never comes from this function's own return path.**
 * [ParkerRuntimeOutcome.Delivered] carries only a structural
 * `ExecutionResult` (status, tool results, errors) -- no reply text field.
 * The actual reply reaches the owner exclusively through
 * `LocalTextChannelDeliverTool`'s `onOwnerNotified` callback, i.e. the
 * `OwnerNotificationSink` [ParkerRuntime] was constructed with. This
 * function's own `Delivered` branch is therefore intentionally a no-op --
 * printing the reply is [Main.kt]'s job, via the console
 * `OwnerNotificationSink` it supplies at construction, not this loop's.
 *
 * **No blanket `catch (e: Exception)`.** Only
 * [ParkerRuntimeException.NotRunning] is caught -- the one, documented,
 * expected exception [submit] can throw (a shutdown racing this loop).
 * [ParkerRuntime.submitOwnerMessage]'s own KDoc states every other
 * fault is already caught internally and returned as a
 * [ParkerRuntimeOutcome.Failed] value, never thrown; anything else reaching
 * this call site is a genuine, unanticipated programmer error and is left
 * to propagate, not swallowed.
 *
 * **No shutdown call of its own.** On EOF, this function returns; ending
 * the process (and therefore triggering [ParkerRuntime.shutdown] via the
 * one, already-registered JVM shutdown hook) is [Main.kt]'s responsibility
 * -- this function introduces no second, competing shutdown path.
 *
 * **Spinner (UX only, no pipeline effect).** While [submit] is suspended
 * for a non-blank line, an in-place `Parker is thinking... <frame>`
 * animation runs via [spinnerOutput] -- purely visual, never touches
 * [readLine]/[writeLine]/[submit]'s own contract, and never reaches
 * `ParkerRuntime` or anything downstream of it. It runs in its own child
 * coroutine ([withSpinner]), started only once [submit] is actually about
 * to be called (never for blank input or EOF, since both `continue`/
 * `return` before reaching this point), and is always cancelled, joined,
 * and its line cleared in a `finally` -- whether [submit] returns
 * normally, throws [ParkerRuntimeException.NotRunning], or (structured
 * concurrency's own guarantee) this whole coroutine is itself externally
 * cancelled. [spinnerEnabled] defaults to `true`; [spinnerOutput] defaults
 * to a real, flushing `System.out` writer; [spinnerFrameDelayMillis]
 * defaults to `100`. All three are constructor-style parameters purely so
 * a test can observe frames/clearing deterministically and disable the
 * spinner entirely without touching real `System.out` or real wall-clock
 * time -- see [withSpinner]'s own KDoc for why plain `kotlinx.coroutines.delay`
 * (not a separately injected delay function) is sufficient for that.
 *
 * **Reply/spinner interleaving is prevented, not merely disclosed.**
 * [ParkerRuntimeOutcome.Delivered]'s own reply text is still printed by a
 * completely different component (the `OwnerNotificationSink` `Main.kt`
 * supplies to `ParkerRuntime`), from inside [submit] itself -- this
 * function still cannot reach into `ParkerRuntime`/`ResponseDelivery`/
 * `LocalTextChannelDeliverTool` to change *that*. What it does control is
 * *when* that already-produced text actually reaches the terminal:
 * [beginNotificationBuffering] is called immediately before [submit], and
 * [endNotificationBufferingAndFlush] immediately after [withSpinner]
 * returns (i.e. strictly after the spinner's own `finally` has already
 * cancelled, joined, and cleared it) -- both no-ops by default, wired in
 * production ([Main.kt]) to [BufferingOwnerNotificationSink.beginBuffering]/
 * [BufferingOwnerNotificationSink.endBufferingAndFlush] on the same
 * instance `Main.kt` constructs `ParkerRuntime` with. A reply produced
 * mid-`submit` is therefore held back by that sink, not printed by it
 * until this function's own flush call -- which is always after the
 * spinner line is already gone. See [BufferingOwnerNotificationSink]'s own
 * KDoc for the buffering mechanism itself.
 *
 * **`You:` prompts.** Written via [writeLine], exactly like every other
 * status line -- once before the loop's first [readLine] call, and once
 * more after each message finishes processing (any [ParkerRuntimeOutcome]
 * variant), immediately before looping back for the next [readLine].
 * Deliberately **not** written after EOF, after the [ParkerRuntimeException.NotRunning]
 * catch, or after any other exception propagates out -- in every one of
 * those cases this function is already returning (or throwing) without
 * reaching the loop's own end, so no further prompt is ever printed once
 * input can no longer be accepted. The one-time startup banner
 * ([printInteractiveBanner]) is a deliberately separate function, called
 * by [Main.kt] before this one, not folded in here -- so callers/tests
 * that have nothing to do with the banner are unaffected by it.
 *
 * **[spinnerLineGuard] (optional).** Forwarded, unchanged, to
 * [withSpinner] -- see [SpinnerLineGuard]'s own KDoc. `null` (the
 * default) preserves this function's own prior behaviour exactly: no
 * guard, no collision handling attempted.
 */
suspend fun runInteractiveConsole(
    channelId: ModuleId,
    ownerPrincipalId: PrincipalId,
    readLine: () -> String?,
    writeLine: (String) -> Unit,
    submit: suspend (InboundOwnerMessage) -> ParkerRuntimeOutcome,
    clock: () -> Instant = Instant::now,
    spinnerEnabled: Boolean = true,
    spinnerOutput: (String) -> Unit = ::defaultSpinnerOutput,
    spinnerFrameDelayMillis: Long = SPINNER_FRAME_DELAY_MILLIS,
    beginNotificationBuffering: suspend () -> Unit = {},
    endNotificationBufferingAndFlush: suspend () -> Unit = {},
    spinnerLineGuard: SpinnerLineGuard? = null,
) {
    writeLine("You:")

    while (true) {
        val line = readLine() ?: run {
            writeLine("[eof] input closed, shutting down")
            return
        }
        if (line.isBlank()) continue

        val message = InboundOwnerMessage(
            channelId = channelId,
            senderPrincipalId = ownerPrincipalId,
            text = line,
            timestamp = clock(),
            correlationId = CorrelationId(UUID.randomUUID().toString()),
        )

        val outcome = try {
            beginNotificationBuffering()
            try {
                withSpinner(spinnerEnabled, spinnerOutput, spinnerFrameDelayMillis, spinnerLineGuard) {
                    submit(message)
                }
            } finally {
                // Strictly after withSpinner's own finally (spinner cancelled, joined, and its
                // line cleared) has already completed -- never before, whether submit() returned
                // normally, threw NotRunning (caught below), or threw anything else.
                endNotificationBufferingAndFlush()
            }
        } catch (e: ParkerRuntimeException.NotRunning) {
            writeLine("[stopped] runtime is shutting down")
            return
        }

        when (outcome) {
            // Reply text already printed by endNotificationBufferingAndFlush() above -- always
            // after the spinner was cleared, never here.
            is ParkerRuntimeOutcome.Delivered -> Unit
            is ParkerRuntimeOutcome.NotAccepted -> writeLine("[not accepted] ${outcome.reason}")
            is ParkerRuntimeOutcome.Failed -> writeLine("[error] (${outcome.stage}) ${outcome.cause.message}")
            is ParkerRuntimeOutcome.Planned -> {
                // Exhaustive over the sealed GoalPlanningHandoffOutcome, not a direct cast -- a
                // future variant added there fails this `when` to compile, rather than surviving
                // as a silent ClassCastException at runtime.
                val planningSessionResult = when (val handoffOutcome = outcome.outcome) {
                    is GoalPlanningHandoffOutcome.Planned -> handoffOutcome.planningSessionResult
                }
                writeLine("[planned] ${planningSessionResult::class.simpleName}")
            }
        }

        writeLine("You:")
    }
}

/**
 * Parker's one-time interactive startup banner -- called by [Main.kt]
 * exactly once, immediately after [ParkerRuntime.start] succeeds and
 * before [runInteractiveConsole] begins its own loop. Deliberately a
 * separate function, not folded into [runInteractiveConsole] itself, so
 * every existing [runInteractiveConsole] caller/test that has nothing to
 * do with the banner is unaffected by its own parameter list.
 *
 * Prints [modelName] and nothing else configuration-shaped -- never the
 * model endpoint URL, any secret, any `PARKER_*` environment variable
 * name or value, any evidence storage/audit path, or any internal
 * [PrincipalId] -- per this Unit's own disclosure constraint.
 */
fun printInteractiveBanner(modelName: String, writeLine: (String) -> Unit) {
    writeLine("Parker")
    writeLine("Local governed AI runtime")
    writeLine("Model: $modelName")
    writeLine("Type Ctrl+C to exit.")
}

/**
 * Composition-layer [OwnerNotificationSink] decorator: while "buffering"
 * ([beginBuffering] has been called, [endBufferingAndFlush] has not yet
 * matched it), a [notify] call is held in memory instead of reaching
 * [delegate] immediately. [endBufferingAndFlush] flushes whatever
 * accumulated -- in arrival order, each exactly once, then clears the
 * buffer -- through [delegate], and turns buffering back off.
 *
 * **Why this exists.** [runInteractiveConsole]'s own spinner and
 * [ParkerRuntime.submitOwnerMessage]'s reply delivery both write to the
 * same terminal, but from two different call stacks that would otherwise
 * race: [delegate]`.notify` (the real `println("Parker: ...")`) is called
 * from deep inside `submit` -- while [runInteractiveConsole]'s spinner may
 * still be animating -- not after it. Wrapping the real sink in this class
 * lets [runInteractiveConsole] decide *when* an already-produced reply
 * actually reaches the terminal (after its own spinner is already gone),
 * without [ParkerRuntime], `ResponseDelivery`, or
 * `LocalTextChannelDeliverTool` needing to know anything changed --
 * [notify]'s own signature and calling convention are completely
 * unaffected; only which [OwnerNotificationSink] instance receives the
 * call is different.
 *
 * **Preserves immediate delivery outside a buffering window (Correction
 * task requirement 6).** A [notify] call that arrives while not currently
 * buffering (before the first [beginBuffering], or after a matching
 * [endBufferingAndFlush]) is forwarded to [delegate] immediately -- this
 * class never assumes it is only ever used from inside a matched
 * begin/end pair.
 *
 * **Narrowest mechanism, not held across a suspend call (PES-001 Chapter
 * 7.1's own in-memory concurrency rule).** [mutex] guards only the plain,
 * synchronous state this class owns (the `buffering` flag and the
 * buffered list) -- it is always released *before* [delegate]`.notify` is
 * called, exactly as [ParkerRuntime]'s own `stateLock` already does around
 * its injected collaborators. [delegate]`.notify` calls for a flush happen
 * sequentially, in order, entirely outside the lock.
 */
class BufferingOwnerNotificationSink(
    private val delegate: OwnerNotificationSink,
) : OwnerNotificationSink {

    private val mutex = Mutex()
    private var buffering = false
    private val buffered = mutableListOf<String>()

    override suspend fun notify(text: String) {
        val bufferedInstead = mutex.withLock {
            if (buffering) {
                buffered.add(text)
                true
            } else {
                false
            }
        }
        if (!bufferedInstead) {
            delegate.notify(text)
        }
    }

    /** Starts holding back [notify] calls instead of forwarding them. Must be matched by exactly one later [endBufferingAndFlush]. */
    suspend fun beginBuffering() {
        mutex.withLock {
            check(!buffering) { "BufferingOwnerNotificationSink.beginBuffering() called while already buffering" }
            buffering = true
        }
    }

    /** Stops buffering and forwards every held [notify] call to [delegate], in arrival order, each exactly once -- then clears the buffer. A no-op flush (no calls to [delegate]) if nothing was buffered. */
    suspend fun endBufferingAndFlush() {
        val toFlush = mutex.withLock {
            buffering = false
            val copy = buffered.toList()
            buffered.clear()
            copy
        }
        toFlush.forEach { delegate.notify(it) }
    }
}

private const val SPINNER_FRAME_DELAY_MILLIS = 100L
private const val SPINNER_LABEL = "Parker is thinking... "
private val SPINNER_FRAMES = charArrayOf('|', '/', '-', '\\')

/** `\r` + enough spaces to blank [SPINNER_LABEL] plus one frame character + `\r` again. */
private val SPINNER_CLEAR_LINE = "\r" + " ".repeat(SPINNER_LABEL.length + 1) + "\r"

/** Default [runInteractiveConsole] `spinnerOutput`: writes straight to the real console, flushed immediately so `\r` updates are visible without buffering delay. */
private fun defaultSpinnerOutput(text: String) {
    print(text)
    System.out.flush()
}

/**
 * Runs [block] with an animated `Parker is thinking...` spinner active for
 * its duration, per [runInteractiveConsole]'s own "Spinner" KDoc section.
 *
 * **Structured, cancellation-safe lifecycle.** [coroutineScope] gives the
 * spinner a real child coroutine ([launch]) sharing this call's own
 * structured-concurrency scope -- if this whole function is itself
 * cancelled (owning shutdown, coroutine scope teardown) while [block] is
 * still suspended, that cancellation reaches the spinner child exactly as
 * it reaches [block], with no separate handling needed. The explicit
 * `try`/`finally` around [block] is what guarantees the spinner is
 * cancelled, joined, and its line cleared *before* this function returns
 * or rethrows -- never left running, never skipped on an exception path.
 * `cancel()` on a coroutine currently suspended in
 * [kotlinx.coroutines.delay] is immediate (a cancellable suspending call),
 * so the animation stops within one dispatch, not up to one more
 * [frameDelayMillis] later.
 *
 * **Why a plain `delay(frameDelayMillis)`, not a separately injected delay
 * function.** [frameDelayMillis] alone is already the "delay or frame
 * timing" seam this Unit's task asked for: a test running under
 * `kotlinx.coroutines.test.runTest` controls the same virtual clock
 * `delay` suspends on (`advanceTimeBy`/`runCurrent`), so real wall-clock
 * time is never spent in tests without a second injected function adding
 * an unused extra seam.
 *
 * **[spinnerLineGuard] is armed for exactly this call's own duration.**
 * [SpinnerLineGuard.activate] is called with an action that clears this
 * exact spinner's own line ([output]`(SPINNER_CLEAR_LINE)`), immediately
 * before the spinner's child coroutine is even launched, and
 * [SpinnerLineGuard.deactivate] is called in an outer `finally` -- so a
 * `ConsoleParkerLogger` sharing the same guard clears this spinner's line
 * before printing any log emitted for this call's own duration, and never
 * does so once this call has already finished (whether normally or via
 * cancellation/exception). `null` skips registration entirely -- no
 * guard, no behaviour change from before this parameter existed.
 */
private suspend fun <T> withSpinner(
    enabled: Boolean,
    output: (String) -> Unit,
    frameDelayMillis: Long,
    spinnerLineGuard: SpinnerLineGuard?,
    block: suspend () -> T,
): T {
    if (!enabled) return block()

    spinnerLineGuard?.activate { output(SPINNER_CLEAR_LINE) }
    return try {
        coroutineScope {
            val spinnerJob = launch { runSpinnerFrames(output, frameDelayMillis) }
            try {
                block()
            } finally {
                spinnerJob.cancel()
                spinnerJob.join()
                output(SPINNER_CLEAR_LINE)
            }
        }
    } finally {
        spinnerLineGuard?.deactivate()
    }
}

/** Prints one spinner frame, in place (leading `\r`, no trailing newline), then waits, forever -- runs only as [withSpinner]'s own cancellable child. */
private suspend fun runSpinnerFrames(output: (String) -> Unit, frameDelayMillis: Long) {
    var frameIndex = 0
    while (true) {
        output("\r$SPINNER_LABEL${SPINNER_FRAMES[frameIndex % SPINNER_FRAMES.size]}")
        frameIndex++
        delay(frameDelayMillis)
    }
}
