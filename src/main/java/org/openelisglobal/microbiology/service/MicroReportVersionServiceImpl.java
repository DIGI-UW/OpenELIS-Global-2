package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroReportVersionDAO;
import org.openelisglobal.microbiology.dao.MicroReportVersionSourceDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionSource;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroReportVersionServiceImpl implements MicroReportVersionService {

    private final MicroReportVersionDAO reportVersionDAO;
    private final MicroCaseAnalysisDAO caseAnalysisDAO;
    private final MicroReportProjectionService reportProjectionService;
    private final MicroReportVersionSourceDAO reportVersionSourceDAO;

    public MicroReportVersionServiceImpl(MicroReportVersionDAO reportVersionDAO, MicroCaseAnalysisDAO caseAnalysisDAO,
            MicroReportProjectionService reportProjectionService, MicroReportVersionSourceDAO reportVersionSourceDAO) {
        this.reportVersionDAO = reportVersionDAO;
        this.caseAnalysisDAO = caseAnalysisDAO;
        this.reportProjectionService = reportProjectionService;
        this.reportVersionSourceDAO = reportVersionSourceDAO;
    }

    @Override
    @Transactional
    public MicroReportVersion ensureFinalBaseline(MicroCase microCase) {
        MicroReportVersion existing = reportVersionDAO.getLatestByCaseId(microCase.getId());
        if (existing != null) {
            return existing;
        }
        if (!hasText(microCase.getClosedBy()) || microCase.getClosedAt() == null) {
            throw new IllegalStateException("FINAL_REPORT_BASELINE_UNAVAILABLE");
        }
        MicroReportProjectionResult projection = reportProjectionService.preview(microCase.getId());
        return append(microCase.getId(), null, 1, MicroReportVersionType.FINAL, projection, microCase.getClosedAt(),
                microCase.getClosedBy(), null);
    }

    @Override
    @Transactional
    public MicroReportVersion recordInitialFinal(String caseId, MicroReportProjectionResult projection,
            String performedBy) {
        MicroReportVersion existing = reportVersionDAO.getLatestByCaseId(caseId);
        if (existing != null) {
            return existing;
        }
        return append(caseId, null, 1, MicroReportVersionType.FINAL, projection, MicroCaseServiceImpl.now(),
                performedBy, null);
    }

    @Override
    @Transactional
    public MicroReportVersion recordAmendedFinal(MicroCaseAmendment amendment, MicroReportProjectionResult projection,
            String performedBy) {
        MicroReportVersion previous = reportVersionDAO.getLatestByCaseId(amendment.getCaseId());
        if (previous == null) {
            throw new IllegalStateException("FINAL_REPORT_BASELINE_UNAVAILABLE");
        }
        return append(amendment.getCaseId(), amendment.getId(), previous.getVersionNumber() + 1,
                MicroReportVersionType.AMENDED, projection, MicroCaseServiceImpl.now(), performedBy, previous.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReportVersion> getVersions(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return reportVersionDAO.getByCaseId(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReportVersionSource> getSourcesForCase(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return reportVersionSourceDAO.getByCaseId(caseId);
    }

    private MicroReportVersion append(String caseId, String amendmentId, int versionNumber,
            MicroReportVersionType releaseType, MicroReportProjectionResult projection, Timestamp releasedAt,
            String releasedBy, String correctsVersionId) {
        if (projection == null || !hasText(projection.getContent())) {
            throw new IllegalStateException("REPORTABLE_CONTENT_REQUIRED");
        }
        MicroReportVersion version = new MicroReportVersion();
        version.setCaseId(caseId);
        version.setAmendmentId(amendmentId);
        version.setVersionNumber(versionNumber);
        version.setReleaseType(releaseType.name());
        version.setContent(projection.getContent());
        version.setReleasedAt(releasedAt);
        version.setReleasedBy(releasedBy);
        version.setCorrectsVersionId(correctsVersionId);
        List<MicroCaseAnalysis> links = caseAnalysisDAO.getByCaseId(caseId);
        reportVersionDAO.insert(version);
        for (MicroCaseAnalysis link : links) {
            MicroReportVersionSource source = new MicroReportVersionSource();
            source.setReportVersionId(version.getId());
            source.setAnalysisId(link.getAnalysisId());
            source.setResultId(link.getProjectedResultId());
            reportVersionSourceDAO.insert(source);
        }
        return version;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
