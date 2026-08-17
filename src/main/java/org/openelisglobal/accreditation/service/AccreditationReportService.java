package org.openelisglobal.accreditation.service;

import java.time.LocalDate;
import java.util.Collection;
import org.openelisglobal.accreditation.dto.AccreditationReportData;

/**
 * OGC-686 — the single place the logo visibility gate is evaluated.
 *
 * <p>
 * Both patient-report families ({@code PatientReport} and
 * {@code PatientProgramReport}) call this and contribute nothing but a set of
 * test ids and a date, so the rule cannot drift between them.
 */
public interface AccreditationReportService {

    /** Template slots available for logos; see {@code AccreditationReportData}. */
    int MAX_LOGOS = 3;

    /**
     * @param testIds the tests actually printed on the report, already filtered to
     *                the ones that count (validated, not referred out)
     * @param asOf    the report's release date — NOT the render date, so a reprint
     *                reproduces the original PDF even after a body has expired
     */
    AccreditationReportData resolve(Collection<String> testIds, LocalDate asOf);
}
