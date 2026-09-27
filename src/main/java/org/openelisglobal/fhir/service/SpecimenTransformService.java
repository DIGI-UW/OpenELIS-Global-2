package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * OpenELIS SampleItem to and from FHIR Specimen.
 */
public interface SpecimenTransformService {

    Specimen transformToFhirSpecimen(SampleTestCollection sampleTest);

    SampleItem createSampleItemFromSpecimen(Specimen specimen, String sysuserId);

    Specimen transformToSpecimen(String sampleItemId);

    Specimen transformToSpecimen(SampleItem sampleItem);
}
