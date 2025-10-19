# FJ6-Prod-JavaSB

Java Spring Boot application for ForeverJava in the FJ6 EC2 production environment.

## High-Level Architecture

```mermaid
flowchart TD
    A[Client / Frontend] -->|POST /java with code + JDK version| B[Server_Controller]
    B -->|Validation & logging| C[CodeExecutionService]
    C -->|Resolve sandbox + JDK| D[SandboxConfig & ExecutionSandboxProperties]
    D -->|Provision isolated workspace| E[Temporary Sandbox Directory]
    C -->|Compile code (javac)| F[Compile Process]
    F -->|Compilation stdout/stderr| C
    C -->|Run compiled class| G[Execution Process]
    G -->|Capture output, memory, timing| C
    C -->|Compose response DTO| H[CodeExecutionResponse]
    H -->|Async CompletableFuture| I[Server_Controller]
    I -->|HTTP 200 with metrics| A
```

1. **Server_Controller** accepts the request, performs validation and logging, and delegates to the async `CodeExecutionService`.
2. **CodeExecutionService** resolves the requested JDK, prepares an isolated sandbox, writes the source file, then runs compilation and execution commands.
3. **SandboxConfig & ExecutionSandboxProperties** control resource limits, default Java versions, and sandbox directories for per-request isolation.
4. **CodeExecutionResponse** captures compilation results, runtime output, timing, exit codes, and memory metrics before being returned to the caller.

Additional endpoints within `Server_Controller` expose AI generation, credential access, and data retrieval services backed by the supporting readers, utilities, and Hibernate components.

## Repository Folder Structure

```text
.
├── .github/workflows/Deploy.yml
├── .mvn/wrapper/
│   ├── maven-wrapper.jar
│   └── maven-wrapper.properties
├── .run/FJ-java.run.xml
├── LICENSE
├── README.md
├── Scripts/Script.sh
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src/main/
    ├── java/com/foreverjava/
    │   ├── AppLauncher.java
    │   ├── Controller/
    │   │   ├── Server_Controller.java
    │   │   └── advice/GlobalExceptionHandler.java
    │   ├── Dto/
    │   │   ├── ApiRequestLogDTO.java
    │   │   ├── ApiResponseDTO.java
    │   │   ├── CodeExecutionRequest.java
    │   │   ├── CodeExecutionResponse.java
    │   │   ├── RoleDTO.java
    │   │   └── UserIPv6DTO.java
    │   ├── Reader/
    │   │   ├── ApiRequestService.java
    │   │   ├── GeminiService.java
    │   │   ├── JavaReaderService.java
    │   │   └── XmlReaderService.java
    │   ├── Util/
    │   │   ├── Creds.java
    │   │   ├── FJCryptoUtil.java
    │   │   ├── FileConfigUtil.java
    │   │   ├── HttpUtilForGeminiAIResponse.java
    │   │   ├── XmlQueryLoader.java
    │   │   └── Hibernate/
    │   │       ├── Table1.java
    │   │       ├── Table1Repository.java
    │   │       └── Table1Service.java
    │   ├── config/
    │   │   ├── ExecutionSandboxProperties.java
    │   │   └── SandboxConfig.java
    │   └── exception/CodeExecutionException.java
    └── resources/
        ├── Statement.xml
        └── application.properties
```

> **Note:** The folder structure omits build output directories and focuses on the source and configuration files tracked in the repository.
