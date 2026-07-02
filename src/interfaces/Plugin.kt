package parker.core.interfaces

interface Plugin {
    val manifest: PluginManifest
    suspend fun initialise(context: PluginContext): PluginStatus
    suspend fun shutdown()
}
