package com.foreverjava.Util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileConfig {

    @Value("${File.Location.Local}")
    String FileLocation;

    public String getFileLocation() {
        return FileLocation;
    }
}