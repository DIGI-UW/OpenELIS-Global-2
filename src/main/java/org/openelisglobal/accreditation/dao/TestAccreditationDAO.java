package org.openelisglobal.accreditation.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.dao.BaseDAO;

public interface TestAccreditationDAO extends BaseDAO<TestAccreditation, Long> {

    /**
     * Enrollment rows for any of these tests — one query per rendered patient
     * report, which is why the report resolver never loops per test. Every other
     * lookup this entity needs is a plain property match, so it goes through
     * {@link BaseDAO#getAllMatching}.
     */
    List<TestAccreditation> getByTestIds(Collection<String> testIds);

    /**
     * Enrolled-test count per accrediting body id, for the bodies list column and
     * the summary. One grouped query rather than N per-body queries: that list
     * renders every body's total in a single page load.
     */
    List<Object[]> countEnrolledTestsByBody();
}
