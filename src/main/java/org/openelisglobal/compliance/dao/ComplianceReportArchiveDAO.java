package org.openelisglobal.compliance.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ComplianceReportArchive;

public interface ComplianceReportArchiveDAO extends BaseDAO<ComplianceReportArchive, Long> {

    Optional<ComplianceReportArchive> findBySampleIdAndAmendmentNumber(Long sampleId, Integer amendmentNumber)
            throws LIMSRuntimeException;

    ComplianceReportArchive save(ComplianceReportArchive entity) throws LIMSRuntimeException;
}
