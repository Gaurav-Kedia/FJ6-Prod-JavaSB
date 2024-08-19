package com.foreverjava.Util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class XmlQueryLoader {

    private Map<String, String> queryMap = new HashMap<>();

    public XmlQueryLoader() {
        try {
            // Load the XML file from the resources folder
            ClassPathResource resource = new ClassPathResource("Statement.xml");
            InputStream inputStream = resource.getInputStream();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            NodeList queryList = doc.getElementsByTagName("query");
            for (int i = 0; i < queryList.getLength(); i++) {
                Element queryElement = (Element) queryList.item(i);
                String id = queryElement.getAttribute("id");
                String sql = queryElement.getTextContent().trim();
                queryMap.put(id, sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getQuery(String id) {
        return queryMap.get(id);
    }
}
