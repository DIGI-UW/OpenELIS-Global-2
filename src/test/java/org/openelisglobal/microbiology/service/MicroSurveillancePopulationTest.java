package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class MicroSurveillancePopulationTest {
    @Test
    public void whonetRequiresExplicitEnvironmentalScreeningAndSurveyChoices() {
        assertTrue(MicroSurveillancePopulation.whonetPurpose("DIAGNOSTIC", false, false, false, false));
        assertTrue(MicroSurveillancePopulation.whonetPurpose("TREATMENT_FOLLOW_UP", false, false, false, false));
        assertFalse(MicroSurveillancePopulation.whonetPurpose("SCREENING", false, false, false, false));
        assertTrue(MicroSurveillancePopulation.whonetPurpose("SCREENING", false, false, true, false));
        assertFalse(MicroSurveillancePopulation.whonetPurpose("SURVEY_OR_STUDY", false, false, true, false));
        assertTrue(MicroSurveillancePopulation.whonetPurpose("SURVEY_OR_STUDY", false, false, false, true));
        assertFalse(MicroSurveillancePopulation.whonetPurpose("EQA_PROFICIENCY", false, true, true, true));
        assertFalse(MicroSurveillancePopulation.whonetPurpose("OUTBREAK_INVESTIGATION", true, false, false, false));
        assertTrue(MicroSurveillancePopulation.whonetPurpose("OUTBREAK_INVESTIGATION", true, true, false, false));
    }

    @Test
    public void clinicalPopulationsNeverIncludeEnvironmentalOrExternalResults() {
        assertTrue(MicroSurveillancePopulation.clinicalSurveillance("DIAGNOSTIC", false, false));
        assertFalse(MicroSurveillancePopulation.clinicalSurveillance("DIAGNOSTIC", true, false));
        assertFalse(MicroSurveillancePopulation.clinicalSurveillance("DIAGNOSTIC", false, true));
        assertFalse(MicroSurveillancePopulation.clinicalSurveillance("TREATMENT_FOLLOW_UP", false, false));
    }

    @Test
    public void noCultureAndContaminatedTbAreExcludedButReceivedIsolatesCount() {
        assertTrue(MicroSurveillancePopulation.excludedCulture("Bacterial", List.of(), false));
        assertTrue(MicroSurveillancePopulation.excludedCulture("TB", List.of("CONTAMINATED"), false));
        assertFalse(MicroSurveillancePopulation.excludedCulture("Bacterial", List.of("CONTAMINATED"), false));
        assertFalse(MicroSurveillancePopulation.excludedCulture("Bacterial", List.of(), true));
    }
}
