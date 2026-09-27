package org.openelisglobal.compliance.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ComplianceReportGeneration;

public interface ComplianceReportGenerationDAO extends BaseDAO<ComplianceReportGeneration, Long> {

    Optional<ComplianceReportGeneration> findLatestBySampleId(Long sampleId) throws LIMSRuntimeException;

    ComplianceReportGeneration save(ComplianceReportGeneration entity) throws LIMSRuntimeException;
}
