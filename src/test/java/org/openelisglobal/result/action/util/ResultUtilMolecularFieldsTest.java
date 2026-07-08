package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.test.beanItems.TestResultItem;

/**
 * Molecular Biology (PCR) capture — the row DTO carries target_gene / ct_value
 * from the browser and the two update paths must copy those onto the Analysis
 * entity so Hibernate persists them. The Analysis edit paths run through
 * {@link ResultUtil#applyMolecularFields} on both the analysis-only and
 * full-results branches; this test locks the mapping without a Spring context.
 */
public class ResultUtilMolecularFieldsTest {

    @Test
    public void applyMolecularFields_copiesTargetGeneAndCtValueOntoAnalysis() {
        Analysis analysis = new Analysis();
        TestResultItem item = new TestResultItem();
        item.setTargetGene("N1");
        item.setCtValue("32.4");

        ResultUtil.applyMolecularFields(analysis, item);

        assertEquals("N1", analysis.getTargetGene());
        assertEquals("32.4", analysis.getCtValue());
    }

    @Test
    public void applyMolecularFields_nullsAreCarriedThrough_soNonMolecularRowsDoNotSpuriouslyPopulate() {
        // Non-molecular rows come back through the save path with null values
        // — the mapping must not fabricate empty strings that would override
        // existing data on unrelated analyses.
        Analysis analysis = new Analysis();
        analysis.setTargetGene("prior");
        analysis.setCtValue("prior");
        TestResultItem item = new TestResultItem();

        ResultUtil.applyMolecularFields(analysis, item);

        assertNull(analysis.getTargetGene());
        assertNull(analysis.getCtValue());
    }
}
