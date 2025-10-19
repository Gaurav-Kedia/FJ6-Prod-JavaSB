package com.foreverjava.service;

import com.foreverjava.Dto.CodeExecutionRequest;
import com.foreverjava.Dto.CodeExecutionResponse;
import com.foreverjava.config.ExecutionSandboxProperties;
import com.foreverjava.exception.CodeExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeExecutionService.class);
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final Pattern CLASS_PATTERN = Pattern.compile("public\\s+class\\s+(\\w+)");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w\\.]+)\\s*;");

    private final ExecutionSandboxProperties properties;
    private final Executor executor;

    public CodeExecutionService(ExecutionSandboxProperties properties, Executor codeExecutionExecutor) {
        this.properties = properties;
        this.executor = codeExecutionExecutor;
        try {
            Files.createDirectories(properties.getBaseDir());
        } catch (IOException e) {
            throw new CodeExecutionException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to initialise sandbox directory", e);
        }
    }

    public CompletableFuture<CodeExecutionResponse> executeAsync(CodeExecutionRequest request) {
        return CompletableFuture.supplyAsync(() -> executeInternal(request), executor);
    }

    private CodeExecutionResponse executeInternal(CodeExecutionRequest request) {
        String trimmedVersion = request.getJavaVersion() == null ? "" : request.getJavaVersion().trim();
        String javaVersion = trimmedVersion.isEmpty() && properties.getDefaultVersion() != null
                ? properties.getDefaultVersion().trim()
                : trimmedVersion;
        if (javaVersion == null || javaVersion.isEmpty()) {
            javaVersion = String.valueOf(Runtime.version().feature());
        }

        Path javaHome = resolveJavaHome(javaVersion);
        Path workingDirectory = createWorkingDirectory();
        LOGGER.debug("Using working directory {} for execution", workingDirectory);

        String code = request.getCode();
        if (code == null || code.isBlank()) {
            throw new CodeExecutionException(HttpStatus.BAD_REQUEST, "Code payload must not be empty");
        }

        try {
            SourceLayout sourceLayout = prepareSourceFiles(code, workingDirectory);
            List<String> compileCommand = buildCompileCommand(javaHome, sourceLayout.sourceFile());
            Instant compileStart = Instant.now();
            ProcessExecution compileExecution = runProcess(compileCommand, workingDirectory, properties.getCompileTimeout(), null, false);
            long compileTimeMillis = Duration.between(compileStart, Instant.now()).toMillis();
            boolean compileSuccessful = compileExecution.exitCode() == 0;

            if (!compileSuccessful) {
                LOGGER.warn("Compilation failed for class {} with exit code {}", sourceLayout.qualifiedClassName(), compileExecution.exitCode());
                return new CodeExecutionResponse(
                        javaVersion,
                        false,
                        compileTimeMillis,
                        compileExecution.stdout(),
                        compileExecution.stderr(),
                        false,
                        false,
                        null,
                        compileExecution.exitCode(),
                        "",
                        "",
                        null,
                        compileExecution.stdoutTruncated(),
                        compileExecution.stderrTruncated());
            }

            List<String> executionCommand = buildExecutionCommand(javaHome, sourceLayout.qualifiedClassName());
            Instant executionStart = Instant.now();
            String executionInput = request.getInput() == null ? "" : request.getInput();
            ProcessExecution execution = runProcess(executionCommand, workingDirectory, properties.getExecutionTimeout(), executionInput, true);
            long executionTimeMillis = Duration.between(executionStart, Instant.now()).toMillis();
            boolean executionSuccessful = execution.exitCode() == 0;
            Long memoryUsageKb = execution.memoryUsageKb();

            return new CodeExecutionResponse(
                    javaVersion,
                    true,
                    compileTimeMillis,
                    compileExecution.stdout(),
                    compileExecution.stderr(),
                    true,
                    executionSuccessful,
                    executionTimeMillis,
                    execution.exitCode(),
                    execution.stdout(),
                    execution.stderr(),
                    memoryUsageKb,
                    execution.stdoutTruncated(),
                    execution.stderrTruncated());
        } catch (IOException e) {
            throw new CodeExecutionException(HttpStatus.INTERNAL_SERVER_ERROR, "I/O error during code execution", e);
        } finally {
            cleanupWorkspace(workingDirectory);
        }
    }

    private Path resolveJavaHome(String version) {
        Map<String, Path> configured = properties.getJdkPaths();
        Path path = configured.get(version);
        if (path != null && path.toString().isBlank()) {
            path = null;
        }
        if (path != null && Files.exists(path)) {
            return path;
        }
        if (String.valueOf(Runtime.version().feature()).equals(version)) {
            return Paths.get(System.getProperty("java.home"));
        }
        if (configured.containsKey(version) && path == null) {
            return Paths.get(System.getProperty("java.home"));
        }
        if (configured.containsKey(version)) {
            throw new CodeExecutionException(HttpStatus.BAD_REQUEST, "Configured Java home does not exist for version " + version);
        }
        String envKey = "JDK_" + version + "_HOME";
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            Path envPath = Paths.get(envValue);
            if (Files.exists(envPath)) {
                return envPath;
            }
        }
        if ("current".equalsIgnoreCase(version)) {
            return Paths.get(System.getProperty("java.home"));
        }
        throw new CodeExecutionException(HttpStatus.BAD_REQUEST, "Unsupported or unavailable Java version: " + version);
    }

    private Path createWorkingDirectory() {
        try {
            return Files.createTempDirectory(properties.getBaseDir(), "exec-");
        } catch (IOException e) {
            throw new CodeExecutionException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create sandbox workspace", e);
        }
    }

    private SourceLayout prepareSourceFiles(String code, Path workingDirectory) throws IOException {
        Matcher classMatcher = CLASS_PATTERN.matcher(code);
        String className = classMatcher.find() ? classMatcher.group(1) : properties.getFallbackClassName();
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(code);
        String packageName = packageMatcher.find() ? packageMatcher.group(1) : null;

        Path sourceDirectory = workingDirectory;
        if (packageName != null) {
            sourceDirectory = workingDirectory.resolve(packageName.replace('.', '/'));
            Files.createDirectories(sourceDirectory);
        }

        Path sourceFile = sourceDirectory.resolve(className + ".java");
        Files.writeString(sourceFile, code, StandardCharsets.UTF_8);
        String qualifiedClassName = packageName != null ? packageName + "." + className : className;
        return new SourceLayout(sourceFile, qualifiedClassName);
    }

    private List<String> buildCompileCommand(Path javaHome, Path sourceFile) {
        Path javacPath = resolveExecutable(javaHome, "javac");
        List<String> command = new ArrayList<>();
        command.add(javacPath.toAbsolutePath().toString());
        command.add("-encoding");
        command.add("UTF-8");
        command.add(sourceFile.toAbsolutePath().toString());
        return command;
    }

    private List<String> buildExecutionCommand(Path javaHome, String qualifiedClassName) {
        Path javaPath = resolveExecutable(javaHome, "java");
        List<String> command = new ArrayList<>();
        command.add(javaPath.toAbsolutePath().toString());
        command.add("-Xms32m");
        command.add("-Xmx" + properties.getMaxMemoryMb() + "m");
        command.add("-XX:MaxRAMPercentage=75");
        command.add("-cp");
        command.add(".");
        command.add(qualifiedClassName);
        return command;
    }

    private Path resolveExecutable(Path javaHome, String binary) {
        Path binDirectory = javaHome.resolve("bin");
        String executableName = IS_WINDOWS ? binary + ".exe" : binary;
        Path executable = binDirectory.resolve(executableName);
        if (!Files.isRegularFile(executable) || !Files.isExecutable(executable)) {
            throw new CodeExecutionException(HttpStatus.INTERNAL_SERVER_ERROR, "Executable not found for " + binary + " at " + executable);
        }
        return executable;
    }

    private ProcessExecution runProcess(List<String> command,
                                        Path workingDirectory,
                                        Duration timeout,
                                        @Nullable String input,
                                        boolean trackMemory) throws IOException {
        Path stdoutFile = Files.createTempFile(workingDirectory, "stdout-", ".log");
        Path stderrFile = Files.createTempFile(workingDirectory, "stderr-", ".log");

        List<String> effectiveCommand = new ArrayList<>(command);
        Path metricsFile = null;
        if (trackMemory && !IS_WINDOWS) {
            Path timeExecutable = Path.of("/usr/bin/time");
            if (Files.isExecutable(timeExecutable)) {
                metricsFile = Files.createTempFile(workingDirectory, "metrics-", ".log");
                List<String> wrappedCommand = new ArrayList<>();
                wrappedCommand.add(timeExecutable.toString());
                wrappedCommand.add("-f");
                wrappedCommand.add("maxrss=%M");
                wrappedCommand.add("-o");
                wrappedCommand.add(metricsFile.toString());
                wrappedCommand.addAll(effectiveCommand);
                effectiveCommand = wrappedCommand;
            } else {
                LOGGER.debug("/usr/bin/time not available; memory usage will not be captured");
            }
        }

        ProcessBuilder builder = new ProcessBuilder(effectiveCommand);
        builder.directory(workingDirectory.toFile());
        builder.redirectOutput(stdoutFile.toFile());
        builder.redirectError(stderrFile.toFile());
        Process process = builder.start();

        if (input != null && !input.isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(input);
                writer.flush();
            }
        }
        process.getOutputStream().close();

        try {
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TimeoutException("Process exceeded time limit of " + timeout.toSeconds() + " seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeExecutionException(HttpStatus.INTERNAL_SERVER_ERROR, "Process interrupted", e);
        } catch (TimeoutException e) {
            throw new CodeExecutionException(HttpStatus.REQUEST_TIMEOUT, e.getMessage(), e);
        }

        int exitCode = process.exitValue();
        OutputCapture stdout = readOutput(stdoutFile);
        OutputCapture stderr = readOutput(stderrFile);
        Long memoryUsageKb = null;
        if (metricsFile != null && Files.exists(metricsFile)) {
            memoryUsageKb = parseMemoryUsage(metricsFile);
            Files.deleteIfExists(metricsFile);
        }
        Files.deleteIfExists(stdoutFile);
        Files.deleteIfExists(stderrFile);
        return new ProcessExecution(exitCode, stdout.content(), stderr.content(), stdout.truncated(), stderr.truncated(), memoryUsageKb);
    }

    private Long parseMemoryUsage(Path metricsFile) {
        try {
            List<String> lines = Files.readAllLines(metricsFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line != null && line.startsWith("maxrss=")) {
                    String value = line.substring("maxrss=".length()).trim();
                    if (!value.isEmpty()) {
                        return Long.parseLong(value);
                    }
                }
            }
        } catch (IOException | NumberFormatException ex) {
            LOGGER.debug("Failed to parse memory usage from {}", metricsFile, ex);
        }
        return null;
    }

    private OutputCapture readOutput(Path file) throws IOException {
        long size = Files.size(file);
        long limit = properties.getMaxOutputBytes();
        if (size <= limit) {
            return new OutputCapture(Files.readString(file, StandardCharsets.UTF_8), false);
        }
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] bytes = inputStream.readNBytes((int) limit);
            String truncated = new String(bytes, StandardCharsets.UTF_8) + System.lineSeparator() + "[output truncated]";
            return new OutputCapture(truncated, true);
        }
    }

    private void cleanupWorkspace(Path workingDirectory) {
        if (!properties.isCleanup()) {
            LOGGER.debug("Preserving working directory {} for inspection", workingDirectory);
            return;
        }
        try {
            Files.walk(workingDirectory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            LOGGER.warn("Failed to delete {}", path, e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.warn("Unable to clean working directory {}", workingDirectory, e);
        }
    }

    private record SourceLayout(Path sourceFile, String qualifiedClassName) {
    }

    private record ProcessExecution(int exitCode,
                                    String stdout,
                                    String stderr,
                                    boolean stdoutTruncated,
                                    boolean stderrTruncated,
                                    Long memoryUsageKb) {
    }

    private record OutputCapture(String content, boolean truncated) {
    }
}
