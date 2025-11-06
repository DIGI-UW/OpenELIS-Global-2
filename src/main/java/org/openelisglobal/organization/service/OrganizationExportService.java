package org.openelisglobal.organization.service;

import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;

public interface OrganizationExportService {

    String exportFhirOrganizationsFromOrganizations(boolean active) throws FhirTransformationException;
}
