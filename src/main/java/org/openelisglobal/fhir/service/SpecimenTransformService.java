package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public interface SpecimenTransformService {

    Specimen transformToSpecimen(SampleItem sampleItem);

    SampleItem createSampleItemFromSpecimen(Specimen specimen, String sysuserId);

}
