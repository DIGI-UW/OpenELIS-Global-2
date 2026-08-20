package org.openelisglobal.eqa.service;

import java.util.Map;

/**
 * Lab EQA Performance (OGC-611, FR-V2.3-07): the accreditation snapshot this
 * lab's quality manager reads — twelve-month KPIs, per-section coverage across
 * each scheme's last four cycles, and the cycles behind them.
 *
 * <p>
 * Derived on read from {@code eqa_participant_result}, which is this lab's own
 * participation by construction: its {@code lab_enrollment_id} references
 * {@code eqa_lab_program_enrollment}, so a remote participant's row can never
 * reach it. Nothing here is stored, so nothing can go stale.
 */
public interface EQALabPerformanceService {

    /**
     * @return {@code kpis}, {@code coverage}, {@code gaps} and
     *         {@code recentCycles}, in one read because the page renders them
     *         together.
     */
    Map<String, Object> getLabPerformance();
}
