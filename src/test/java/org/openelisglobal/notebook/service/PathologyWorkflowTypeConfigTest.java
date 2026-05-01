package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PathologyWorkflowTypeConfigTest {

    @Test
    public void normalizesSupportedAliases() {
        assertEquals(PathologyWorkflowTypeConfig.HISTOPATHOLOGY_BIOPSY,
                PathologyWorkflowTypeConfig.normalizeWorkflowType("histopathology"));
        assertEquals(PathologyWorkflowTypeConfig.PERIPHERAL_SMEAR_BONE_MARROW,
                PathologyWorkflowTypeConfig.normalizeWorkflowType("bone_marrow"));
        assertEquals(PathologyWorkflowTypeConfig.CYTOLOGY_LIQUID_PAP,
                PathologyWorkflowTypeConfig.normalizeWorkflowType("pap_smear"));
    }

    @Test
    public void enablesHistopathologyStages() {
        assertTrue(PathologyWorkflowTypeConfig.isStageEnabledForPage(
                PathologyWorkflowTypeConfig.HISTOPATHOLOGY_BIOPSY, "Gross Examination", 3));
        assertTrue(PathologyWorkflowTypeConfig.isStageEnabledForPage(
                PathologyWorkflowTypeConfig.HISTOPATHOLOGY_BIOPSY, "Cassette Setup", 4));
    }

    @Test
    public void skipsHistologyOnlyStagesForFnac() {
        assertFalse(PathologyWorkflowTypeConfig.isStageEnabledForPage(PathologyWorkflowTypeConfig.FNAC,
                "Gross Examination", 3));
        assertFalse(PathologyWorkflowTypeConfig.isStageEnabledForPage(PathologyWorkflowTypeConfig.FNAC,
                "Cassette Setup", 4));
        assertTrue(PathologyWorkflowTypeConfig.isStageEnabledForPage(PathologyWorkflowTypeConfig.FNAC,
                "Microscopy & Diagnosis", 8));
    }

    @Test
    public void cytologyKeepsBlockStageButSkipsGrossAndCassetteStages() {
        assertFalse(PathologyWorkflowTypeConfig.isStageEnabledForPage(
                PathologyWorkflowTypeConfig.CYTOLOGY_LIQUID_PAP, "Gross Examination", 3));
        assertFalse(PathologyWorkflowTypeConfig.isStageEnabledForPage(
                PathologyWorkflowTypeConfig.CYTOLOGY_LIQUID_PAP, "Cassette Setup", 4));
        assertTrue(PathologyWorkflowTypeConfig.isStageEnabledForPage(
                PathologyWorkflowTypeConfig.CYTOLOGY_LIQUID_PAP, "Block Creation", 5));
    }
}
