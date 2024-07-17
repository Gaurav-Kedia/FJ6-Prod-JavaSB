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

    @Value("${Creds.Location.Local}")
    String CredLocation;

    public String getFileLocation() {
        return FileLocation;
    }

    public String getCredLocation() {
        return CredLocation;
    }
}