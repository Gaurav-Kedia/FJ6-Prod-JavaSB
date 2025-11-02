# FJ6-Prod-JavaSB

Java Spring Boot application for ForeverJava in the FJ6 EC2 production environment. The backend exposes a `/java` endpoint that compiles and runs untrusted snippets inside short-lived sandboxes while reporting timing, memory, and output metrics back to the caller.

## How the `/java` execution works (step-by-step)

1. **Request lands in [`Server_Controller`](src/main/java/com/foreverjava/Controller/Server_Controller.java)**  
   A POST to `/java` brings a JSON payload with the source code, optional stdin input, and the requested JDK version. Spring automatically validates the request body thanks to `@Valid` and the constraints defined on [`CodeExecutionRequest`](src/main/java/com/foreverjava/Dto/CodeExecutionRequest.java). If a field is missing or malformed the request never reaches the service layer; [`GlobalExceptionHandler`](src/main/java/com/foreverjava/Controller/advice/GlobalExceptionHandler.java) formats a 400 response.
2. **The controller delegates to [`CodeExecutionService`](src/main/java/com/foreverjava/service/CodeExecutionService.java)**  
   The controller immediately hands the request off to `CodeExecutionService.executeAsync`, which runs on a dedicated thread pool defined in [`SandboxConfig`](src/main/java/com/foreverjava/config/SandboxConfig.java). This keeps HTTP threads free even when thousands of users submit code simultaneously.
3. **`SupportedJavaVersion` validates the JDK choice**  
   [`SupportedJavaVersion`](src/main/java/com/foreverjava/execution/SupportedJavaVersion.java) is an enum containing the exact JDK releases we provision (8, 11, 17, and 21). The enum’s custom deserializer accepts friendly inputs such as `"17"` or `"jdk-21"` but rejects anything outside the supported list, preventing “random” JDK requests from running.
4. **Sandbox settings come from [`ExecutionSandboxProperties`](src/main/java/com/foreverjava/config/ExecutionSandboxProperties.java)**  
   The service reads limits (timeouts, memory caps, queue size) and JDK installation hints from `sandbox.*` properties. Property values can point to explicit install directories, environment variables like `JDK_17_HOME`, or fall back to the JVM that launched the app when versions line up.
5. **Per-request workspace is provisioned**  
   `CodeExecutionService.createWorkingDirectory` creates a unique folder under the configured sandbox base directory (defaults to `${java.io.tmpdir}/code-exec`). Each run gets isolated `stdout`, `stderr`, and metrics files which are deleted at the end of the request to avoid leaking user data.
6. **Source code is materialised safely**
   `prepareSourceFiles` extracts the public top-level type (class, interface, enum, record, or annotation) and package name (if present) before writing exactly one `.java` file into the sandbox. This keeps the compiler from traversing arbitrary directories and allows modern constructs such as records to compile correctly.
7. **Compilation happens with explicit resource limits**  
   `buildCompileCommand` resolves the `javac` binary within the selected JDK home, then `runProcess` executes it with the configured timeout. Compiler logs are streamed into sandbox-local files so even large outputs do not overload memory.
8. **Execution is monitored for time, memory, and output**  
   On successful compilation `buildExecutionCommand` launches the compiled class with JVM flags limiting heap usage. Non-Windows systems additionally wrap execution with `/usr/bin/time` to capture peak RSS and store it alongside stdout/stderr for inclusion in the response.
9. **Results are mapped into [`CodeExecutionResponse`](src/main/java/com/foreverjava/Dto/CodeExecutionResponse.java)**  
   The service collects compile/execute exit codes, runtimes, output snippets, and the JDK version that actually ran. Jackson serialises the enum back to a plain string (e.g. `"17"`) for frontend consumers.
10. **Errors bubble back through structured handlers**  
    Any timeout, unsupported JDK, or I/O error raises a `CodeExecutionException`. The global advice translates it into a consistent JSON body, while unexpected exceptions are logged and responded to with a generic 500 message.

### Updated architecture diagram

```mermaid
flowchart TD
    A[Client / Frontend] -->|POST /java<br/>code + stdin + JDK enum| B[Server_Controller]
    B -->|Bean validation| C[CodeExecutionRequest DTO]
    C -->|Accepted versions| V[SupportedJavaVersion]
    B -->|Async dispatch| S[CodeExecutionService]
    S -->|Sandbox knobs| P[ExecutionSandboxProperties]
    P -->|Resolves base dir + JDK paths| Q[SandboxConfig & Environment]
    S -->|Create isolated workspace| W[Temp sandbox folder]
    S -->|javac (timeout + logs)| F[Compile Process]
    F -->|stdout/stderr files| W
    S -->|java execution (memory limited)| G[Execution Process]
    G -->|RSS, stdout, stderr| W
    S -->|Assemble metrics| R[CodeExecutionResponse]
    R -->|CompletableFuture| B
    B -->|HTTP 200 / 4xx / 5xx| A
```

## Sandbox and environment storage

| Asset | Where it lives | How to configure |
| --- | --- | --- |
| **Sandbox working folders** | `${sandbox.base-dir}/exec-*` on the server (defaults to `${java.io.tmpdir}/code-exec`) | `sandbox.base-dir` in [`application.properties`](src/main/resources/application.properties) or the `SANDBOX_BASE_DIR` environment variable |
| **Compiler/runtime logs** | `stdout-*.log`, `stderr-*.log`, and `metrics-*.log` files inside each sandbox folder | Automatically created and removed per request |
| **Provisioned JDK homes** | Paths supplied via `sandbox.jdk-paths.<version>` (8, 11, 17, 21) or environment variables like `JDK_17_HOME` | Either hard-code the absolute path in `application.properties` or export the environment variable before launching the app |
| **Default JDK selection** | Optional value read from `sandbox.default-version` | Provide `SANDBOX_DEFAULT_VERSION` (e.g. `JAVA_17` or `17`) to pick the version used when clients omit one |

## Supported Java versions

The backend only accepts the versions listed below. Incoming requests containing other values are rejected before any code runs.
Call `GET /java/versions` to retrieve this curated list at runtime and populate dropdowns in the UI without hardcoding it.

| Enum constant | Request payload accepted values | Environment variable for JDK home |
| --- | --- | --- |
| `JAVA_8` | `"8"`, `"JAVA_8"`, `"jdk8"` | `JDK_8_HOME` |
| `JAVA_11` | `"11"`, `"JAVA_11"`, `"jdk-11"` | `JDK_11_HOME` |
| `JAVA_17` | `"17"`, `"JAVA_17"`, `"jdk17"` | `JDK_17_HOME` |
| `JAVA_21` | `"21"`, `"JAVA_21"`, `"jdk-21"` | `JDK_21_HOME` |

> ℹ️ When the requested version matches the JVM that launched Spring Boot (for example both are JDK 17) the service safely reuses `java.home` without needing an additional installation.

## Key source files and their roles

| File | Responsibility |
| --- | --- |
| [`Server_Controller`](src/main/java/com/foreverjava/Controller/Server_Controller.java) | Receives HTTP requests, validates payloads, and invokes the async execution service. |
| [`CodeExecutionRequest`](src/main/java/com/foreverjava/Dto/CodeExecutionRequest.java) | DTO carrying user code, stdin, and the strictly validated `SupportedJavaVersion`. |
| [`SupportedJavaVersionDTO`](src/main/java/com/foreverjava/Dto/SupportedJavaVersionDTO.java) | Response DTO returned by `/java/versions` so clients can build JDK pickers dynamically. |
| [`SupportedJavaVersion`](src/main/java/com/foreverjava/execution/SupportedJavaVersion.java) | Enum + deserializer that whitelists the JDKs we allow to run inside the sandbox. |
| [`CodeExecutionService`](src/main/java/com/foreverjava/service/CodeExecutionService.java) | Orchestrates sandbox creation, compilation, execution, metrics gathering, and cleanup. |
| [`CodeExecutionResponse`](src/main/java/com/foreverjava/Dto/CodeExecutionResponse.java) | Immutable response DTO exposing compile/runtime stats and the chosen JDK. |
| [`ExecutionSandboxProperties`](src/main/java/com/foreverjava/config/ExecutionSandboxProperties.java) | Centralised configuration for timeouts, memory caps, thread pools, sandbox base directory, and known JDK locations. |
| [`SandboxConfig`](src/main/java/com/foreverjava/config/SandboxConfig.java) | Creates the dedicated thread pool that backs async execution. |
| [`GlobalExceptionHandler`](src/main/java/com/foreverjava/Controller/advice/GlobalExceptionHandler.java) | Formats validation errors, illegal argument issues, and runtime failures into consistent HTTP responses. |

## Repository layout

```text
.
├── LICENSE
├── README.md
├── Scripts/
│   └── Script.sh
├── mvnw*
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
    │   │   ├── SupportedJavaVersionDTO.java
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
    │   ├── exception/
    │   │   └── CodeExecutionException.java
    │   └── execution/
    │       └── SupportedJavaVersion.java
    └── resources/
        ├── Statement.xml
        └── application.properties
```

> **Tip:** The production deployment should mount any provisioned JDK installations on the host and expose them through the environment variables listed above. That keeps container images slim while still allowing multiple Java releases to run in parallel sandboxes.
