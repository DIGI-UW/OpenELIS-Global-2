package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * OpenELIS SampleItem to and from FHIR Specimen.
 */
@CrossDomainService(callers = "FHIR transform pipeline — one of the per-resource transformers"
        + " FhirTransformService (itself @CrossDomainService) was decomposed into. Pure resource"
        + " mapping invoked by the import/export pipeline and by sibling transformers; no controller"
        + " references it. The caller's own endpoint carries the privilege gate.")
public interface SpecimenTransformService {

    Specimen transformToFhirSpecimen(SampleTestCollection sampleTest);

    SampleItem createSampleItemFromSpecimen(Specimen specimen, String sysuserId);

    Specimen transformToSpecimen(String sampleItemId);

    Specimen transformToSpecimen(SampleItem sampleItem);
}
