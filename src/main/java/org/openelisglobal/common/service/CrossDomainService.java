package org.openelisglobal.common.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service interface as deliberately shared across multiple privilege
 * domains, where no single {@code @PreAuthorize} privilege applies.
 *
 * <p>
 * Cross-domain services are called from multiple unrelated privilege contexts
 * (e.g. order entry, result entry, FHIR pipeline, reporting). Authorization is
 * enforced upstream by the caller; annotating the service method itself would
 * either block legitimate callers or grant overly broad access.
 *
 * <p>
 * Services carrying this annotation are excluded from the
 * {@code allServiceMethods_shouldHaveHasAuthorityPreAuthorize} scan. The
 * annotation must include a {@link #callers()} description explaining which
 * domains call the service, so the exemption is auditable.
 *
 * <p>
 * <strong>Do not use this annotation to avoid adding authorization.</strong> It
 * is only for services that are genuinely called from incompatible privilege
 * contexts. Single-domain services must have per-method
 * {@code @PreAuthorize("hasAuthority('PRIV_*')")} annotations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossDomainService {

    /**
     * Human-readable description of which domains call this service and why a
     * single privilege cannot be assigned.
     */
    String callers();
}
