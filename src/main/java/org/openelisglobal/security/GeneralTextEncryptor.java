package org.openelisglobal.security;

import org.jasypt.util.text.AES256TextEncryptor;
import org.jasypt.util.text.TextEncryptor;

/**
 * Normal operation uses one installation key. Old-key conversion is an explicit
 * upgrade.
 */
public final class GeneralTextEncryptor implements TextEncryptor {
    public static final String INSTALL_KEY_PREFIX = "OE1:";
    private final TextEncryptor current;

    private GeneralTextEncryptor(String key) {
        AES256TextEncryptor encryptor = new AES256TextEncryptor();
        encryptor.setPassword(key);
        current = encryptor;
    }

    /**
     * Property-based installations supply their key through the existing property.
     */
    public static GeneralTextEncryptor of(String installKey, String configuredKey) {
        String key = installKey == null || installKey.isBlank() ? configuredKey : installKey;
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("An installation encryption key is required");
        }
        return new GeneralTextEncryptor(key);
    }

    @Override
    public String encrypt(String message) {
        return message == null ? null : INSTALL_KEY_PREFIX + current.encrypt(message);
    }

    @Override
    public String decrypt(String encryptedMessage) {
        if (encryptedMessage == null) {
            return null;
        }
        // Unmarked values are readable only with this same key, never a fallback key.
        return current.decrypt(encryptedMessage.startsWith(INSTALL_KEY_PREFIX)
                ? encryptedMessage.substring(INSTALL_KEY_PREFIX.length())
                : encryptedMessage);
    }
}
