package org.openelisglobal.fhir.service;

import org.openelisglobal.organization.valueholder.Organization;

/**
 * OpenELIS Organization to and from FHIR Organization.
 */
public interface OrganizationTransformService {

    org.hl7.fhir.r4.model.Organization transformToFhirOrganization(Organization organization);

    /**
     * The Location that stands for an organization's site: same id as the
     * Organization, managed by it. ServiceRequest.locationReference and the subject
     * of a patientless order point at it. Null for an organization without a FHIR
     * id.
     */
    org.hl7.fhir.r4.model.Location transformToFhirLocation(Organization organization);

    Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization);
}
