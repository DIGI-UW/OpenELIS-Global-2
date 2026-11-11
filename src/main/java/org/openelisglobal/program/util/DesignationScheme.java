package org.openelisglobal.program.util;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

/**
 * The convention a laboratory names and barcodes its cassettes, blocks and
 * slides by (FR-9.3).
 */
public final class DesignationScheme {

    /** How the parts a case is divided into are named. */
    public enum PartScheme {
        /** A, B, C. */
        ALPHA,
        /** 1, 2, 3. */
        NUMERIC
    }

    private static final PartScheme DEFAULT_PART_SCHEME = PartScheme.ALPHA;
    private static final String DEFAULT_BLOCK_FORMAT = "{part}{n}";
    private static final String DEFAULT_SLIDE_FORMAT = "{n}";
    private static final String DEFAULT_SEPARATOR = ".";

    private final PartScheme partScheme;
    private final String blockFormat;
    private final String slideFormat;
    private final String separator;

    /** Any argument left null or blank takes the shipped default in its place. */
    public DesignationScheme(PartScheme partScheme, String blockFormat, String slideFormat, String separator) {
        this.partScheme = partScheme == null ? DEFAULT_PART_SCHEME : partScheme;
        this.blockFormat = orDefault(blockFormat, DEFAULT_BLOCK_FORMAT);
        this.slideFormat = orDefault(slideFormat, DEFAULT_SLIDE_FORMAT);
        this.separator = orDefault(separator, DEFAULT_SEPARATOR);
    }

    /** The scheme this deployment has configured. */
    public static DesignationScheme fromConfiguration() {
        ConfigurationProperties configuration = ConfigurationProperties.getInstance();
        return new DesignationScheme(
                parsePartScheme(configuration.getPropertyValue(Property.PATHOLOGY_IDENTIFIER_PART_SCHEME)),
                numberedOrDefault(configuration.getPropertyValue(Property.PATHOLOGY_IDENTIFIER_BLOCK_FORMAT),
                        DEFAULT_BLOCK_FORMAT),
                numberedOrDefault(configuration.getPropertyValue(Property.PATHOLOGY_IDENTIFIER_SLIDE_FORMAT),
                        DEFAULT_SLIDE_FORMAT),
                configuration.getPropertyValue(Property.PATHOLOGY_IDENTIFIER_SEPARATOR));
    }

    /**
     * A configured format that could never number anything falls back to the
     * default, so a typo on the admin page does not stop every case save.
     */
    private static String numberedOrDefault(String format, String fallback) {
        return format != null && format.contains("{n}") ? format : fallback;
    }

    /** The scheme a deployment gets before it configures one of its own. */
    public static DesignationScheme defaults() {
        return new DesignationScheme(DEFAULT_PART_SCHEME, DEFAULT_BLOCK_FORMAT, DEFAULT_SLIDE_FORMAT,
                DEFAULT_SEPARATOR);
    }

    /** How the parts a case is divided into are named. */
    public PartScheme getPartScheme() {
        return partScheme;
    }

    /** The block designation pattern, in {@code {part}} and {@code {n}}. */
    public String getBlockFormat() {
        return blockFormat;
    }

    /** The slide designation pattern, in {@code {n}}. */
    public String getSlideFormat() {
        return slideFormat;
    }

    /** What separates the segments of a barcode. */
    public String getSeparator() {
        return separator;
    }

    /**
     * A part scheme a deployment has not heard of leaves its cases unnamed, so an
     * unrecognized value falls back to the shipped scheme rather than failing.
     */
    private static PartScheme parsePartScheme(String value) {
        if (value != null) {
            for (PartScheme candidate : PartScheme.values()) {
                if (candidate.name().equalsIgnoreCase(value.trim())) {
                    return candidate;
                }
            }
        }
        return DEFAULT_PART_SCHEME;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
