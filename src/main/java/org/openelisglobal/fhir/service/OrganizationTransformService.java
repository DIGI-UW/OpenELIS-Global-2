package org.openelisglobal.fhir.service;

import org.openelisglobal.organization.valueholder.Organization;

/**
 * OpenELIS Organization to and from FHIR Organization.
 */
public interface OrganizationTransformService {

    org.hl7.fhir.r4.model.Organization transformToFhirOrganization(Organization organization);

    Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization);
}
