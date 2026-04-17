package org.openelisglobal.organization.service;

import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OrganizationExportService {

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    String exportFhirOrganizationsFromOrganizations(boolean active) throws FhirTransformationException;
}
