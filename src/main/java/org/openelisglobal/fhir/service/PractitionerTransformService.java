package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.provider.valueholder.Provider;

/**
 * OpenELIS Provider to and from FHIR Practitioner.
 */
@CrossDomainService(callers = "FHIR transform pipeline — one of the per-resource transformers"
        + " FhirTransformService (itself @CrossDomainService) was decomposed into. Pure resource"
        + " mapping invoked by the import/export pipeline and by sibling transformers; no controller"
        + " references it. The caller's own endpoint carries the privilege gate.")
public interface PractitionerTransformService {

    Practitioner transformProviderToPractitioner(String providerId);

    Practitioner transformProviderToPractitioner(Provider provider);

    Practitioner transformNameToPractitioner(String practitionerName);

    Provider transformToProvider(Practitioner practitioner);
}
