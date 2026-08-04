package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;

public interface MicroReportVersionDAO extends BaseDAO<MicroReportVersion, String> {

    List<MicroReportVersion> getByCaseId(String caseId);

    MicroReportVersion getLatestByCaseId(String caseId);
}
