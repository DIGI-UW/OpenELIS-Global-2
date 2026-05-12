package org.openelisglobal.common.constants.rbac;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.common.constants.Constants;

/**
 * AHRI project role helper.
 */
public final class ProjectRole {

    private static final Set<String> ROLE_NAMES;

    static {
        Set<String> names = new HashSet<>();
        names.add(AHRIRoleCatalog.normalizeRoleName(Constants.ROLE_PRINCIPAL_INVESTIGATOR));
        names.add(AHRIRoleCatalog.normalizeRoleName(Constants.ROLE_PROJECT_COORDINATOR));
        names.add(AHRIRoleCatalog.normalizeRoleName(Constants.ROLE_DATA_MANAGER));
        ROLE_NAMES = Collections.unmodifiableSet(names);
    }

    private ProjectRole() {
        // Utility class
    }

    public static Set<String> roleNames() {
        return ROLE_NAMES;
    }
}
