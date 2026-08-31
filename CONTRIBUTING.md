# Contributing

Parker is architecture-led.

## Rules

1. Do not bypass the Execution Pipeline.
2. Do not allow models to execute tools directly.
3. Do not add hidden permissions.
4. Do not merge Memory, World Model and Context.
5. Do not give internal agents implicit privilege.
6. Do not create plugin side doors.
7. Do not weaken auditability for convenience.
8. Parker implementation, source editing, test execution, build, commit, and deployment work must occur directly on the authoritative Ubuntu Parker environment unless Steven explicitly changes the authoritative environment. Windows, WSL, OneDrive, Windows temporary directories, and Windows-side staging must not be used as implementation intermediaries. Missing convenience tooling such as `apply_patch` does not authorize crossing the environment boundary.
