package com.foreverjava.Util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class FileConfigUtil {

    @Value("${File.Location.Local}")
    String FileLocation;

    public String getFileLocation() {
        return FileLocation;
    }

    public static List<String> listJavaFiles(String directoryPath) {
        List<String> javaFiles = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".java"));
            if (files != null) {
                for (File file : files) {
                    javaFiles.add(file.getName());
                }
            }
        }
        return javaFiles;
    }

    public static String readFileContents(String directoryPath, String fileName) throws IOException {
        File file = new File(directoryPath, fileName);
        if (file.exists() && file.isFile()) {
            return new String(Files.readAllBytes(Paths.get(file.getPath())));
        } else {
            throw new IOException("File not found: " + fileName);
        }
    }
}