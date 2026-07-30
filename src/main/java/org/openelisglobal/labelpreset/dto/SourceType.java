package org.openelisglobal.labelpreset.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Source tag for an aggregated Order Entry label cell (FRS §4.4.1 /
 * contracts/openapi.yaml §8.1 {@code SourceType}). Indicates whether the cell's
 * default quantity was driven by a test→preset link ({@link #TEST}) or fell
 * back to the preset's system default ({@link #PRESET_DEFAULT}).
 *
 * <p>
 * {@link JsonValue} pins the serialized form to the snake_case wire value
 * ({@code "test"} / {@code "preset_default"}) regardless of the Java constant
 * name.
 */
public enum SourceType {

    TEST("test"),

    PRESET_DEFAULT("preset_default");

    private final String wireValue;

    SourceType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
