package org.openelisglobal.program.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.program.util.DesignationScheme.PartScheme;

/**
 * A block or a slide keeps its designation for as long as it is retained, which
 * is years, so a designation handed out twice would leave two objects answering
 * to one printed label. Plain JUnit: no Spring, no database, so the naming
 * rules, the numbering that never reuses and the barcode a label carries
 * (FR-9.2, FR-9.3, FR-9.4) are pinned deterministically.
 */
public class PathologyDesignationsTest {

    private static final DesignationScheme DEFAULTS = DesignationScheme.defaults();

    private static final String ACCESSION = "24TST000010";

    @Test
    public void nextBlockDesignation_onAnEmptyCase_startsAtOne() {
        assertEquals("the first cassette cut from part A is A1, because the bench counts from one", "A1",
                PathologyDesignations.nextBlockDesignation(List.of(), "A", DEFAULTS));
    }

    @Test
    public void nextBlockDesignation_afterA1AndA2_givesA3() {
        assertEquals("the next cassette takes the number after the highest the part has used", "A3",
                PathologyDesignations.nextBlockDesignation(List.of("A1", "A2"), "A", DEFAULTS));
    }

    @Test
    public void nextBlockDesignation_neverReusesADesignationStillOnTheCase() {
        // A2's block has been deactivated, and the caller passes deactivated
        // designations in exactly so that they keep their number: the block is
        // retained for years and the label printed for it must still resolve to it.
        List<String> deactivatedA2Included = List.of("A1", "A2");

        assertEquals("a deactivated block keeps its designation, so the next cassette is A3 and never A2", "A3",
                PathologyDesignations.nextBlockDesignation(deactivatedA2Included, "A", DEFAULTS));
    }

    @Test
    public void nextBlockDesignation_fillsAGapOnlyWhenItWasNeverUsed() {
        // Nothing this code writes leaves a gap; one arrives only on data migrated
        // from the years when blocks carried a typed-in number.
        assertEquals("a number no object on the case has ever held is free to use", "A2",
                PathologyDesignations.nextBlockDesignation(List.of("A1", "A3"), "A", DEFAULTS));
    }

    @Test
    public void nextBlockDesignation_forASecondPart_countsFromOneAgain() {
        assertEquals("a block is numbered within its own part, so the first cassette of part B is B1", "B1",
                PathologyDesignations.nextBlockDesignation(List.of("A1", "A2"), "B", DEFAULTS));
    }

    @Test
    public void nextBlockDesignation_underANumericScheme_givesPlainNumbers() {
        DesignationScheme numeric = new DesignationScheme(PartScheme.NUMERIC, "{part}-{n}", "{n}", ".");

        assertEquals("a numbered part names its first block after itself", "1-1",
                PathologyDesignations.nextBlockDesignation(List.of(), "1", numeric));
        assertEquals("and counts on from there", "1-2",
                PathologyDesignations.nextBlockDesignation(List.of("1-1"), "1", numeric));
        assertEquals("the first part of a numbered case is 1, not A", "1", PathologyDesignations.firstPart(numeric));
        assertEquals("the first part of a lettered case is A", "A", PathologyDesignations.firstPart(DEFAULTS));
    }

    @Test
    public void nextBlockDesignation_ignoresLegacyNumericDesignations() {
        // A migrated case carries the numbers its blocks were typed in under, which
        // this scheme would never produce; they collide with nothing it does produce.
        List<String> migratedCase = List.of("1", "2", "1-2");

        assertEquals("designations from another scheme do not hold back the numbering of this one", "A1",
                PathologyDesignations.nextBlockDesignation(migratedCase, "A", DEFAULTS));
    }

    @Test
    public void nextSlideDesignation_isIndependentPerBlock() {
        assertEquals("a slide is numbered within its own block", "3",
                PathologyDesignations.nextSlideDesignation(List.of("1", "2"), DEFAULTS));
        assertEquals("a block that has been cut from before starts its own slides at one", "1",
                PathologyDesignations.nextSlideDesignation(List.of(), DEFAULTS));
    }

    @Test
    public void blockBarcode_joinsAccessionAndDesignation() {
        assertEquals("a block's label scans to its case and its designation", "24TST000010.A1",
                PathologyDesignations.blockBarcode(ACCESSION, "A1", DEFAULTS));

        DesignationScheme dashed = new DesignationScheme(PartScheme.ALPHA, "{part}{n}", "{n}", "-");

        assertEquals("the deployment's own separator is what joins the segments", "24TST000010-A1",
                PathologyDesignations.blockBarcode(ACCESSION, "A1", dashed));
    }

    @Test
    public void slideBarcode_joinsAccessionBlockAndSlide() {
        assertEquals("a slide's label scans to its case, its block and itself", "24TST000010.A1.1",
                PathologyDesignations.slideBarcode(ACCESSION, "A1", "1", DEFAULTS));
        assertEquals("a slide whose block is not known still scans to its case and itself", "24TST000010.1",
                PathologyDesignations.slideBarcode(ACCESSION, null, "1", DEFAULTS));
    }

    @Test
    public void barcode_withoutAnAccession_isTheDesignationAlone() {
        assertEquals("a barcode never opens with a bare separator", "A1",
                PathologyDesignations.blockBarcode(null, "A1", DEFAULTS));
        assertEquals("a blank accession is as absent as a missing one", "A1.1",
                PathologyDesignations.slideBarcode("   ", "A1", "1", DEFAULTS));
    }

    @Test
    public void nextDesignation_isDeterministicAndNeverACollision() {
        List<Collection<String>> cases = List.of(List.of(), List.of("A1"), List.of("A1", "A2", "A3"),
                Arrays.asList("A1", null, "  ", "A2"), Arrays.asList(" A1 ", "A3"), List.of("B1", "B2"));

        for (Collection<String> existing : cases) {
            String first = PathologyDesignations.nextBlockDesignation(existing, "A", DEFAULTS);
            String second = PathologyDesignations.nextBlockDesignation(existing, "A", DEFAULTS);

            assertEquals("the same case must always be answered with the same designation, or two benches"
                    + " asking at once would disagree about what to write", first, second);
            assertFalse("the designation handed out for " + existing + " must be one no object already holds",
                    trimmed(existing).contains(first));
        }
    }

    @Test
    public void nextBlockDesignation_rejectsAFormatWithoutTheNumberPlaceholder() {
        DesignationScheme unnumbered = new DesignationScheme(PartScheme.ALPHA, "{part}", "{n}", ".");

        IllegalArgumentException thrown = assertThrows(
                "a format that never varies would name every block on the case the same",
                IllegalArgumentException.class,
                () -> PathologyDesignations.nextBlockDesignation(List.of(), "A", unnumbered));

        assertEquals("the message must name the setting the deployment has to correct", true,
                thrown.getMessage().contains("blockFormat"));
    }

    @Test
    public void nextDesignation_rejectsTheArgumentsItCannotName() {
        assertThrows("a part with no name cannot number a block", IllegalArgumentException.class,
                () -> PathologyDesignations.nextBlockDesignation(List.of(), "  ", DesignationScheme.defaults()));
        assertThrows("nothing can be named without a scheme", IllegalArgumentException.class,
                () -> PathologyDesignations.nextBlockDesignation(List.of(), "A", null));
        assertThrows("nothing can be named without a scheme", IllegalArgumentException.class,
                () -> PathologyDesignations.blockBarcode(ACCESSION, "A1", null));
    }

    // helpers

    private static List<String> trimmed(Collection<String> designations) {
        return designations.stream().filter(designation -> designation != null && !designation.isBlank())
                .map(String::trim).toList();
    }
}
