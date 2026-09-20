package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionSource;

public interface MicroReportVersionSourceDAO extends BaseDAO<MicroReportVersionSource, String> {

    List<MicroReportVersionSource> getByReportVersionId(String reportVersionId);

    List<MicroReportVersionSource> getByCaseId(String caseId);
}
