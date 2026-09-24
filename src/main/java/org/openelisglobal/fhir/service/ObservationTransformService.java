package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.beanItems.TestResultItem;

/**
 * OpenELIS Result to and from FHIR Observation.
 */
public interface ObservationTransformService {

    TestResultItem createResultFromObservation(org.hl7.fhir.r4.model.Observation observation);

    Observation transformResultToObservation(String resultId);

    Observation transformResultToObservation(Result result);
}
