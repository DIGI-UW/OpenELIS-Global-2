package org.openelisglobal.accreditation.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.accreditation.dto.EqaCoverageView;
import org.openelisglobal.accreditation.dto.TestAccreditationView;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.service.BaseObjectService;

public interface TestAccreditationService extends BaseObjectService<TestAccreditation, Long> {

    /**
     * Enrollment rows, optionally narrowed by body and/or test. Both filters null
     * returns everything. The {@code testId} filter backs the {@code ?testId=} deep
     * link (FR-23).
     */
    List<TestAccreditationView> getEnrollmentViews(Long accreditingBodyId, String testId);

    /**
     * Enroll a test under a body. Rejects an unknown test or body, and a duplicate
     * (test, body) pair (FR-19).
     */
    TestAccreditation enroll(String testId, Long accreditingBodyId, LocalDate effectiveFrom, String sysUserId);

    /** Remove one enrollment row. */
    void unenroll(Long id, String sysUserId);

    /**
     * OGC-686 (QA-D.5) — per body: accredited scope vs. live EQA cover, plus the
     * tests that fall in the gap. One row per body that has at least one accredited
     * test; bodies with an empty scope have nothing to answer for.
     */
    List<EqaCoverageView> getEqaCoverage();
}
