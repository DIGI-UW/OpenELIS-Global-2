package org.openelisglobal.accreditation.dao;

import java.util.List;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.common.dao.BaseDAO;

public interface AccreditingBodyDAO extends BaseDAO<AccreditingBody, Long> {

    /** Every body, in report-logo order (display_order, then code). */
    List<AccreditingBody> getAllOrdered();

    /** Lookup by unique short code, or null. Used to reject duplicate codes. */
    AccreditingBody getByCode(String code);

    /** Enrolled-test count per body id, for the list column and summary. */
    List<Object[]> countEnrolledTestsByBody();
}
