package org.openelisglobal.fhir.service;

import java.util.UUID;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PractitionerTransformServiceImpl implements PractitionerTransformService {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private FhirCommonTransformService common;

    @Override
    public Practitioner transformProviderToPractitioner(String providerId) {
        return transformProviderToPractitioner(providerService.get(providerId));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A provider without a FHIR uuid still has to come back carrying an id: HAPI
     * rejects a whole bundle over one id-less resource, so a single such row would
     * otherwise make every Practitioner search fail.
     */
    @Override
    public Practitioner transformProviderToPractitioner(Provider provider) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformProviderToPractitioner",
                "transformProviderToPractitioner called");

        Practitioner practitioner = new Practitioner();
        practitioner.setId(provider.getFhirUuid() == null ? provider.getId() : provider.getFhirUuidAsString());
        practitioner.getMeta().setLastUpdated(provider.getLastupdated());
        practitioner.addIdentifier(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/provider_uuid",
                provider.getFhirUuidAsString()));
        Identifier facilityId = common.createFacilityIdentifier();
        if (facilityId != null) {
            practitioner.addIdentifier(facilityId);
        }
        practitioner.addName(new HumanName().setFamily(provider.getPerson().getLastName())
                .addGiven(provider.getPerson().getFirstName()));
        practitioner.setTelecom(common.transformToTelecom(provider.getPerson()));
        Address address = common.transformToAddress(provider.getPerson());
        if (!address.isEmpty()) {
            practitioner.addAddress(address);
        }
        practitioner.setActive(provider.getActive());

        return practitioner;
    }

    @Override
    public Practitioner transformNameToPractitioner(String practitionerName) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformNameToPractitioner",
                "transformNameToPractitioner called");

        Practitioner practitioner = new Practitioner();
        HumanName name = practitioner.addName();

        if (practitionerName.contains(",")) {
            String[] names = practitionerName.split(",", 2);
            name.setFamily(names[0]);
            for (int i = 1; i < names.length; ++i) {
                name.addGiven(names[i]);
            }
        } else {
            String[] names = practitionerName.split(" ");
            if (names.length >= 1) {
                name.setFamily(names[names.length - 1]);
                for (int i = 0; i < names.length - 1; ++i) {
                    name.addGiven(names[i]);
                }
            }
        }
        return practitioner;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The address is carried across because {@code address-city},
     * {@code address-state}, {@code address-postalcode} and {@code address-country}
     * are declared search parameters that read the person's address columns;
     * leaving them unpopulated made those four parameters unable to match a
     * practitioner created through this facade.
     */
    @Override
    public Provider transformToProvider(Practitioner practitioner) {
        Provider provider = new Provider();
        provider.setActive(practitioner.getActive());
        provider.setFhirUuid(UUID.fromString(practitioner.getIdElement().getIdPart()));

        provider.setPerson(new Person());
        common.addHumanNameToPerson(practitioner.getNameFirstRep(), provider.getPerson());
        common.addTelecomToPerson(practitioner.getTelecom(), provider.getPerson());
        common.addAddressToPerson(practitioner.getAddressFirstRep(), provider.getPerson());

        return provider;
    }
}
