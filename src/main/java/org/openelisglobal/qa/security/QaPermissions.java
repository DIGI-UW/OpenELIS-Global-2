package org.openelisglobal.qa.security;

/**
 * The authority expressions guarding the quality-assurance endpoints.
 *
 * <p>
 * Each constant is a complete Spring Security expression, so an endpoint reads
 * {@code @PreAuthorize(QaPermissions.VIEW_QMS)} rather than repeating the
 * permission key and its administrator fallback. The keys themselves are seeded
 * by the quality-assurance permission changelog and granted to roles there; the
 * Global Administrator role holds every one of them, so the fallback matters
 * only where an installation has edited the grants away in Role Management.
 */
public final class QaPermissions {

    /** Seeded permission keys, shared with the role and menu changelogs. */
    public static final String KEY_VIEW_OVERVIEW = "qa.view.overview";

    public static final String KEY_VIEW_QC = "qa.view.qc";

    public static final String KEY_VIEW_QI = "qa.view.qi";

    public static final String KEY_VIEW_QMS = "qa.view.qms";

    public static final String KEY_VIEW_EQA = "qa.view.eqa";

    public static final String KEY_MANAGE_QI = "qa.manage.qi";

    public static final String KEY_MANAGE_ACCREDITATION = "qa.manage.accreditation";

    private static final String OR_GLOBAL_ADMIN = " or hasRole('GLOBAL_ADMIN')";

    private static final String AUTHORITY = "hasAuthority('";

    public static final String VIEW_OVERVIEW = AUTHORITY + KEY_VIEW_OVERVIEW + "')" + OR_GLOBAL_ADMIN;

    public static final String VIEW_QC = AUTHORITY + KEY_VIEW_QC + "')" + OR_GLOBAL_ADMIN;

    public static final String VIEW_QI = AUTHORITY + KEY_VIEW_QI + "')" + OR_GLOBAL_ADMIN;

    public static final String VIEW_QMS = AUTHORITY + KEY_VIEW_QMS + "')" + OR_GLOBAL_ADMIN;

    public static final String MANAGE_QI = AUTHORITY + KEY_MANAGE_QI + "')" + OR_GLOBAL_ADMIN;

    public static final String MANAGE_ACCREDITATION = AUTHORITY + KEY_MANAGE_ACCREDITATION + "')" + OR_GLOBAL_ADMIN;

    /**
     * The quality-indicator reports additionally admit the bench roles that read
     * them as part of daily work, rather than the administrator fallback alone.
     */
    public static final String VIEW_QI_OR_BENCH = AUTHORITY + KEY_VIEW_QI + "') or hasAnyRole('ADMIN', 'RESULTS',"
            + " 'REPORTS')";

    private QaPermissions() {
    }
}
