package org.openelisglobal.compliance.service;

import org.openelisglobal.sample.valueholder.Sample;

public interface ComplianceEvaluationService {

    /**
     * Evaluate all results for a sample against its linked compliance standard.
     * Returns null if no standard is linked or no results exist.
     */
    ComplianceEvaluationResult evaluate(Sample sample);
}
