package com.foreverjava.Writer;

import com.foreverjava.Util.FileConfigUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;



public class FileWriterClass {
	
	private static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
	private String code, input, timestamp, command, command2;
	private Process process, process2;
	StringBuilder output = new StringBuilder("");
	java.util.Date date = new java.util.Date();
	private String FileLocation;

	public FileWriterClass(String val, String in, String ts, FileConfigUtil fc) {
		this.code=val;
		this.input=in;
		this.timestamp=ts;
		FileLocation = fc.getFileLocation();
	}
	
	public StringBuilder write() throws IOException {
		make_java_file();
		make_input_file();
		compiler();
		runtime();
        return output;
	}
	
	public void make_java_file() {
		try {
            File newTextFile = new File(FileLocation + "code_" + timestamp + ".java");
            FileWriter fw = new FileWriter(newTextFile);
            fw.write(code);
            fw.close();
	    	System.out.println(date + " :successfully create code file");
        } catch (IOException iox) {
            iox.printStackTrace();
        }
	}
	
	public void make_input_file() {
		try {
            File newTextFile = new File(FileLocation + "input_" + timestamp + ".txt");
            FileWriter fw = new FileWriter(newTextFile);
            fw.write(input);
            fw.close();
	    	System.out.println(date + " :successfully create input file");
        } catch (IOException iox) {
            iox.printStackTrace();
        }
	}
	
	public void compiler() throws IOException {
	File location = new File(FileLocation);
	command = "javac code_" + timestamp + ".java";
	ProcessBuilder builder = new ProcessBuilder();
        builder.directory(location);

        if(isWindows) {
			builder.command("cmd.exe", "/c", command);
        }else {
			builder.command("sh", "-c", command);
        }
	process = null;
	try
	{process = builder.start();}
	catch (IOException e)
	{e.printStackTrace();}
		
	final BufferedReader is = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = is.readLine()) != null) {
        	System.out.println("c : " + line);
        	output.append(line);
			output.append("\n");
		}
        final BufferedReader is2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        boolean f = false;
        while ((line = is2.readLine()) != null) {
        	f = true;
        	System.out.println(date + line);
        	output.append(line);
			output.append("\n");
        }
        
        if(!f) {
        	output.append("Successfully Compiled");
			output.append("\n");
        }
	}
	
	public void runtime() throws IOException {
		File location = new File(FileLocation);
		command2 = "java code_" + timestamp + ".java" + " < input_" + timestamp + ".txt";
		
		ProcessBuilder builder = new ProcessBuilder();
        	builder.directory(location);

        	if(isWindows) {
				builder.command("cmd.exe", "/c", command2);
        	}else {
				builder.command("sh", "-c", command2);
        	}
		process2 = null;
		try
		{process2 = builder.start();}
		catch (IOException e)
		{e.printStackTrace();}
	
		final BufferedReader is3 = new BufferedReader(new InputStreamReader(process2.getInputStream()));
		String line2;
		output.append("Output : ");
		output.append("\n");
		while ((line2 = is3.readLine()) != null) {
			System.out.println(date + " :r : " + line2);
			output.append(line2);
			output.append("\n");
		}
		final BufferedReader is4 = new BufferedReader(new InputStreamReader(process2.getErrorStream()));
		while ((line2 = is4.readLine()) != null) {
			System.out.println(date + line2);
			output.append(line2);
			output.append("\n");
		}
	}
}