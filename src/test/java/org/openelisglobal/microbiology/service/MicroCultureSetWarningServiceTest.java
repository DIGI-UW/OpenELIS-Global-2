package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.microbiology.form.MicroCaseSpecimenForm;

public class MicroCultureSetWarningServiceTest {
    @Test
    public void singleBottleWarningsNameTheirSetAndIgnoreOrdinaryOrUnassignedSamples() {
        var ordinary = bottle(9, "Aerobic", "Left", "2026-10-07 07:00:00");
        ordinary.collectedInSets = false;
        var missing = bottle(null, null, null, null);
        var warnings = new MicroCultureSetWarningService(30)
                .evaluate(List.of(bottle(3, "Aerobic", "Left", "2026-10-07 07:00:00"), ordinary, missing));
        assertEquals(1, warnings.size());
        assertEquals(Integer.valueOf(3), warnings.get(0).setNumber());
        assertEquals("SINGLE_BOTTLE", warnings.get(0).code());
    }

    @Test
    public void duplicateContainersAndDifferentSitesWarnWithoutChangingAssignments() {
        var first = bottle(2, " Aerobic ", "Left arm", "2026-10-07 07:00:00");
        var second = bottle(2, "aerobic", "Right arm", "2026-10-07 07:10:00");
        var warnings = new MicroCultureSetWarningService(30).evaluate(List.of(first, second));
        assertEquals(List.of("REPEATED_CONTAINER", "DIFFERENT_SITES"), warnings.stream().map(w -> w.code()).toList());
        assertEquals(Integer.valueOf(2), first.cultureSetNumber);
        assertEquals(Integer.valueOf(2), second.cultureSetNumber);
    }

    @Test
    public void collectionIntervalUsesConfiguredThresholdAndStrictBoundaryAcrossMidnight() {
        var first = bottle(1, "Aerobic", "Left", "2026-10-07 23:50:00");
        var second = bottle(1, "Anaerobic", "Left", "2026-10-08 00:20:00");
        assertTrue(new MicroCultureSetWarningService(30).evaluate(List.of(first, second)).isEmpty());
        second.collectionDate = Timestamp.valueOf("2026-10-08 00:20:01");
        var warnings = new MicroCultureSetWarningService(30).evaluate(List.of(first, second));
        assertEquals("COLLECTION_INTERVAL", warnings.get(0).code());
        assertEquals(30, warnings.get(0).intervalMinutes());
        assertTrue(new MicroCultureSetWarningService(45).evaluate(List.of(first, second)).isEmpty());
    }

    @Test
    public void missingDetailsDoNotBecomeDuplicateOrSiteWarnings() {
        assertTrue(new MicroCultureSetWarningService(30)
                .evaluate(List.of(bottle(1, null, null, null), bottle(1, "", " ", null))).isEmpty());
    }

    @Test
    public void populationWarningUsesOnlyExplicitClassification() {
        var adult = bottle(1, "Paediatric sounding label", null, null);
        var child = bottle(1, "Adult sounding label", null, null);
        assertTrue(new MicroCultureSetWarningService(30).evaluate(List.of(adult, child)).isEmpty());
        adult.containerPopulation = "ADULT";
        child.containerPopulation = "PAEDIATRIC";
        assertEquals(List.of("MIXED_POPULATIONS"), new MicroCultureSetWarningService(30).evaluate(List.of(adult, child))
                .stream().map(w -> w.code()).toList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveConfiguration() {
        new MicroCultureSetWarningService(0);
    }

    private MicroCaseSpecimenForm bottle(Integer set, String container, String site, String time) {
        var specimen = new MicroCaseSpecimenForm();
        specimen.collectedInSets = true;
        specimen.cultureSetNumber = set;
        specimen.containerType = container;
        specimen.bodySite = site;
        specimen.collectionDate = time == null ? null : Timestamp.valueOf(time);
        return specimen;
    }
}
