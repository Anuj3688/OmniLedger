package dev.fintech.omniledger.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiiCryptoServiceTest {

    private PiiCryptoService piiCryptoService;

    // 32-byte Base64 key
    private static final String TEST_KEY = "k9sP2vX8qY6wZ3bA1cE5gH7jK9mN1pQ3rT5vX7yZ9aA=";
    // 32-byte Base64 pepper
    private static final String TEST_PEPPER = "pP4kR7vT9wZ1bC3eG5jM7qS9uV1yX3aC5eG7jK9mN1p=";

    @BeforeEach
    void setUp() {
        piiCryptoService = new PiiCryptoService(TEST_KEY, TEST_PEPPER);
    }

    @Test
    @DisplayName("Encrypt and Decrypt roundtrip succeeds with exact plaintext")
    void encryptDecrypt_RoundTrip_Succeeds() {
        String rawPan = "ABCDE1234F";
        String encrypted = piiCryptoService.encrypt(rawPan);

        assertNotNull(encrypted);
        assertNotEquals(rawPan, encrypted);

        String decrypted = piiCryptoService.decrypt(encrypted);
        assertEquals(rawPan, decrypted);
    }

    @Test
    @DisplayName("Encrypting identical plaintext twice produces different ciphertexts due to random IV")
    void encrypt_ProducesUniqueCiphertexts_DueToRandomIv() {
        String pan = "ABCDE1234F";
        String cipher1 = piiCryptoService.encrypt(pan);
        String cipher2 = piiCryptoService.encrypt(pan);

        assertNotEquals(cipher1, cipher2, "AES-GCM must use random IV per encryption");
        assertEquals(piiCryptoService.decrypt(cipher1), piiCryptoService.decrypt(cipher2));
    }

    @Test
    @DisplayName("Tampered ciphertext fails decryption with authentication tag failure")
    void decrypt_TamperedCiphertext_ThrowsException() {
        String encrypted = piiCryptoService.encrypt("ABCDE1234F");
        byte[] bytes = Base64.getDecoder().decode(encrypted);

        // Flip a bit in ciphertext
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThrows(IllegalStateException.class, () -> piiCryptoService.decrypt(tampered));
    }

    @Test
    @DisplayName("Blind index computation is deterministic and case-insensitive")
    void computeBlindIndex_Deterministic() {
        String pan1 = "ABCDE1234F";
        String pan2 = "abcde1234f ";

        String hash1 = piiCryptoService.computeBlindIndex(pan1);
        String hash2 = piiCryptoService.computeBlindIndex(pan2);

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "HMAC-SHA256 hex string should be 64 characters");
        assertEquals(hash1, hash2, "Normalized blind index should match regardless of casing or whitespace");
    }

    @Test
    @DisplayName("Null handling in crypto operations")
    void nullHandling() {
        assertNull(piiCryptoService.encrypt(null));
        assertNull(piiCryptoService.decrypt(null));
        assertNull(piiCryptoService.computeBlindIndex(null));
    }

    @Test
    @DisplayName("PiiMasker masks PAN, Email, and Phone correctly")
    void piiMasker_MasksFieldsCorrectly() {
        assertEquals("XXXXX1234F", PiiMasker.maskPan("ABCDE1234F"));
        assertEquals("r***a@example.com", PiiMasker.maskEmail("rahul.sharma@example.com"));
        assertTrue(PiiMasker.maskPhone("+919876543210").endsWith("3210"));
    }
}
