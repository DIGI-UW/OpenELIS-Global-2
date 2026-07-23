package org.openelisglobal.address.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Locks the inputType normalization contract for the address-hierarchy
 * pipeline. The frontend branches on an exact string ("freetext" vs
 * "dropdown"), so any value that gets to the wire must be in the supported set.
 * This helper is called both write-side (by the handler before persistence) and
 * read-side (by the REST controller before serializing).
 */
public class AddressHierarchyConfigurationHandlerNormalizeInputTypeTest {

    @Test
    public void normalize_supportedDropdown_returnsAsIs() {
        assertEquals("dropdown", AddressHierarchyConfigurationHandler.normalizeInputType("dropdown"));
    }

    @Test
    public void normalize_supportedFreetext_returnsAsIs() {
        assertEquals("freetext", AddressHierarchyConfigurationHandler.normalizeInputType("freetext"));
    }

    @Test
    public void normalize_mixedCase_lowercases() {
        assertEquals("freetext", AddressHierarchyConfigurationHandler.normalizeInputType("FreeText"));
        assertEquals("dropdown", AddressHierarchyConfigurationHandler.normalizeInputType("DROPDOWN"));
    }

    @Test
    public void normalize_leadingTrailingWhitespace_trims() {
        assertEquals("freetext", AddressHierarchyConfigurationHandler.normalizeInputType("  freetext  "));
        assertEquals("dropdown", AddressHierarchyConfigurationHandler.normalizeInputType("\tdropdown\n"));
    }

    @Test
    public void normalize_unknownToken_defaultsToDropdown() {
        // Unknown tokens default to dropdown so the frontend never receives a
        // garbage value it would silently fall through.
        assertEquals("dropdown", AddressHierarchyConfigurationHandler.normalizeInputType("calendar"));
        assertEquals("dropdown", AddressHierarchyConfigurationHandler.normalizeInputType("textarea"));
        assertEquals("dropdown", AddressHierarchyConfigurationHandler.normalizeInputType("xss"));
    }

    @Test
    public void normalize_null_defaultsToDropdown() {
        assertEquals("dropdown", AddressHierarchyConfigurationHandler.normalizeInputType(null));
    }

    @Test
    public void normalize_emptyOrWhitespace_defaultsToDropdown() {
        assertEquals("dropdown", AddressHierarchyConfigurationHandler.normalizeInputType(""));
        assertEquals("dropdown", AddressHierarchyConfigurationHandler.normalizeInputType("   "));
    }
}
