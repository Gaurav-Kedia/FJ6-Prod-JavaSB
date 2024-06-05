package com.foreverjava.Util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class FJCryptoUtil {

    @Value("${encryption.key}")
    private String key;

    private static String ALGORITHM = "AES";
    private static byte[] KEY;

    @PostConstruct
    public void init() {
        KEY = key.getBytes(); // Initialize KEY after the value is injected
    }

    public String decrypt(String value) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(KEY, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decodedValue = Base64.getDecoder().decode(value);
            byte[] decryptedValue = cipher.doFinal(decodedValue);
            return new String(decryptedValue);
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting", e);
        }
    }
}