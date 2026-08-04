package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetExportRun;

public interface MicroWhonetExportRunDAO extends BaseDAO<MicroWhonetExportRun, String> {

    List<MicroWhonetExportRun> getRecent(int limit);
}
