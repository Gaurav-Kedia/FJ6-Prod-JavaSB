package com.foreverjava.Writer;

import com.foreverjava.Util.FileConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;

public class FileWriterClass {

	private static final Logger logger = LoggerFactory.getLogger(FileWriterClass.class);
	private static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

	private final String code;
	private final String input;
	private final String timestamp;
	private final String fileLocation;
	private StringBuilder output;

	public FileWriterClass(String code, String input, String timestamp, FileConfigUtil fileConfig) {
		this.code = code;
		this.input = input;
		this.timestamp = timestamp;
		this.fileLocation = fileConfig.getFileLocation();
		this.output = new StringBuilder();
	}

	public StringBuilder write() throws IOException {
		createFile("code_", ".java", code);
		createFile("input_", ".txt", input);
		compileTimeJavaFile();
		runtimeJavaFile();
		return output;
	}

	private void createFile(String prefix, String suffix, String content) {
		String fileName = fileLocation + prefix + timestamp + suffix;
		try (FileWriter fileWriter = new FileWriter(new File(fileName))) {
			fileWriter.write(content);
			logInfo("Successfully created file: " + fileName);
		} catch (IOException e) {
			logError("Error creating file: " + fileName, e);
		}
	}

	private void compileTimeJavaFile() throws IOException {
		String command = "javac code_" + timestamp + ".java";
		executeCommand(command, "Compilation");
	}

	private void runtimeJavaFile() throws IOException {
		String command = "java code_" + timestamp + ".java < input_" + timestamp + ".txt";
		executeCommand(command, "Execution");
	}

	private void executeCommand(String command, String phase) throws IOException {
		File location = new File(fileLocation);
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(location);

		if (isWindows) {
			builder.command("cmd.exe", "/c", command);
		} else {
			builder.command("sh", "-c", command);
		}

		Process process = builder.start();
		logProcessOutput(process, phase);
	}

	private void logProcessOutput(Process process, String phase) throws IOException {
		try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

			String line;
			while ((line = inputReader.readLine()) != null) {
				logInfo(phase + " output: " + line);
				output.append(line).append("\n");
			}
			boolean hasError = false;
			while ((line = errorReader.readLine()) != null) {
				hasError = true;
				logError(phase + " error: " + line);
				output.append(line).append("\n");
			}
			if (!hasError && "Compilation".equals(phase)) {
				output.append("Successfully Compiled\n");
			}
		}
	}

	private void logInfo(String message) {
		logger.info("[{}] [{}] - {}", new Date(), Thread.currentThread().getName(), message);
	}

	private void logError(String message) {
		logger.error("[{}] [{}] - {}", new Date(), Thread.currentThread().getName(), message);
	}

	private void logError(String message, Throwable throwable) {
		logger.error("[{}] [{}] - {}", new Date(), Thread.currentThread().getName(), message, throwable);
	}
}