package org.openelisglobal.accreditation.dao;

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

    /** The (test, body) row if it exists, else null. Enforces FR-19 uniqueness. */
    TestAccreditation getByTestAndBody(String testId, Long accreditingBodyId);

    /** Whether any test is still enrolled under this body (blocks body delete). */
    long countByBody(Long accreditingBodyId);
}
