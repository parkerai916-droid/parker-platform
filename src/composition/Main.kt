package parker.composition

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId

/**
 * Parker Runtime's own OS process entry point --
 * `docs/architecture/IMPLEMENTATION_GAPS.md`'s own disclosed "no live
 * entry point exists" item. Loads [ParkerRuntimeConfig] from the real
 * process environment via [ParkerRuntimeConfigLoader], constructs
 * [ParkerRuntime] from it, starts it, and keeps the process alive until a
 * termination signal (SIGTERM, Ctrl-C) triggers a JVM shutdown hook that
 * calls [ParkerRuntime.shutdown] before the process exits.
 *
 * **`--interactive` (development/debug only).** Passing `--interactive` as
 * a program argument (e.g. `docker compose run --rm parker --interactive`)
 * switches the tail of this function from the headless wait below to
 * [runInteractiveConsole] -- a stdin adapter (`readLine ->
 * ParkerRuntime.submitOwnerMessage -> print`). This is strictly opt-in:
 * with no arguments (the normal, detached production path -- `docker
 * compose up -d`, or this same image run with no override command), this
 * function's behaviour is completely unchanged from before this flag
 * existed. Introduces no new runtime component, coordinator, or inbound
 * message source beyond that one adapter: [ParkerRuntime.submitOwnerMessage]
 * remains uncalled in headless mode, exactly as it is uncalled from
 * anywhere else in this repository outside its own tests.
 *
 * Lifecycle coordination between the shutdown hook thread and this
 * function's own suspended wait is coroutine-native
 * ([CompletableDeferred]), not a raw thread-blocking primitive --
 * [shutdownComplete] is completed exactly once, by the shutdown hook,
 * after [ParkerRuntime.shutdown] itself finishes (successfully or not),
 * and this function's own `runBlocking` body suspends on it via `await()`
 * rather than parking the main thread outside coroutine machinery.
 *
 * **One shutdown path, not two.** Whether triggered by a real termination
 * signal (headless or interactive) or by [runInteractiveConsole] returning
 * on EOF (interactive only, via this function's own [exitProcess] call
 * below), [ParkerRuntime.shutdown] is only ever invoked from the single
 * shutdown hook registered here. [exitProcess] does not return; per
 * `Runtime.exit`'s own documented contract it runs every registered
 * shutdown hook to completion first, then halts the JVM with the supplied
 * status -- so EOF exits through this same graceful path and yields exit
 * code `0`, never a second, independent shutdown implementation.
 */
fun main(args: Array<String>) = runBlocking {
    // Config hasn't loaded yet, so its own logLevel isn't known -- this bootstrap logger uses
    // ConsoleParkerLogger's own default (LogLevel.INFO) and is used for nothing beyond reporting
    // a config-load failure, which always logs at ERROR (always shown regardless of threshold).
    val bootstrapLogger = ConsoleParkerLogger("main")
    val interactive = "--interactive" in args

    val config = try {
        ParkerRuntimeConfigLoader.load(System.getenv())
    } catch (e: ParkerRuntimeException) {
        bootstrapLogger.error("Parker Runtime configuration invalid", e)
        exitProcess(1)
    }

    val logger = ConsoleParkerLogger("main", minLevel = config.logLevel)

    // Interactive mode's own OwnerNotificationSink prints only the delivered reply text, with a
    // fixed, stable prefix -- no formatting framework, no change to how a reply is produced or
    // delivered (LocalTextChannelDeliverTool/ResponseDelivery/ExecutionPipeline are all
    // untouched); this is the same extension seam headless mode already defaults through
    // (LoggingOwnerNotificationSink), just a different sink. Wrapped in
    // BufferingOwnerNotificationSink so runInteractiveConsole can hold a reply back until its own
    // spinner has already been cleared (see that class's own KDoc) -- constructing it costs
    // nothing and has no effect unless it's actually passed to ParkerRuntime below, which only
    // happens in the interactive branch. Headless mode passes no third argument at all, so it
    // keeps ParkerRuntime's own default (LoggingOwnerNotificationSink) completely unchanged.
    val interactiveNotificationSink = BufferingOwnerNotificationSink(
        OwnerNotificationSink { text -> println("Parker: $text") },
    )
    val runtime = if (interactive) {
        ParkerRuntime(config, logger, interactiveNotificationSink)
    } else {
        ParkerRuntime(config, logger)
    }

    try {
        runtime.start()
    } catch (e: ParkerRuntimeException) {
        logger.error("Parker Runtime failed to start", e)
        runCatching { runtime.shutdown() }
        exitProcess(1)
    }

    val shutdownComplete = CompletableDeferred<Unit>()
    Runtime.getRuntime().addShutdownHook(
        Thread(
            {
                logger.info("Shutdown signal received")
                runBlocking {
                    try {
                        runtime.shutdown()
                    } catch (e: ParkerRuntimeException) {
                        logger.error("Parker Runtime did not shut down cleanly", e)
                    } catch (e: Exception) {
                        logger.error("Unexpected failure while shutting down Parker Runtime", e)
                    } finally {
                        shutdownComplete.complete(Unit)
                    }
                }
            },
            "parker-shutdown",
        ),
    )

    if (interactive) {
        runInteractiveConsole(
            channelId = ModuleId(config.localTextChannelModuleId),
            ownerPrincipalId = PrincipalId(config.ownerPrincipalId),
            readLine = ::readlnOrNull,
            writeLine = { line -> println(line) },
            submit = runtime::submitOwnerMessage,
            beginNotificationBuffering = interactiveNotificationSink::beginBuffering,
            endNotificationBufferingAndFlush = interactiveNotificationSink::endBufferingAndFlush,
        )
        exitProcess(0)
    } else {
        shutdownComplete.await()
    }
}
