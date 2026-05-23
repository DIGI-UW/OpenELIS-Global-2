package org.openelisglobal.common.constants.rbac;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.openelisglobal.common.constants.Constants;

/**
 * Catalog of AHRI role names used for UI/controller filtering.
 */
public final class AHRIRoleCatalog {

    private static final Set<String> GLOBAL_ROLE_NAMES;
    private static final Set<String> DEPARTMENT_ROLE_NAMES;
    private static final Set<String> PROJECT_ROLE_NAMES;
    private static final List<String> DEPARTMENT_ROLE_ORDER;
    private static final List<String> PROJECT_ROLE_ORDER;
    private static final List<String> GLOBAL_ROLE_ORDER;

    static {
        Set<String> globals = new HashSet<>();
        globals.add(Constants.ROLE_SYSTEM_ADMIN);
        globals.add(Constants.ROLE_GLOBAL_ADMIN);
        globals.add(Constants.ROLE_USER_ACCOUNT_ADMIN);
        globals.add(Constants.ROLE_AUDIT_TRAIL);
        globals.add(Constants.ROLE_ADMINISTRATIVE_STAFF);
        globals.add(Constants.ROLE_IT_SUPPORT_STAFF);
        globals.add(Constants.ROLE_EQA_PERSONNEL);
        globals.add(Constants.ROLE_EXTERNAL_STAKEHOLDERS);
        GLOBAL_ROLE_NAMES = normalizeAll(globals);

        Set<String> departments = new HashSet<>();
        departments.add(Constants.ROLE_SAMPLE_COLLECTOR);
        departments.add(Constants.ROLE_LABORATORY_TECHNICIAN);
        departments.add(Constants.ROLE_JUNIOR_RESEARCHER);
        departments.add(Constants.ROLE_SENIOR_RESEARCHER);
        departments.add(Constants.ROLE_LAB_MANAGER);
        departments.add(Constants.ROLE_BIOMEDICAL_STAFF);
        DEPARTMENT_ROLE_NAMES = normalizeAll(departments);

        Set<String> projects = new HashSet<>();
        projects.add(Constants.ROLE_PRINCIPAL_INVESTIGATOR);
        projects.add(Constants.ROLE_PROJECT_COORDINATOR);
        projects.add(Constants.ROLE_DATA_MANAGER);
        PROJECT_ROLE_NAMES = normalizeAll(projects);

        GLOBAL_ROLE_ORDER = List.of(Constants.ROLE_GLOBAL_ADMIN, Constants.ROLE_SYSTEM_ADMIN,
                Constants.ROLE_USER_ACCOUNT_ADMIN, Constants.ROLE_AUDIT_TRAIL, Constants.ROLE_ADMINISTRATIVE_STAFF,
                Constants.ROLE_IT_SUPPORT_STAFF, Constants.ROLE_EQA_PERSONNEL, Constants.ROLE_EXTERNAL_STAKEHOLDERS);

        DEPARTMENT_ROLE_ORDER = List.of(Constants.ROLE_SAMPLE_COLLECTOR, Constants.ROLE_LABORATORY_TECHNICIAN,
                Constants.ROLE_JUNIOR_RESEARCHER, Constants.ROLE_SENIOR_RESEARCHER, Constants.ROLE_LAB_MANAGER,
                Constants.ROLE_BIOMEDICAL_STAFF);

        PROJECT_ROLE_ORDER = List.of(Constants.ROLE_PRINCIPAL_INVESTIGATOR, Constants.ROLE_PROJECT_COORDINATOR,
                Constants.ROLE_DATA_MANAGER);
    }

    private AHRIRoleCatalog() {
        // Utility class
    }

    public static boolean isGlobalRoleName(String roleName) {
        return GLOBAL_ROLE_NAMES.contains(normalizeRoleName(roleName));
    }

    public static boolean isDepartmentRoleName(String roleName) {
        return DEPARTMENT_ROLE_NAMES.contains(normalizeRoleName(roleName));
    }

    public static boolean isProjectRoleName(String roleName) {
        return PROJECT_ROLE_NAMES.contains(normalizeRoleName(roleName));
    }

    public static List<String> globalRoleDisplayOrder() {
        return GLOBAL_ROLE_ORDER;
    }

    public static List<String> departmentRoleDisplayOrder() {
        return DEPARTMENT_ROLE_ORDER;
    }

    public static List<String> projectRoleDisplayOrder() {
        return PROJECT_ROLE_ORDER;
    }

    public static int displayOrderIndex(List<String> order, String roleName) {
        String normalized = normalizeRoleName(roleName);
        for (int i = 0; i < order.size(); i++) {
            if (normalizeRoleName(order.get(i)).equals(normalized)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    public static String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "";
        }
        return roleName.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static Set<String> normalizeAll(Set<String> source) {
        Set<String> normalized = new HashSet<>();
        for (String value : source) {
            normalized.add(normalizeRoleName(value));
        }
        return Collections.unmodifiableSet(normalized);
    }
}
