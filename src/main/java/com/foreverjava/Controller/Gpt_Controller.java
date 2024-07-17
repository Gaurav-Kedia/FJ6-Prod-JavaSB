package com.foreverjava.Controller;

import com.foreverjava.Reader.GeminiService;
import com.foreverjava.Reader.XmlReaderService;
import com.foreverjava.Util.Creds;
import com.foreverjava.Util.FileConfigUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class Gpt_Controller {
    @Autowired
    private GeminiService geminiService;

    @Autowired
    private XmlReaderService xmlReaderService;

    private final FileConfigUtil fileConfig;

    @Autowired
    public Gpt_Controller(FileConfigUtil fileConfig) {
        this.fileConfig = fileConfig;
    }

    @GetMapping("/FJ")
    public String getResponse(String prompt) throws IOException {
        String suffix = "Write only the Java code for the following: ";
        Creds creds = xmlReaderService.readCredsFromXml(fileConfig);
        String Credlocation = creds.getAI_API();
        return geminiService.callApi(suffix + prompt, Credlocation);
    }
}