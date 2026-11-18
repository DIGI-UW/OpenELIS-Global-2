package org.openelisglobal.payment.gateway;

import java.util.regex.Pattern;

/**
 * The only patient details a payment link may carry: a phone number and an
 * email address, so the gateway can send the link to the patient. Never the
 * name or anything about the tests. Values that do not look valid are dropped
 * (null) rather than sent.
 */
public record CustomerContact(String phone, String email) {

    public static final CustomerContact NONE = new CustomerContact(null, null);

    /** Optional leading "+", then 8 to 15 digits (E.164 allows at most 15). */
    private static final Pattern PHONE = Pattern.compile("\\+?\\d{8,15}");

    /** Something@domain.tld with no spaces; deliberately loose. */
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private static final int MAX_EMAIL_LENGTH = 254;

    public static CustomerContact of(String rawPhone, String rawEmail) {
        String phone = normalizePhone(rawPhone);
        String email = normalizeEmail(rawEmail);
        return phone == null && email == null ? NONE : new CustomerContact(phone, email);
    }

    public boolean isEmpty() {
        return phone == null && email == null;
    }

    private static String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[\\s().-]", "");
        return PHONE.matcher(cleaned).matches() ? cleaned : null;
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.length() <= MAX_EMAIL_LENGTH && EMAIL.matcher(trimmed).matches() ? trimmed : null;
    }
}
