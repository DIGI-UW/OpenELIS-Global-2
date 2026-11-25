package org.openelisglobal.test.service;

import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;

/**
 * OGC-189 (M4): the single definition of whether a test may be ordered, taking
 * its lab unit's status into account.
 *
 * <p>
 * Before this existed, a lab unit's active flag did not participate in
 * orderability at all: the ordering path filtered on the <em>test's</em> own
 * flag, so deactivating a unit was purely presentational — tests still routed
 * to it and orders kept landing in it (measured on testing 2026-09-02, QA
 * LU-W-11/LU-W-12).
 *
 * <p>
 * <b>Derived, never written.</b> These are read-only predicates: the cascade
 * must never mutate {@code test.active} or {@code test.orderable}. That is what
 * makes reactivation free and lossless — switch the unit back on and every test
 * returns to exactly what its own configuration says, with no restore step and
 * no shadow column remembering why something was off.
 *
 * <p>
 * The two flags mean different things and are deliberately kept apart:
 * <ul>
 * <li>{@code active} — the test can be ordered <em>at all</em>, including as a
 * reflex, from an analyzer, or over FHIR/HL7.</li>
 * <li>{@code orderable} — the test appears in the <em>manual</em> order-entry
 * list.</li>
 * </ul>
 * {@code active: true, orderable: false} is an intentional category, not a
 * misconfiguration: reflex-ordered antibiotic susceptibility, confirmation
 * tests, analyzer-driven CT values. The cascade therefore acts on
 * <em>active</em>; suppressing {@code orderable} would block the one route that
 * does not apply to those tests and leave every route that does.
 */
public interface EffectiveTestStatusService {

    /**
     * Whether a NEW analysis may be created for this test by any route — manual
     * entry, reflex, analyzer, FHIR, incoming electronic orders.
     *
     * <p>
     * {@code effectiveActive = test.active && labUnit.isActive}. A test with no lab
     * unit is governed by its own flag alone.
     *
     * <p>
     * This gates <em>creation</em> only. Completing work that already exists is
     * never blocked: results entry, validation, workplan, by-unit reports, patient
     * history and the unified worklist must all keep working for an in-flight
     * analysis whose lab unit has since been switched off (comment 37313 §2).
     */
    boolean isEffectivelyActive(Test test);

    /** As {@link #isEffectivelyActive(Test)}, resolving the test by id. */
    boolean isEffectivelyActiveById(String testId);

    /**
     * Whether this test should appear in the manual order-entry list:
     * {@code test.orderable && isEffectivelyActive(test)}.
     */
    boolean isEffectivelyOrderable(Test test);

    /**
     * Whether this lab unit is switched off. Null-safe: a null section is not
     * treated as inactive, so a test with no lab unit is never blocked by this
     * rule.
     */
    boolean isLabUnitInactive(TestSection testSection);
}
