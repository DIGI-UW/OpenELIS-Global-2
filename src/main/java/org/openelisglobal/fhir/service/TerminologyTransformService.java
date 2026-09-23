package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Test, component and sample-type codings shared by ServiceRequest, Observation
 * and DiagnosticReport transforms.
 */
public interface TerminologyTransformService {

    CodeableConcept transformTestToCodeableConcept(String testId, String sampleTypeId);

    CodeableConcept transformTestToCodeableConcept(Test test, String sampleTypeId);

    org.openelisglobal.testresultcomponent.valueholder.TestResultComponent resolveResultComponent(String testId,
            Result result);

    CodeableConcept transformResultCodeableConcept(Test test,
            org.openelisglobal.testresultcomponent.valueholder.TestResultComponent component, String sampleTypeId);

    CodeableConcept transformTypeOfSampleToCodeableConcept(String typeOfSampleId);

    CodeableConcept transformTypeOfSampleToCodeableConcept(TypeOfSample typeOfSample);
}
