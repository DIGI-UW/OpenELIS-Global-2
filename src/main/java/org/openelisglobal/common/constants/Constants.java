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
    public static final String ROLE_SYSTEM_ADMIN = "System Admin";
    public static final String ROLE_GLOBAL_ADMIN = "Global Administrator";
    public static final String ROLE_USER_ACCOUNT_ADMIN = "User Account Administrator";
    public static final String ROLE_AUDIT_TRAIL = "Audit Trail";
    public static final String ROLE_RECEPTION = "Reception";
    public static final String ROLE_RESULTS = "Results";
    public static final String ROLE_VALIDATION = "Validation";
    public static final String ROLE_REPORTS = "Reports";
    public static final String ROLE_PATHOLOGIST = "Pathologist";
    public static final String ROLE_NOTEBOOK_ADMIN = "Notebook Administrator";

    // AHRI SRS global roles
    public static final String ROLE_ADMINISTRATIVE_STAFF = "Administrative Staff";
    public static final String ROLE_IT_SUPPORT_STAFF = "IT Support Staff";
    public static final String ROLE_EQA_PERSONNEL = "EQA Personnel";
    public static final String ROLE_EXTERNAL_STAKEHOLDERS = "External Stakeholders";

    // AHRI SRS department/lab unit roles
    public static final String ROLE_SAMPLE_COLLECTOR = "Sample Collector";
    public static final String ROLE_LABORATORY_TECHNICIAN = "Laboratory Technician";
    public static final String ROLE_JUNIOR_RESEARCHER = "Junior Researcher";
    public static final String ROLE_SENIOR_RESEARCHER = "Senior Researcher";
    public static final String ROLE_LAB_MANAGER = "Lab Manager";
    public static final String ROLE_BIOMEDICAL_STAFF = "Biomedical Staff";

    // AHRI SRS project roles
    public static final String ROLE_PRINCIPAL_INVESTIGATOR = "Principal Investigator";
    public static final String ROLE_PROJECT_COORDINATOR = "Project Coordinator";
    public static final String ROLE_DATA_MANAGER = "Data Manager";

    // roles groups
    public static final String GLOBAL_ROLES_GROUP = "Global Roles";
    public static final String LAB_ROLES_GROUP = "Lab Unit Roles";
    public static final String PROJECT_ROLES_GROUP = "Project Roles";
}
