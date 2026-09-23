package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Gated per method, not at class level: AppTestConfig publishes
 * {@code mock(OrganizationTypeService.class)} as a context bean, and Mockito
 * copies a class-level annotation onto the mock, so method security sees
 * {@code @PreAuthorize} twice and throws AnnotationConfigurationException. Same
 * privilege on every method — a packaging change, not a policy change. See
 * WHONetReportService for the failure this prevents.
 */
public interface OrganizationTypeService extends BaseObjectService<OrganizationType, String> {

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<OrganizationType> getAllOrganizationTypes();

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    OrganizationType getOrganizationTypeByName(String name);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<String> getOrganizationIdsForType(String typeId);
}
