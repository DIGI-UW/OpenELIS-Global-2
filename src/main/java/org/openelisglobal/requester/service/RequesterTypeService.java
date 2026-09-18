package org.openelisglobal.requester.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.requester.valueholder.RequesterType;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Gated per method, not at class level: AppTestConfig publishes
 * {@code mock(RequesterTypeService.class)} as a context bean, and Mockito
 * copies a class-level annotation onto the mock, so method security sees
 * {@code @PreAuthorize} twice and throws AnnotationConfigurationException. See
 * WHONetReportService for the failure this prevents.
 */
public interface RequesterTypeService extends BaseObjectService<RequesterType, String> {

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_REQUESTER_VIEW')")
    RequesterType getRequesterTypeByName(String typeName);
}
