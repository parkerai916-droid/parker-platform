package parker.core.interfaces

interface ModelManager {
    suspend fun infer(request: ModelRequest): ModelResponse
    suspend fun capabilityStatus(capability: ModelCapability): CapabilityStatus
}
