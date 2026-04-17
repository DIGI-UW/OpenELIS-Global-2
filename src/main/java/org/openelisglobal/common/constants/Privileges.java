package org.openelisglobal.common.constants;

/**
 * Fine-grained privilege name constants.
 *
 * <p>
 * These strings match the {@code name} column values seeded into
 * {@code clinlims.system_privilege} by the Phase 1 Liquibase changeset
 * {@code 012-003-seed-privileges.xml}.
 *
 * <p>
 * The sentinel value {@link #GLOBAL_ADMIN_SENTINEL} is never stored in the
 * database; it is returned at runtime by
 * {@code PrivilegeService.resolveAllPrivilegesForRole} when the role is
 * {@code Global Administrator}, meaning the role implicitly holds every
 * privilege.
 */
public final class Privileges {

    private Privileges() {
        // utility class — no instances
    }

    // -----------------------------------------------------------------------
    // Sentinel
    // -----------------------------------------------------------------------

    /** Returned when a role is Global Administrator — implies every privilege. */
    public static final String GLOBAL_ADMIN_SENTINEL = "*";

    // -----------------------------------------------------------------------
    // Orders
    // -----------------------------------------------------------------------

    public static final String ORDER_CREATE = "order:create";
    public static final String ORDER_VIEW = "order:view";
    public static final String ORDER_EDIT = "order:edit";
    public static final String ORDER_DELETE = "order:delete";

    // -----------------------------------------------------------------------
    // Panel (orderable test group)
    // -----------------------------------------------------------------------

    public static final String PANEL_VIEW = "panel:view";
    public static final String PANEL_MANAGE = "panel:manage";

    // -----------------------------------------------------------------------
    // Analyte (measurable substance)
    // -----------------------------------------------------------------------

    public static final String ANALYTE_VIEW = "analyte:view";
    public static final String ANALYTE_MANAGE = "analyte:manage";

    // -----------------------------------------------------------------------
    // Method (testing method/technique)
    // -----------------------------------------------------------------------

    public static final String METHOD_VIEW = "method:view";
    public static final String METHOD_MANAGE = "method:manage";

    // -----------------------------------------------------------------------
    // Sample Type (specimen type)
    // -----------------------------------------------------------------------

    public static final String SAMPLE_TYPE_VIEW = "sample_type:view";
    public static final String SAMPLE_TYPE_MANAGE = "sample_type:manage";

    // -----------------------------------------------------------------------
    // Sample Status (status codes for samples)
    // -----------------------------------------------------------------------

    public static final String SAMPLE_STATUS_VIEW = "sample_status:view";

    // -----------------------------------------------------------------------
    // Results
    // -----------------------------------------------------------------------

    public static final String RESULT_VIEW = "result:view";
    public static final String RESULT_ENTER = "result:enter";
    public static final String RESULT_MODIFY = "result:modify";
    public static final String RESULT_VALIDATE = "result:validate";
    public static final String RESULT_PATHOLOGY_SIGN_OFF = "result:pathology-sign-off";
    public static final String RESULT_CYTOPATHOLOGY_SIGN_OFF = "result:cytopathology-sign-off";

    // -----------------------------------------------------------------------
    // Patients
    // -----------------------------------------------------------------------

    public static final String PATIENT_VIEW = "patient:view";
    public static final String PATIENT_CREATE = "patient:create";
    public static final String PATIENT_EDIT = "patient:edit";

    // -----------------------------------------------------------------------
    // Reports
    // -----------------------------------------------------------------------

    public static final String REPORT_RUN = "report:run";
    public static final String REPORT_EXPORT = "report:export";

    // -----------------------------------------------------------------------
    // NCE (Non-Conformance Events)
    // -----------------------------------------------------------------------

    public static final String NCE_VIEW = "nce:view";
    public static final String NCE_CREATE = "nce:create";
    public static final String NCE_EDIT = "nce:edit";
    public static final String NCE_ASSIGN = "nce:assign";

    // -----------------------------------------------------------------------
    // Analyzer
    // -----------------------------------------------------------------------

    public static final String ANALYZER_IMPORT = "analyzer:import";
    public static final String ANALYZER_CONFIGURE = "analyzer:configure";

    // -----------------------------------------------------------------------
    // System / Test / Report Configuration (admin-level write actions)
    // -----------------------------------------------------------------------

    public static final String USER_MANAGE = "user:manage";
    public static final String SYSTEM_CONFIGURE = "system:configure";
    public static final String TEST_CONFIGURE = "test:configure";
    public static final String REPORT_CONFIGURE = "report:configure";

    // -----------------------------------------------------------------------
    // Audit
    // -----------------------------------------------------------------------

    public static final String AUDIT_VIEW = "audit:view";

    // -----------------------------------------------------------------------
    // Shipment
    // -----------------------------------------------------------------------

    public static final String SHIPMENT_VIEW = "shipment:view";
    public static final String SHIPMENT_CREATE = "shipment:create";
    public static final String SHIPMENT_EDIT = "shipment:edit";
    public static final String SHIPMENT_DELETE = "shipment:delete";

    // -----------------------------------------------------------------------
    // EQA (External Quality Assurance)
    // -----------------------------------------------------------------------

    public static final String EQA_VIEW = "eqa:view";
    public static final String EQA_MANAGE = "eqa:manage";

    // -----------------------------------------------------------------------
    // Electronic Signature (21 CFR Part 11)
    // -----------------------------------------------------------------------

    public static final String ESIG_USE = "esig:use";

    // -----------------------------------------------------------------------
    // Alert
    // -----------------------------------------------------------------------

    public static final String ALERT_VIEW = "alert:view";
    public static final String ALERT_MANAGE = "alert:manage";

    // -----------------------------------------------------------------------
    // Barcode
    // -----------------------------------------------------------------------

    public static final String BARCODE_VIEW = "barcode:view";
    public static final String BARCODE_MANAGE = "barcode:manage";

    // -----------------------------------------------------------------------
    // Calendar
    // -----------------------------------------------------------------------

    public static final String CALENDAR_VIEW = "calendar:view";
    public static final String CALENDAR_MANAGE = "calendar:manage";

    // -----------------------------------------------------------------------
    // Cold Storage
    // -----------------------------------------------------------------------

    public static final String COLDSTORAGE_VIEW = "coldstorage:view";
    public static final String COLDSTORAGE_MANAGE = "coldstorage:manage";

    // -----------------------------------------------------------------------
    // Dictionary
    // -----------------------------------------------------------------------

    public static final String DICTIONARY_VIEW = "dictionary:view";
    public static final String DICTIONARY_MANAGE = "dictionary:manage";

    // -----------------------------------------------------------------------
    // External Connections
    // -----------------------------------------------------------------------

    public static final String EXTCONNECTION_VIEW = "extconnection:view";
    public static final String EXTCONNECTION_MANAGE = "extconnection:manage";

    // -----------------------------------------------------------------------
    // Inventory
    // -----------------------------------------------------------------------

    public static final String INVENTORY_VIEW = "inventory:view";
    public static final String INVENTORY_MANAGE = "inventory:manage";

    // -----------------------------------------------------------------------
    // Localization
    // -----------------------------------------------------------------------

    public static final String LOCALIZATION_VIEW = "localization:view";
    public static final String LOCALIZATION_MANAGE = "localization:manage";

    // -----------------------------------------------------------------------
    // Notebook
    // -----------------------------------------------------------------------

    public static final String NOTEBOOK_VIEW = "notebook:view";
    public static final String NOTEBOOK_MANAGE = "notebook:manage";

    // -----------------------------------------------------------------------
    // Notification
    // -----------------------------------------------------------------------

    public static final String NOTIFICATION_VIEW = "notification:view";
    public static final String NOTIFICATION_MANAGE = "notification:manage";

    // -----------------------------------------------------------------------
    // Organization
    // -----------------------------------------------------------------------

    public static final String ORGANIZATION_VIEW = "organization:view";
    public static final String ORGANIZATION_MANAGE = "organization:manage";

    // -----------------------------------------------------------------------
    // Program
    // -----------------------------------------------------------------------

    public static final String PROGRAM_VIEW = "program:view";
    public static final String PROGRAM_MANAGE = "program:manage";

    // -----------------------------------------------------------------------
    // Branding
    // -----------------------------------------------------------------------

    public static final String BRANDING_VIEW = "branding:view";
    public static final String BRANDING_MANAGE = "branding:manage";

    // -----------------------------------------------------------------------
    // Provider
    // -----------------------------------------------------------------------

    public static final String PROVIDER_VIEW = "provider:view";
    public static final String PROVIDER_MANAGE = "provider:manage";

    // -----------------------------------------------------------------------
    // Site Information
    // -----------------------------------------------------------------------

    public static final String SITE_INFO_VIEW = "siteinfo:view";

    // -----------------------------------------------------------------------
    // Referral
    // -----------------------------------------------------------------------

    public static final String REFERRAL_VIEW = "referral:view";
    public static final String REFERRAL_MANAGE = "referral:manage";

    // -----------------------------------------------------------------------
    // Storage
    // -----------------------------------------------------------------------

    public static final String STORAGE_VIEW = "storage:view";
    public static final String STORAGE_MANAGE = "storage:manage";

    // -----------------------------------------------------------------------
    // Test Calculation
    // -----------------------------------------------------------------------

    public static final String TESTCALC_VIEW = "testcalc:view";
    public static final String TESTCALC_MANAGE = "testcalc:manage";

    // -----------------------------------------------------------------------
    // Role (role definitions — read during login/auth and admin management)
    // -----------------------------------------------------------------------

    public static final String ROLE_VIEW = "role:view";
    public static final String ROLE_MANAGE = "role:manage";

    // -----------------------------------------------------------------------
    // System User (user accounts — read during login/auth and admin management)
    // -----------------------------------------------------------------------

    public static final String SYSTEM_USER_VIEW = "system_user:view";
    public static final String SYSTEM_USER_MANAGE = "system_user:manage";

    // -----------------------------------------------------------------------
    // User Role (role assignments — read during auth, managed by admin)
    // -----------------------------------------------------------------------

    public static final String USER_ROLE_VIEW = "user_role:view";
    public static final String USER_ROLE_MANAGE = "user_role:manage";

    // -----------------------------------------------------------------------
    // Sample Requester (who requested the sample — clinician/organization)
    // -----------------------------------------------------------------------

    public static final String SAMPLE_REQUESTER_VIEW = "sample_requester:view";
    public static final String SAMPLE_REQUESTER_MANAGE = "sample_requester:manage";
}
