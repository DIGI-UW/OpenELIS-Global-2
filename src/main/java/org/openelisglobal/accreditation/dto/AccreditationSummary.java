package org.openelisglobal.accreditation.dto;

import java.util.List;

/**
 * OGC-686 — the accreditation portfolio at a glance.
 *
 * <p>
 * Serves two consumers from one endpoint: the banner on the Accreditation
 * Status page, and the QA Overview's Inspector Readiness answer for
 * "Accreditation status?" ({@code qa.overview.inspector.q5}), which needs a
 * short human summary plus a worst-case tone.
 *
 * <p>
 * {@code activeBodies} / {@code expiringBodies} / {@code expiredBodies} are
 * mutually exclusive and all exclude inactive bodies, so they sum to the number
 * of bodies the lab is actually tracking.
 */
public class AccreditationSummary {

    /** Every body on file, including inactive ones. */
    public int totalBodies;

    /** In date, and not inside the expiring window. */
    public int activeBodies;

    /** In date, but within {@code AccreditationStatus.EXPIRING_WINDOW_DAYS}. */
    public int expiringBodies;

    /** Past their expiry. */
    public int expiredBodies;

    /**
     * Names of the bodies the lab can currently claim (active + expiring), in
     * report-logo order — the "ISO 15189 + CAP current" line.
     */
    public List<String> inForceBodyNames;

    /**
     * Worst status among non-inactive bodies — EXPIRED &gt; EXPIRING &gt; ACTIVE,
     * or null when no bodies are configured (the inert default state).
     */
    public String worstStatus;
}
