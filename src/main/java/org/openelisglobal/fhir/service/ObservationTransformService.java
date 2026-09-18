package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.beanItems.TestResultItem;

/**
 * OpenELIS Result to and from FHIR Observation.
 */
@CrossDomainService(callers = "FHIR transform pipeline — one of the per-resource transformers"
        + " FhirTransformService (itself @CrossDomainService) was decomposed into. Pure resource"
        + " mapping invoked by the import/export pipeline and by sibling transformers; no controller"
        + " references it. The caller's own endpoint carries the privilege gate.")
public interface ObservationTransformService {

    TestResultItem createResultFromObservation(org.hl7.fhir.r4.model.Observation observation);

    Observation transformResultToObservation(String resultId);

    Observation transformResultToObservation(Result result);
}
