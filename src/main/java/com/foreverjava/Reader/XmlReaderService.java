package com.foreverjava.Reader;

import com.foreverjava.Util.Creds;
import com.foreverjava.Util.FJCryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Service
public class XmlReaderService {

    @Autowired
    private FJCryptoUtil encryptionUtils;

    public Creds readCredsFromXml(String filePath) {
        Creds creds = null;
        String content = null;

        XMLInputFactory factory = XMLInputFactory.newInstance();

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(filePath));

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamReader.START_ELEMENT:
                        if ("AWS".equals(reader.getLocalName())) {
                            creds = new Creds();
                        }
                        break;

                    case XMLStreamReader.CHARACTERS:
                        content = reader.getText().trim();
                        break;

                    case XMLStreamReader.END_ELEMENT:
                        if ("ACCESS_KEY".equals(reader.getLocalName())) {
                            creds.setACCESS_KEY(encryptionUtils.decrypt(content));
                        } else if ("SECRET_KEY".equals(reader.getLocalName())) {
                            creds.setSECRET_KEY(encryptionUtils.decrypt(content));
                        } else if ("PUBLIC_IP".equals(reader.getLocalName())) {
                            creds.setPUBLIC_IP(encryptionUtils.decrypt(content));
                        } else if ("EMAIL".equals(reader.getLocalName())) {
                            creds.setEMAIL(encryptionUtils.decrypt(content));
                        }
                        break;
                }
            }
        } catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }

        return creds;
    }
}
