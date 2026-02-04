package org.openelisglobal.sitebranding.service;

/**
 * Enum for logo types in site branding
 */
public enum LogoType {
    HEADER("header"), LOGIN("login"), FAVICON("favicon");

    private final String value;

    LogoType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LogoType fromString(String value) {
        for (LogoType type : LogoType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown logo type: " + value);
    }
}
