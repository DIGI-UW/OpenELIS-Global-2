package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroDstResolution;

public interface MicroDstResolutionDAO extends BaseDAO<MicroDstResolution, String> {
    List<MicroDstResolution> getByCaseId(String id);
}
