package org.openelisglobal.organization.service;

import ca.uhn.fhir.context.FhirContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Resource;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationExportServiceImpl implements OrganizationExportService {

    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private OrganizationService organizationService;

    @Transactional(readOnly = true)
    @Override
    public String exportFhirOrganizationsFromOrganizations(boolean active) throws FhirTransformationException {
        List<Organization> organizations;
        if (active) {
            organizations = organizationService.getActiveOrganizations();
        } else {
            organizations = organizationService.getAll();
        }
        int i = 0;

        Map<String, Resource> resources = new HashMap<>();
        for (Organization organization : organizations) {
            org.hl7.fhir.r4.model.Organization fhirOrganization = fhirTransformService
                    .transformToFhirOrganization(organization);
            if (!resources.containsKey(fhirOrganization.getIdElement().getIdPart())) {
                resources.put(fhirOrganization.getIdElement().getIdPart(), fhirOrganization);
            }

            if (!GenericValidator.isBlankOrNull(organization.getInternetAddress())) {
                Endpoint endpoint = addFhirConnectionInfo(fhirOrganization, organization, String.valueOf(++i));
                resources.put(endpoint.getIdElement().getIdPart(), endpoint);
            }
            if (organization.getOrganization() != null) {
                org.hl7.fhir.r4.model.Organization parentFhirOrganization = addFhirParentOrg(fhirOrganization,
                        organization);
                if (!resources.containsKey(parentFhirOrganization.getIdElement().getIdPart())) {
                    resources.put(parentFhirOrganization.getIdElement().getIdPart(), parentFhirOrganization);
                }
            }
        }
        Bundle transactionBundle = fhirPersistanceService.makeTransactionBundleForCreate(resources);
        return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(transactionBundle);
    }

    private Endpoint addFhirConnectionInfo(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization, String tempId) {
        Endpoint endpoint = new Endpoint() //
                .setAddress(organization.getInternetAddress());
        endpoint.setId(tempId);
        fhirOrganization.addEndpoint(fhirTransformService.createReferenceFor(endpoint));
        return endpoint;
    }

    private org.hl7.fhir.r4.model.Organization addFhirParentOrg(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization) throws FhirTransformationException {
        org.hl7.fhir.r4.model.Organization partOfOrg = fhirTransformService
                .transformToFhirOrganization(organization.getOrganization());
        fhirOrganization.setPartOf(fhirTransformService.createReferenceFor(partOfOrg));
        return partOfOrg;
    }
}
