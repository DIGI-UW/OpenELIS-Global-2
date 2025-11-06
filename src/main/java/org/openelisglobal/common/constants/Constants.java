package org.openelisglobal.common.constants;

public class Constants {

    private Constants() {
        // hide public constructor
    }

    public static final String SUCCESS_MSG = "successMessage";
    public static final String REQUEST_MESSAGES = "requestMessages"; // unimplemented in display layer
    public static final String REQUEST_WARNINGS = "requstWarnings";
    public static final String REQUEST_ERRORS = "requestErrors";
    public static final String LOGIN_ERRORS = "loginErrors";
    // all active roles
    public static final String ROLE_GLOBAL_ADMIN = "Global Administrator";
    public static final String ROLE_USER_ACCOUNT_ADMIN = "User Account Administrator";
    public static final String ROLE_AUDIT_TRAIL = "Audit Trail";
    public static final String ROLE_RECEPTION = "Reception";
    public static final String ROLE_RESULTS = "Results";
    public static final String ROLE_VALIDATION = "Validation";
    public static final String ROLE_REPORTS = "Reports";
    public static final String ROLE_PATHOLOGIST = "Pathologist";
    // roles groups
    public static final String GLOBAL_ROLES_GROUP = "Global Roles";
    public static final String LAB_ROLES_GROUP = "Lab Unit Roles";
}
