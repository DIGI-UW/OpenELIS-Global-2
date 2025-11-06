package org.openelisglobal.security;

import java.security.SecureRandom;
import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    private static SecureRandom RANDOM = new SecureRandom();

    private static final String ALPHA_CAPS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*_=+-/";

    private static final String CHARSET = ALPHA_CAPS + ALPHA + NUMERIC + SPECIAL_CHARS;

    private static int PASSWORD_LENGTH = 12;

    private static int WORK = 12;

    public PasswordUtil() {
    }

    // salt-hash password for security
    public static String hashPassword(String password) {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(WORK));
        return hash;
    }

    // check password against salt-hashed password
    public static boolean checkPassword(String password, String hash) {
        boolean match = BCrypt.checkpw(password, hash);
        return match;
    }

    public static String generatePassword() {
        String result = "";
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARSET.length());
            result += CHARSET.charAt(index);
        }
        return result;
    }
}
