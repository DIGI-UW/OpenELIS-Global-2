package org.openelisglobal.reportqueue.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.reportqueue.valueholder.ReportVersion;

public interface ReportVersionDAO extends BaseDAO<ReportVersion, Long> {

    Optional<ReportVersion> findLatest(Long sampleId, String reportType);

    List<ReportVersion> findAllForSample(Long sampleId, String reportType);

    ReportVersion save(ReportVersion version);
}
