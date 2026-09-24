package org.openelisglobal.fhir.service;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationTransformServiceImpl implements OrganizationTransformService {

    /** Widths of the ORGANIZATION address columns, which are narrow and legacy. */
    private static final int STREET_ADDRESS_LIMIT = 30;

    private static final int CITY_LIMIT = 30;

    private static final int STATE_LIMIT = 2;

    private static final int POSTAL_CODE_LIMIT = 10;

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private FhirCommonTransformService common;

    @Transactional(readOnly = true)

    @Override
    public org.hl7.fhir.r4.model.Organization transformToFhirOrganization(Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirOrganization",
                "transformToFhirOrganization called");

        org.hl7.fhir.r4.model.Organization fhirOrganization = new org.hl7.fhir.r4.model.Organization();
        fhirOrganization
                .setId(organization.getFhirUuid() == null ? organization.getId() : organization.getFhirUuidAsString());
        fhirOrganization.getMeta().setLastUpdated(organization.getLastupdated());
        fhirOrganization.setName(organization.getOrganizationName());
        fhirOrganization.setActive(!IActionConstants.NO.equals(organization.getIsActive()));
        this.setFhirOrganizationIdentifiers(fhirOrganization, organization);
        this.setFhirAddressInfo(fhirOrganization, organization);
        this.setFhirOrganizationTypes(fhirOrganization, organization);
        if (organization.getOrganization() != null && organization.getOrganization().getFhirUuid() != null) {
            fhirOrganization.setPartOf(common.createReferenceFor(ResourceType.Organization,
                    organization.getOrganization().getFhirUuidAsString()));
        }
        return fhirOrganization;
    }

    @Transactional(readOnly = true)

    @Override
    public Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToOrganization", "transformToOrganization called");

        Organization organization = new Organization();
        organization.setOrganizationName(fhirOrganization.getName());
        organization.setIsActive(Boolean.FALSE == fhirOrganization.getActiveElement().getValue() ? IActionConstants.NO
                : IActionConstants.YES);

        setOeOrganizationIdentifiers(organization, fhirOrganization);
        setOeOrganizationAddressInfo(organization, fhirOrganization);
        setOeOrganizationTypes(organization, fhirOrganization);

        organization.setMlsLabFlag(IActionConstants.NO);
        organization.setMlsSentinelLabFlag(IActionConstants.NO);

        return organization;
    }

    private void setOeOrganizationIdentifiers(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOeOrganizationIdentifiers",
                "setOeOrganizationIdentifiers called");

        organization.setFhirUuid(UUID.fromString(fhirOrganization.getIdElement().getIdPart()));
        for (Identifier identifier : fhirOrganization.getIdentifier()) {
            if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_cliaNum")) {
                organization.setCliaNum(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_shortName")) {
                organization.setShortName(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_code")) {
                organization.setCode(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_uuid")) {
                organization.setFhirUuid(UUID.fromString(identifier.getValue()));
            }
        }
    }

    private void setFhirOrganizationIdentifiers(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setFhirOrganizationIdentifiers",
                "setFhirOrganizationIdentifiers called");

        if (!GenericValidator.isBlankOrNull(organization.getCliaNum())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_cliaNum")
                    .setValue(organization.getCliaNum()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getShortName())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_shortName")
                    .setValue(organization.getShortName()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getCode())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_code")
                    .setValue(organization.getCode()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getCode())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_uuid")
                    .setValue(organization.getFhirUuidAsString()));
        }
        Identifier facilityId = common.createFacilityIdentifier();
        if (facilityId != null) {
            fhirOrganization.addIdentifier(facilityId);
        }
    }

    private void setOeOrganizationTypes(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOeOrganizationTypes", "setOeOrganizationTypes called");

        Set<OrganizationType> orgTypes = new HashSet<>();
        OrganizationType orgType = null;
        for (CodeableConcept type : fhirOrganization.getType()) {
            for (Coding coding : type.getCoding()) {
                if (coding.getSystem() != null
                        && coding.getSystem().equals(fhirConfig.getOeFhirSystem() + "/orgType")) {
                    orgType = new OrganizationType();
                    orgType.setName(coding.getCode());
                    orgType.setDescription(type.getText());
                    orgType.setNameKey("org_type." + coding.getCode() + ".name");
                    orgType.setOrganizations(new HashSet<>());
                    orgType.getOrganizations().add(organization);
                    orgTypes.add(orgType);
                }
            }
        }
        organization.setOrganizationTypes(orgTypes);
    }

    private void setFhirOrganizationTypes(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setFhirOrganizationTypes",
                "setFhirOrganizationTypes called");

        Set<OrganizationType> orgTypes = organizationService.get(organization.getId()).getOrganizationTypes();
        for (OrganizationType orgType : orgTypes) {
            fhirOrganization.addType(new CodeableConcept() //
                    .setText(orgType.getDescription()) //
                    .addCoding(new Coding() //
                            .setSystem(fhirConfig.getOeFhirSystem() + "/orgType") //
                            .setCode(orgType.getName())));
        }
    }

    /**
     * Copies the address onto the organization, rejecting any part that the column
     * behind it cannot hold.
     *
     * <p>
     * These are narrow legacy columns - {@code STATE} in particular is a two
     * character US state code - and writing through them unchecked made the
     * database abort the whole insert, which reached the caller as a 422 carrying
     * the entire generated SQL statement and no indication of which field was at
     * fault. Naming the field and its limit up front is the difference between a
     * client being able to correct the payload and not.
     */
    private void setOeOrganizationAddressInfo(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOeOrganizationAddressInfo",
                "setOeOrganizationAddressInfo called");

        Address address = fhirOrganization.getAddressFirstRep();
        organization.setStreetAddress(requireFits("Organization.address.line",
                address.getLine().stream().map(e -> e.asStringValue()).collect(Collectors.joining("\\n")),
                STREET_ADDRESS_LIMIT));
        organization.setCity(requireFits("Organization.address.city", address.getCity(), CITY_LIMIT));
        organization.setState(requireFits("Organization.address.state", address.getState(), STATE_LIMIT));
        organization
                .setZipCode(requireFits("Organization.address.postalCode", address.getPostalCode(), POSTAL_CODE_LIMIT));
    }

    private String requireFits(String fhirPath, String value, int limit) {
        if (value != null && value.length() > limit) {
            throw new UnprocessableEntityException(fhirPath + " is limited to " + limit
                    + " characters in this database, but was " + value.length() + " characters");
        }
        return value;
    }

    private void setFhirAddressInfo(org.hl7.fhir.r4.model.Organization fhirOrganization, Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setFhirAddressInfo", "setFhirAddressInfo called");

        if (!GenericValidator.isBlankOrNull(organization.getStreetAddress())) {
            fhirOrganization.getAddressFirstRep().addLine(organization.getStreetAddress());
        }
        if (!GenericValidator.isBlankOrNull(organization.getCity())) {
            fhirOrganization.getAddressFirstRep().setCity(organization.getCity());
        }
        if (!GenericValidator.isBlankOrNull(organization.getState())) {
            fhirOrganization.getAddressFirstRep().setState(organization.getState());
        }
        if (!GenericValidator.isBlankOrNull(organization.getZipCode())) {
            fhirOrganization.getAddressFirstRep().setPostalCode(organization.getZipCode());
        }
    }
}
