package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.beanItems.TestResultItem;

public interface ObservationTransformService {

    Observation transformResultToObservation(Result result);

    TestResultItem createResultFromObservation(Observation observation);

}