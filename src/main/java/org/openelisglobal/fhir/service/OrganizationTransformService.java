package org.openelisglobal.fhir.service;

import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.organization.valueholder.Organization;

/**
 * OpenELIS Organization to and from FHIR Organization.
 */
@CrossDomainService(callers = "FHIR transform pipeline — one of the per-resource transformers"
        + " FhirTransformService (itself @CrossDomainService) was decomposed into. Pure resource"
        + " mapping invoked by the import/export pipeline and by sibling transformers; no controller"
        + " references it. The caller's own endpoint carries the privilege gate.")
public interface OrganizationTransformService {

    org.hl7.fhir.r4.model.Organization transformToFhirOrganization(Organization organization);

    Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization);
}
