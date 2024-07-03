package com.foreverjava.Writer;

import com.foreverjava.Util.FileConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class FileWriterClass {

	private static final Logger logger = LoggerFactory.getLogger(FileWriterClass.class);
	private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

	private final String code;
	private final String input;
	private final String timestamp;
	private final String fileLocation;

	public FileWriterClass(String code, String input, String timestamp, FileConfigUtil fileConfig) {
		this.code = code;
		this.input = input;
		this.timestamp = timestamp;
		this.fileLocation = fileConfig.getFileLocation();
	}

	public StringBuilder write() throws IOException {
		StringBuilder output = new StringBuilder();
		createJavaFile();
		createInputFile();
		compileJavaFile(output);
		executeJavaFile(output);
		return output;
	}

	private void createJavaFile() {
		File javaFile = new File(fileLocation + "code_" + timestamp + ".java");
		try (FileWriter writer = new FileWriter(javaFile)) {
			writer.write(code);
			logger.info("Successfully created Java file at {}", javaFile.getAbsolutePath());
		} catch (IOException e) {
			logger.error("Error creating Java file", e);
		}
	}

	private void createInputFile() {
		File inputFile = new File(fileLocation + "input_" + timestamp + ".txt");
		try (FileWriter writer = new FileWriter(inputFile)) {
			writer.write(input);
			logger.info("Successfully created input file at {}", inputFile.getAbsolutePath());
		} catch (IOException e) {
			logger.error("Error creating input file", e);
		}
	}

	private void compileJavaFile(StringBuilder output) throws IOException {
		String compileCommand = "javac code_" + timestamp + ".java";
		executeCommand(compileCommand, output, "Compilation");
	}

	private void executeJavaFile(StringBuilder output) throws IOException {
		String executeCommand = "java code_" + timestamp + " < input_" + timestamp + ".txt";
		output.append("Output:\n");
		executeCommand(executeCommand, output, "Execution");
	}

	private void executeCommand(String command, StringBuilder output, String processType) throws IOException {
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(new File(fileLocation));

		if (IS_WINDOWS) {
			builder.command("cmd.exe", "/c", command);
		} else {
			builder.command("sh", "-c", command);
		}

		Process process;
		try {
			process = builder.start();
		} catch (IOException e) {
			logger.error("Error starting {} process", processType, e);
			return;
		}

		try (BufferedReader standardOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			 BufferedReader errorOutput = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

			String line;
			while ((line = standardOutput.readLine()) != null) {
				logger.info("{} output: {}", processType, line);
				output.append(line).append("\n");
			}

			boolean hasErrors = false;
			while ((line = errorOutput.readLine()) != null) {
				hasErrors = true;
				logger.error("{} error: {}", processType, line);
				output.append(line).append("\n");
			}

			if (!hasErrors && processType.equals("Compilation")) {
				output.append("Successfully Compiled\n");
			}
		}
	}
}