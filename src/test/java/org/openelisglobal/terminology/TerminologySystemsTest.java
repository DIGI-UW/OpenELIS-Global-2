package org.openelisglobal.terminology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TerminologySystemsTest {

    @Test
    public void fhirGetsTheCanonicalUriOrAUriTheCatalogOwnerChose() {
        assertEquals("http://loinc.org", TerminologySystems.urlOf("loinc"));
        assertEquals("https://lab.example.org/concepts",
                TerminologySystems.urlOf(" https://lab.example.org/concepts "));
        assertEquals("urn:oid:1.2.3", TerminologySystems.urlOf("urn:oid:1.2.3"));
        assertNull(TerminologySystems.urlOf("CPHL"));
    }

    @Test
    public void aSourceWithoutACanonicalUriIsItsOwnSystem() {
        assertEquals("http://loinc.org", TerminologySystems.systemOf("LOINC"));
        assertEquals("CPHL", TerminologySystems.systemOf(" CPHL "));
        assertNull(TerminologySystems.systemOf(" "));
    }

    @Test
    public void aKnownSourceIsStoredInItsOwnSpellingAndAnyOtherAsWritten() {
        assertEquals("LOINC", TerminologySystems.sourceOf(" loinc "));
        assertEquals("CPHL", TerminologySystems.sourceOf("CPHL"));
        assertEquals("cphl-local", TerminologySystems.sourceOf(" cphl-local "));
    }

    @Test
    public void canonicalUrisUseTheirStorageAliasesBeforeTheLengthLimit() {
        for (String source : new String[] { "LOINC", "SNOMED", "CIEL", "OCL" }) {
            assertEquals(source, TerminologySystems.sourceOf(TerminologySystems.urlOf(source)));
        }
        assertEquals("LOINC", TerminologySystems.sourceOf(" http://loinc.org "));
    }

    @Test
    public void aKnownSourceIsRecognizedWhateverTheServersLocale() {
        java.util.Locale before = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            assertEquals("LOINC", TerminologySystems.sourceOf("loinc"));
            assertEquals("https://openconceptlab.org/orgs/CIEL/sources/CIEL", TerminologySystems.urlOf("ciel"));
        } finally {
            java.util.Locale.setDefault(before);
        }
    }

    @Test
    public void aSourceMustBeNonBlankAndFitItsColumn() {
        assertNull(TerminologySystems.sourceOf(" "));
        assertNull(TerminologySystems.sourceOf(null));
        assertEquals("A".repeat(20), TerminologySystems.sourceOf("A".repeat(20)));
        assertNull(TerminologySystems.sourceOf("A".repeat(21)));
    }
}
