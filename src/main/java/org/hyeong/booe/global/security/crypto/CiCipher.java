package org.hyeong.booe.global.security.crypto;

import jakarta.annotation.PostConstruct;
import org.hyeong.booe.exception.CryptoOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class CiCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();

    @Value("${booe.crypto.ci.aes-key}")
    private String base64Key;

    private SecretKeySpec key;

    @PostConstruct
    public void init() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("CI AES key is not configured!");
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] encrypt(String ci) {
        byte[] iv = generateIv();
        byte[] ciphertext = doCipher(Cipher.ENCRYPT_MODE, iv, ci.getBytes(StandardCharsets.UTF_8));
        return concat(iv, ciphertext);
    }

    public String decrypt(byte[] data) {
        byte[] iv = Arrays.copyOfRange(data, 0, IV_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(data, IV_BYTES, data.length);
        byte[] plain = doCipher(Cipher.DECRYPT_MODE, iv, ciphertext);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private byte[] doCipher(int mode, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException e) {
            throw new CryptoOperationException(e);
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        return iv;
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
