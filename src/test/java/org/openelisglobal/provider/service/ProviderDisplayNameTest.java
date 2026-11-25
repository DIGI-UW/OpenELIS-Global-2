package org.openelisglobal.provider.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * OGC-1223 FR-11: one helper renders a provider's name wherever a screen has
 * room for a single string, and a missing part never leaves a stray space or
 * separator behind.
 */
public class ProviderDisplayNameTest {

    @Test
    public void titledName_readsTitleFirstNameLastName() {
        assertEquals("Dr John Kila", ProviderDisplayName.titled("Dr", "John", "Kila"));
    }

    @Test
    public void noTitle_leavesNoLeadingSpace() {
        assertEquals("John Kila", ProviderDisplayName.titled(null, "John", "Kila"));
        assertEquals("John Kila", ProviderDisplayName.titled("", "John", "Kila"));
        assertEquals("John Kila", ProviderDisplayName.titled("   ", "John", "Kila"));
    }

    @Test
    public void noFirstName_leavesNoDoubleSpace() {
        assertEquals("Dr Kila", ProviderDisplayName.titled("Dr", null, "Kila"));
    }

    @Test
    public void onlyATitle_rendersJustTheTitle() {
        assertEquals("Dr", ProviderDisplayName.titled("Dr", null, null));
    }

    @Test
    public void nothingAtAll_isEmptyRatherThanSpaces() {
        assertEquals("", ProviderDisplayName.titled(null, null, null));
    }

    @Test
    public void surroundingWhitespaceIsTrimmedNotPreserved() {
        assertEquals("Dr John Kila", ProviderDisplayName.titled(" Dr ", " John ", " Kila "));
    }

    @Test
    public void familyFirst_putsTheTitleWithTheGivenName() {
        assertEquals("Kila, Dr John", ProviderDisplayName.titledFamilyFirst("Dr", "John", "Kila"));
    }

    @Test
    public void familyFirst_withoutATitle_readsAsBefore() {
        assertEquals("Kila, John", ProviderDisplayName.titledFamilyFirst(null, "John", "Kila"));
    }

    @Test
    public void familyFirst_withoutAGivenName_dropsTheComma() {
        assertEquals("Kila", ProviderDisplayName.titledFamilyFirst(null, null, "Kila"));
    }

    @Test
    public void familyFirst_withoutAFamilyName_stillReads() {
        assertEquals("Dr John", ProviderDisplayName.titledFamilyFirst("Dr", "John", null));
    }
}
