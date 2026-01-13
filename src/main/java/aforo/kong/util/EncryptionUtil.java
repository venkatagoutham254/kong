package aforo.kong.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Component
public class EncryptionUtil {

    private static final String V1_PREFIX = "v1:";
    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String AES_ECB = "AES/ECB/PKCS5Padding";

    private static final int IV_LEN_BYTES = 12;
    private static final int TAG_LEN_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();

    private final SecretKey key;

    public EncryptionUtil(@Value("${encryption.secret.key:}") String base64Key) {
        Objects.requireNonNull(base64Key, "encryption.secret.key must be configured");
        if (base64Key.isEmpty()) {
            throw new IllegalArgumentException("encryption.secret.key cannot be empty. Generate with: openssl rand -base64 32");
        }
        
        byte[] raw = Base64.getDecoder().decode(base64Key.trim());
        if (!(raw.length == 16 || raw.length == 24 || raw.length == 32)) {
            throw new IllegalArgumentException("Invalid AES key length: " + raw.length + " bytes. Use 16/24/32 bytes.");
        }
        this.key = new SecretKeySpec(raw, AES);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LEN_BYTES];
            RNG.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LEN_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] ctAndTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return V1_PREFIX
                    + Base64.getEncoder().encodeToString(iv)
                    + "."
                    + Base64.getEncoder().encodeToString(ctAndTag);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }

        if (stored.startsWith(V1_PREFIX)) {
            return decryptV1(stored);
        }

        return decryptLegacyEcb(stored);
    }

    private String decryptV1(String stored) {
        try {
            String payload = stored.substring(V1_PREFIX.length());
            String[] parts = payload.split("\\.", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted payload format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ctAndTag = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LEN_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] pt = cipher.doFinal(ctAndTag);
            return new String(pt, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException | RuntimeException e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private String decryptLegacyEcb(String stored) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(stored);

            Cipher cipher = Cipher.getInstance(AES_ECB);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] pt = cipher.doFinal(cipherBytes);
            return new String(pt, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException | RuntimeException e) {
            throw new IllegalStateException("Legacy decryption failed", e);
        }
    }
}
