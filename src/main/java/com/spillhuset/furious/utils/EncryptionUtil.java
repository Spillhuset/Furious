package com.spillhuset.furious.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for encrypting and decrypting sensitive data.
 * This class provides methods to securely encrypt and decrypt strings
 * such as passwords stored in configuration files.
 */
public class EncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    private static final String KEY_FILE_NAME = "encryption.key";

    private final Logger logger;
    private final File dataFolder;
    private SecretKey secretKey;

    /**
     * Creates a new EncryptionUtil instance.
     *
     * @param logger The logger to use for logging errors
     * @param dataFolder The plugin's data folder where the encryption key will be stored
     */
    public EncryptionUtil(Logger logger, File dataFolder) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        initializeKey();
    }

    /**
     * Initializes the encryption key. If a key file exists, it will be loaded.
     * Otherwise, a new key will be generated and saved.
     */
    private void initializeKey() {
        File keyFile = new File(dataFolder, KEY_FILE_NAME);

        if (keyFile.exists()) {
            // Load existing key
            try (FileInputStream fis = new FileInputStream(keyFile)) {
                byte[] keyBytes = new byte[(int) keyFile.length()];
                fis.read(keyBytes);
                secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
                logger.info("Loaded existing encryption key");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load encryption key", e);
                generateAndSaveKey(keyFile);
            }
        } else {
            // Generate and save new key
            generateAndSaveKey(keyFile);
        }
    }

    /**
     * Generates a new encryption key and saves it to the specified file.
     *
     * @param keyFile The file where the key will be saved
     */
    private void generateAndSaveKey(File keyFile) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            SecureRandom secureRandom = new SecureRandom();
            keyGenerator.init(KEY_SIZE, secureRandom);
            secretKey = keyGenerator.generateKey();

            // Save the key
            try (FileOutputStream fos = new FileOutputStream(keyFile)) {
                fos.write(secretKey.getEncoded());
            }

            logger.info("Generated and saved new encryption key");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to generate or save encryption key", e);
        }
    }

    /**
     * Encrypts a string.
     *
     * @param plainText The string to encrypt
     * @return The encrypted string, or null if encryption fails
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Encryption failed", e);
            return null;
        }
    }

    /**
     * Decrypts a string.
     *
     * @param encryptedText The string to decrypt
     * @return The decrypted string, or null if decryption fails
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Decryption failed", e);
            return null;
        }
    }

    /**
     * Checks if a string is encrypted.
     * This is a best-effort check and may not be 100% accurate.
     *
     * @param text The string to check
     * @return true if the string appears to be encrypted, false otherwise
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        try {
            // Try to decode as Base64
            byte[] decoded = Base64.getDecoder().decode(text);
            // If we can decode it and it's not just ASCII text, it's likely encrypted
            return decoded.length > 0;
        } catch (IllegalArgumentException e) {
            // Not valid Base64, so not encrypted
            return false;
        }
    }
}