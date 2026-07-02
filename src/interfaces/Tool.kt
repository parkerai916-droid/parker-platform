package parker.core.interfaces

interface Tool {
    val descriptor: ToolDescriptor
    suspend fun validate(request: ExecutionRequest): ValidationResult
    suspend fun execute(request: ExecutionRequest): ToolResult
}
