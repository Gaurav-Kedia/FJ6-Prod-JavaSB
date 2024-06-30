package com.foreverjava.Reader;

import com.foreverjava.Util.FileConfigUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class JavaReaderService {

    @Autowired
    FileConfigUtil fileConfigUtil;
    public List<String> listJavaFiles() {
        String directoryPath = fileConfigUtil.getFileLocation();
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

    public String readFileContents(String fileName) throws IOException {
        String directoryPath = fileConfigUtil.getFileLocation();
        File file = new File(directoryPath, fileName);
        if (file.exists() && file.isFile()) {
            return new String(Files.readAllBytes(Paths.get(file.getPath())));
        } else {
            throw new IOException("File not found: " + fileName);
        }
    }
}
