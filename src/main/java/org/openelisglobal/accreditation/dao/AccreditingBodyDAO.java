package org.openelisglobal.accreditation.dao;

import java.util.List;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.common.dao.BaseDAO;

public interface AccreditingBodyDAO extends BaseDAO<AccreditingBody, Long> {

    /**
     * Every body, in report-logo order (display_order, then code). Named so the
     * report order lives in one place rather than at each call site; codes are
     * stored normalized, so every other lookup is a plain
     * {@link BaseDAO#getAllMatching} match.
     */
    List<AccreditingBody> getAllOrdered();
}
