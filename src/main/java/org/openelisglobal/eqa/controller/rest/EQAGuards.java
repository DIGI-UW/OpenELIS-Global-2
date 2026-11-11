package org.openelisglobal.eqa.controller.rest;

/**
 * The EQA authorization tiers, in one place (OGC-609). Twelve controllers carry
 * these guards and {@code EQARestGuardMatrixTest} asserts them as text, so a
 * literal per annotation is a literal that drifts.
 *
 * <p>
 * Reads sit under one umbrella; every state-mutating handler declares its lane,
 * because Spring replaces the class annotation rather than ANDing it.
 */
public final class EQAGuards {

    public static final String READ = "hasAuthority('qa.view.eqa') or hasRole('GLOBAL_ADMIN')";

    /**
     * Self-enrollment, panel receipt, result entry and import, FHIR submission.
     * Bench roles reach it through the qa/026 grant, not a role clause here.
     */
    public static final String PARTICIPANT = "hasAuthority('qa.eqa.participant') or hasRole('GLOBAL_ADMIN')";

    /**
     * Scheme CRUD, distributions, participant enrollment, late-submission approval.
     */
    public static final String PROVIDER = "hasAuthority('qa.eqa.provider') or hasRole('GLOBAL_ADMIN')";

    /** Panel seal and distribute, cycle transition, scoring. */
    public static final String MANAGE = "hasAuthority('qa.manage.eqa') or hasRole('GLOBAL_ADMIN')";

    /**
     * Also read imperatively by {@code EQAPanelRestController#callerCanUnblind()}.
     */
    public static final String UNBLIND_AUTHORITY = "qa.eqa.inhouse.unblind";

    /** Revealing sealed targets — a different privilege from {@link #MANAGE}. */
    public static final String UNBLIND = "hasAuthority('" + UNBLIND_AUTHORITY + "') or hasRole('GLOBAL_ADMIN')";

    /**
     * Not an EQA tier: the lab-wide alerts dashboard. See EQAAlertRestController.
     */
    public static final String LAB_WIDE_ALERTS = "hasAnyRole('RECEPTION', 'RESULTS')";

    private EQAGuards() {
    }
}
