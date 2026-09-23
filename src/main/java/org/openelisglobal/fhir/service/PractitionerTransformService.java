package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.provider.valueholder.Provider;

/**
 * OpenELIS Provider to and from FHIR Practitioner.
 */
public interface PractitionerTransformService {

    Practitioner transformProviderToPractitioner(String providerId);

    Practitioner transformProviderToPractitioner(Provider provider);

    Practitioner transformNameToPractitioner(String practitionerName);

    Provider transformToProvider(Practitioner practitioner);
}
