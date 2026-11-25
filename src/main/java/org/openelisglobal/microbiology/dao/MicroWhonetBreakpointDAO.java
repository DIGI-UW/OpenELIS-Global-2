package org.openelisglobal.microbiology.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetBreakpoint;

public interface MicroWhonetBreakpointDAO extends BaseDAO<MicroWhonetBreakpoint, String> {
    void deleteAllRows();

    void insertAll(List<MicroWhonetBreakpoint> breakpoints);

    /** The latest year the table holds for any of these guidelines, or null. */
    String latestYear(Collection<String> guidelines);

    List<MicroWhonetBreakpoint> findHumanFromYear(Collection<String> guidelines, String fromYear);
}
