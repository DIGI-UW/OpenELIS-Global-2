package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.result.valueholder.Result;

/**
 * OpenELIS Result to FHIR Observation.
 */
public interface ObservationTransformService {

    Observation transformResultToObservation(String resultId);

    Observation transformResultToObservation(Result result);
}
