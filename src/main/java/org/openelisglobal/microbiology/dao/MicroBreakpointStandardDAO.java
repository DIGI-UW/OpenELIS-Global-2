package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;

public interface MicroBreakpointStandardDAO extends BaseDAO<MicroBreakpointStandard, String> {
    MicroBreakpointStandard getActiveStandard(String authority, String version);

    List<MicroBreakpointStandard> getActiveStandards();

    Optional<MicroBreakpointStandard> findByAuthorityAndVersion(String authority, String version);

    List<MicroBreakpointStandard> getActiveForAuthority(String authority);

    List<MicroBreakpointStandard> search(String q, String status, String authority, String sort, int offset, int limit);

    long countSearch(String q, String status, String authority);
}
