package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PathologyWorkflowTypeConfigTest {

    @Test
    public void canonicalStageOrder_mapsLegacyPathologyTitlesToCanonicalStages() {
        assertEquals(Integer.valueOf(1),
                PathologyWorkflowTypeConfig.canonicalStageOrder("Sample Creation & Metadata Capture", 1));
        assertEquals(Integer.valueOf(9),
                PathologyWorkflowTypeConfig.canonicalStageOrder("Microscopy & Diagnosis", 8));
        assertEquals(Integer.valueOf(11),
                PathologyWorkflowTypeConfig.canonicalStageOrder("Storage & Inventory Management", 9));
        assertEquals(Integer.valueOf(12),
                PathologyWorkflowTypeConfig.canonicalStageOrder("Reporting & Performance Monitoring", 10));
        assertEquals(Integer.valueOf(13),
                PathologyWorkflowTypeConfig.canonicalStageOrder("Disposal & Archiving", 11));
    }

    @Test
    public void isStageEnabledForPage_usesCanonicalStageForLegacyPathologyPages() {
        assertTrue(PathologyWorkflowTypeConfig.isStageEnabledForPage(PathologyWorkflowTypeConfig.FNAC,
                "Slide Preparation", 6));
        assertFalse(PathologyWorkflowTypeConfig.isStageEnabledForPage(PathologyWorkflowTypeConfig.FNAC,
                "Gross Examination", 3));
        assertTrue(PathologyWorkflowTypeConfig.isStageEnabledForPage(PathologyWorkflowTypeConfig.FNAC,
                "Reporting & Performance Monitoring", 10));
        assertTrue(PathologyWorkflowTypeConfig.isStageEnabledForPage(PathologyWorkflowTypeConfig.FNAC,
                "Disposal & Archiving", 11));
    }
}
