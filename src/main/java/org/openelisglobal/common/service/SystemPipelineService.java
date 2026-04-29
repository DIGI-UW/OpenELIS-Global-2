package org.openelisglobal.common.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service interface as an internal system pipeline — not reachable by
 * direct user-initiated requests. No {@code @PreAuthorize} is needed because
 * the service is only ever called from scheduled jobs, async pipelines, Spring
 * Security infrastructure, or other system-internal contexts where no user
 * privilege model applies.
 *
 * <p>
 * Services carrying this annotation are excluded from the
 * {@code allServiceMethods_shouldHaveHasPrivilegePreAuthorize} coverage scan.
 *
 * <p>
 * <strong>Do not use this annotation to avoid adding authorization.</strong> It
 * is only for services that are genuinely unreachable from user-initiated
 * request paths. User-facing services must have per-method
 * {@code @PreAuthorize("hasPrivilege('PRIV_*')")} annotations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SystemPipelineService {
}
