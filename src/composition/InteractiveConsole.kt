package parker.composition

import java.time.Instant
import java.util.UUID
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
 */
suspend fun runInteractiveConsole(
    channelId: ModuleId,
    ownerPrincipalId: PrincipalId,
    readLine: () -> String?,
    writeLine: (String) -> Unit,
    submit: suspend (InboundOwnerMessage) -> ParkerRuntimeOutcome,
    clock: () -> Instant = Instant::now,
) {
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
            submit(message)
        } catch (e: ParkerRuntimeException.NotRunning) {
            writeLine("[stopped] runtime is shutting down")
            return
        }

        when (outcome) {
            // Reply text already printed by the console OwnerNotificationSink Main.kt supplied.
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
    }
}
