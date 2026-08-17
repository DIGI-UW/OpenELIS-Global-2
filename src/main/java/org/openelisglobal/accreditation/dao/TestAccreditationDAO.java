package org.openelisglobal.accreditation.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.dao.BaseDAO;

public interface TestAccreditationDAO extends BaseDAO<TestAccreditation, Long> {

    /** Every enrollment row. */
    List<TestAccreditation> getAll();

    /** Enrollment rows for one body. */
    List<TestAccreditation> getByBody(Long accreditingBodyId);

    /** Enrollment rows for one test — backs the {@code ?testId=} deep link. */
    List<TestAccreditation> getByTest(String testId);

    /**
     * Enrollment rows for any of these tests — one query per rendered patient
     * report, which is why the report resolver never loops per test.
     */
    List<TestAccreditation> getByTestIds(Collection<String> testIds);

    /**
     * The (test, body) row if it exists, else null. One enrollment per (test, body)
     * pair.
     */
    TestAccreditation getByTestAndBody(String testId, Long accreditingBodyId);

    /** Whether any test is still enrolled under this body (blocks body delete). */
    long countByBody(Long accreditingBodyId);
}
