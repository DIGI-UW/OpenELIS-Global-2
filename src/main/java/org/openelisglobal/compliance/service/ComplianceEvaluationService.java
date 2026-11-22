package org.openelisglobal.compliance.service;

import java.util.List;
import org.openelisglobal.sample.valueholder.Sample;

public interface ComplianceEvaluationService {

    /**
     * Evaluate all results for a sample against its linked compliance standard.
     * Returns null if no standard is linked or no results exist.
     */
    ComplianceEvaluationResult evaluate(Sample sample);

    /**
     * Whether the order still has a test waiting for a result or for validation
     * (OGC-552, OGC-1620): every test that was not cancelled must be validated and
     * released before a certificate is offered.
     */
    boolean awaitsValidation(Sample sample);

    /** How many tests the order has, not counting cancelled ones. */
    int orderedTestCount(Sample sample);

    /**
     * Every validated result of the order, sample by sample (OGC-1617), with the
     * guideline value and status of each linked standard that sets a limit for the
     * test, one line per such standard. A test no standard covers is listed once
     * with an empty guideline.
     */
    List<EnvironmentalReportRow> reportRows(Sample sample);
}
