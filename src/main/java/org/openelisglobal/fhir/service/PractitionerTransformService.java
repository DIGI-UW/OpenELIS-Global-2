package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.provider.valueholder.Provider;

public interface PractitionerTransformService {

    Practitioner transformProviderToPractitioner(Provider provider);

    Provider transformToProvider(Practitioner practitioner);

}
