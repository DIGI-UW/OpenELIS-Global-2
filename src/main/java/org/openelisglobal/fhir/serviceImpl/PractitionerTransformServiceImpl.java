package org.openelisglobal.fhir.serviceImpl;

import java.util.UUID;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FHIRTransformUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.fhir.service.PractitionerTransformService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PractitionerTransformServiceImpl implements PractitionerTransformService {
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FHIRTransformUtil fhirTransformUtil;

    @Override
    public Practitioner transformProviderToPractitioner(Provider provider) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformProviderToPractitioner",
                "transformProviderToPractitioner called");

        Practitioner practitioner = new Practitioner();
        practitioner.setId(provider.getFhirUuidAsString());
        practitioner.addIdentifier(fhirTransformUtil.createIdentifier(fhirConfig.getOeFhirSystem() + "/provider_uuid",
                provider.getFhirUuidAsString()));
        Identifier facilityId = fhirTransformUtil.createFacilityIdentifier();
        if (facilityId != null) {
            practitioner.addIdentifier(facilityId);
        }
        practitioner.addName(new HumanName().setFamily(provider.getPerson().getLastName())
                .addGiven(provider.getPerson().getFirstName()));
        practitioner.setTelecom(fhirTransformUtil.transformToTelecom(provider.getPerson()));
        practitioner.setActive(provider.getActive());

        return practitioner;
    }

    @Override
    public Provider transformToProvider(Practitioner practitioner) {
        Provider provider = new Provider();
        provider.setActive(practitioner.getActive());
        provider.setFhirUuid(UUID.fromString(practitioner.getIdElement().getIdPart()));

        provider.setPerson(new Person());
        fhirTransformUtil.addHumanNameToPerson(practitioner.getNameFirstRep(), provider.getPerson());
        fhirTransformUtil.addTelecomToPerson(practitioner.getTelecom(), provider.getPerson());

        return provider;
    }

}
