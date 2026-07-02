package parker.core.interfaces

interface Agent {
    val principalId: PrincipalId
    suspend fun start()
    suspend fun stop()
    suspend fun health(): AgentHealth
}
