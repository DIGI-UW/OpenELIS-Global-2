package org.openelisglobal.fhir.serviceImpl;

import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FHIRTransformUtil;
import org.openelisglobal.fhir.service.OrganizationTransformService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationTransformServiceImpl implements OrganizationTransformService {
    @Autowired
    private FHIRTransformUtil fhirTransformUtil;

    @Override
    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Organization transformToFhirOrganization(Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirOrganization",
                "transformToFhirOrganization called");

        org.hl7.fhir.r4.model.Organization fhirOrganization = new org.hl7.fhir.r4.model.Organization();
        fhirOrganization
                .setId(organization.getFhirUuid() == null ? organization.getId() : organization.getFhirUuidAsString());
        fhirOrganization.setName(organization.getOrganizationName());
        fhirOrganization.setActive(organization.getIsActive().equals(IActionConstants.YES) ? true : false);
        fhirTransformUtil.setFhirOrganizationIdentifiers(fhirOrganization, organization);
        fhirTransformUtil.setFhirAddressInfo(fhirOrganization, organization);
        fhirTransformUtil.setFhirOrganizationTypes(fhirOrganization, organization);
        return fhirOrganization;
    }

    @Override
    @Transactional(readOnly = true)
    public Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToOrganization", "transformToOrganization called");

        Organization organization = new Organization();
        organization.setOrganizationName(fhirOrganization.getName());
        organization.setIsActive(Boolean.FALSE == fhirOrganization.getActiveElement().getValue() ? IActionConstants.NO
                : IActionConstants.YES);

        fhirTransformUtil.setOeOrganizationIdentifiers(organization, fhirOrganization);
        fhirTransformUtil.setOeOrganizationAddressInfo(organization, fhirOrganization);
        fhirTransformUtil.setOeOrganizationTypes(organization, fhirOrganization);

        organization.setMlsLabFlag(IActionConstants.NO);
        organization.setMlsSentinelLabFlag(IActionConstants.NO);

        return organization;
    }
}
