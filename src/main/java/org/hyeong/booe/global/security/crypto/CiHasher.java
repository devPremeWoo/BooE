package org.hyeong.booe.global.security.crypto;

import jakarta.annotation.PostConstruct;
import org.hyeong.booe.exception.CryptoOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class CiHasher {

    private static final String ALGORITHM = "HmacSHA256";

    @Value("${booe.crypto.ci.hmac-key}")
    private String base64Key;

    private SecretKeySpec key;

    @PostConstruct
    public void init() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("CI HMAC key is not configured!");
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.key = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public String hash(String ci) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] result = mac.doFinal(ci.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (GeneralSecurityException e) {
            throw new CryptoOperationException(e);
        }
    }
}
