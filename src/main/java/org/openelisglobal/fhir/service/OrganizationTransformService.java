package org.openelisglobal.fhir.service;

import org.openelisglobal.organization.valueholder.Organization;

public interface OrganizationTransformService {

    Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization);

    org.hl7.fhir.r4.model.Organization transformToFhirOrganization(Organization organization);

}