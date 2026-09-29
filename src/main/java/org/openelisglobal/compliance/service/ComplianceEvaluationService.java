package org.openelisglobal.compliance.service;

import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.sample.valueholder.Sample;

@CrossDomainService(callers = "internal evaluation engine invoked by the gated FHIR/sample create path; not a user-facing entry")
public interface ComplianceEvaluationService {

    /**
     * Evaluate all results for a sample against its linked compliance standard.
     * Returns null if no standard is linked or no results exist.
     */
    ComplianceEvaluationResult evaluate(Sample sample);
}
