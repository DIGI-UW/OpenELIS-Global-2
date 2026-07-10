package org.openelisglobal.qa.service;

import java.util.Collection;
import java.util.Set;

public interface QaPermissionService {

    /**
     * Names of the qa.* permission modules (FRS §6 registry, seeded in
     * liquibase/qa/004) granted with select access to any of the given roles.
     * Unknown or blank role names are ignored.
     */
    Set<String> getQaPermissionsForRoleNames(Collection<String> roleNames);
}
