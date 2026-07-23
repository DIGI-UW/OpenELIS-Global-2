/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.common.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.springframework.stereotype.Service;

/** */
@Service
public class PhoneNumberService {
    private static final Object lock = new Object();
    private static String rawFormat = null;
    private static String rawInternationalValidation = null;
    private static List<String> rawConfiguredCountryCodes = new ArrayList<>();
    private static String regex = null;
    private static final String FORMAT_REGEX = "[a-zA-Z0-9\\+\\(\\)\\s\\-/\\|]+";
    private static final String NON_MATCHING_REGEX = "a^";
    private static final Pattern INTERNATIONAL_PRESENTATION_REGEX = Pattern.compile("^\\+[1-9][0-9\\s-]*$");
    private static final Pattern INTERNATIONAL_DIGITS_REGEX = Pattern.compile("^[1-9]\\d{7,14}$");

    public static String getPhoneFormat() {
        return ConfigurationProperties.getInstance().getPropertyValue(ConfigurationProperties.Property.PHONE_FORMAT);
    }

    public static String getInternationalPhoneValidation() {
        return ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.PHONE_INTERNATIONAL_VALIDATION);
    }

    /**
     * Validates a phone number according to the given format but allows for an
     * arbitrary extension
     *
     * @param number to be validated
     * @return true if valid false otherwise
     */
    public boolean validatePhoneNumber(String number) {
        if (ConfigurationProperties.getInstance()
                .isPropertyValueEqual(ConfigurationProperties.Property.VALIDATE_PHONE_FORMAT, "false")
                || GenericValidator.isBlankOrNull(number)) {
            return true;
        }

        String configuredFormat = blankToEmpty(getPhoneFormat());
        String configuredInternationalValidation = blankToEmpty(getInternationalPhoneValidation());

        if (!configuredFormat.equals(rawFormat)
                || !configuredInternationalValidation.equals(rawInternationalValidation)) {
            synchronized (lock) {
                rawFormat = configuredFormat;
                rawInternationalValidation = configuredInternationalValidation;
                buildRegEx();
            }
        }

        String trimmedNumber = number.trim();
        return trimmedNumber.matches(regex) || isInternationalPhoneConfigured()
                && validateInternationalPhoneNumber(trimmedNumber) && !usesConfiguredCountryCode(trimmedNumber);
    }

    private void buildRegEx() {
        List<String> alternatives = new ArrayList<>();
        for (String format : splitAlternatives(rawFormat)) {
            if (!GenericValidator.isBlankOrNull(format)) {
                alternatives.add(buildTemplateRegex(format.trim()));
            }
        }
        rawConfiguredCountryCodes = extractConfiguredCountryCodes(rawFormat);
        regex = alternatives.isEmpty() ? NON_MATCHING_REGEX : "^(?:" + String.join("|", alternatives) + ")(\\s+.*)?$";
    }

    private static List<String> splitAlternatives(String format) {
        List<String> alternatives = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int groupDepth = 0;
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '(') {
                groupDepth++;
            } else if (c == ')' && groupDepth > 0) {
                groupDepth--;
            }

            if (c == '|' && groupDepth == 0) {
                alternatives.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        alternatives.add(current.toString());
        return alternatives;
    }

    private String buildTemplateRegex(String format) {
        StringBuilder builder = new StringBuilder();
        boolean inFlexibleSeparator = false;
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '(') {
                int groupEnd = format.indexOf(')', i);
                if (groupEnd > i) {
                    builder.append("(?:");
                    String[] groupAlternatives = format.substring(i + 1, groupEnd).split("[\\|/]");
                    for (int j = 0; j < groupAlternatives.length; j++) {
                        if (j > 0) {
                            builder.append("|");
                        }
                        appendTokenRegex(builder, groupAlternatives[j]);
                    }
                    builder.append(")");
                    i = groupEnd;
                    inFlexibleSeparator = false;
                } else {
                    builder.append(Pattern.quote(String.valueOf(c)));
                    inFlexibleSeparator = false;
                }
            } else if (Character.isLetterOrDigit(c)) {
                int tokenEnd = i;
                while (tokenEnd + 1 < format.length()) {
                    char next = format.charAt(tokenEnd + 1);
                    if (!Character.isLetterOrDigit(next) && next != '/') {
                        break;
                    }
                    tokenEnd++;
                }
                appendTokenRegex(builder, format.substring(i, tokenEnd + 1));
                i = tokenEnd;
                inFlexibleSeparator = false;
            } else if (Character.isWhitespace(c) || c == '-') {
                if (!inFlexibleSeparator) {
                    builder.append("[\\s-]*");
                    inFlexibleSeparator = true;
                }
            } else {
                builder.append(Pattern.quote(String.valueOf(c)));
                inFlexibleSeparator = false;
            }
        }
        return builder.toString();
    }

    private static void appendTokenRegex(StringBuilder builder, String token) {
        String[] alternatives = token.split("/");
        if (alternatives.length > 1) {
            builder.append("(?:");
        }
        for (int i = 0; i < alternatives.length; i++) {
            if (i > 0) {
                builder.append("|");
            }
            appendLiteralTokenRegex(builder, alternatives[i]);
        }
        if (alternatives.length > 1) {
            builder.append(")");
        }
    }

    private static void appendLiteralTokenRegex(StringBuilder builder, String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isLetter(c)) {
                builder.append("\\d");
            } else {
                builder.append(Pattern.quote(String.valueOf(c)));
            }
        }
    }

    private static boolean isInternationalPhoneConfigured() {
        return "E164".equalsIgnoreCase(blankToEmpty(rawInternationalValidation));
    }

    private static boolean validateInternationalPhoneNumber(String number) {
        if (!INTERNATIONAL_PRESENTATION_REGEX.matcher(number).matches()) {
            return false;
        }
        String digits = number.substring(1).replaceAll("[\\s-]", "");
        return INTERNATIONAL_DIGITS_REGEX.matcher(digits).matches();
    }

    private static boolean usesConfiguredCountryCode(String number) {
        String digits = number.substring(1).replaceAll("[\\s-]", "");
        for (String countryCode : rawConfiguredCountryCodes) {
            if (digits.startsWith(countryCode)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> extractConfiguredCountryCodes(String formatList) {
        List<String> countryCodes = new ArrayList<>();
        for (String format : splitAlternatives(formatList)) {
            String trimmedFormat = format.trim();
            if (trimmedFormat.startsWith("+")) {
                StringBuilder countryCode = new StringBuilder();
                for (int i = 1; i < trimmedFormat.length() && Character.isDigit(trimmedFormat.charAt(i)); i++) {
                    countryCode.append(trimmedFormat.charAt(i));
                }
                if (countryCode.length() > 0 && !countryCodes.contains(countryCode.toString())) {
                    countryCodes.add(countryCode.toString());
                }
            }
        }
        return countryCodes;
    }

    private static String blankToEmpty(String value) {
        return GenericValidator.isBlankOrNull(value) ? "" : value;
    }

    public static boolean validatePhoneFormat(String format) {
        return format.matches(FORMAT_REGEX);
    }

    public static boolean validateInternationalPhoneValidation(String value) {
        String normalized = blankToEmpty(value);
        return normalized.isEmpty() || "NONE".equalsIgnoreCase(normalized) || "E164".equalsIgnoreCase(normalized);
    }
}
