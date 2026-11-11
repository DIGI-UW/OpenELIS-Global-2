package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroReportVersionDAO;
import org.openelisglobal.microbiology.dao.MicroReportVersionSourceDAO;
import org.openelisglobal.microbiology.form.MicroReportVersionForm;
import org.openelisglobal.microbiology.form.MicroReportVersionSourceForm;
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

    private final MicroPatientReportService patientReports;
    private final MicroReportVersionDAO reportVersionDAO;
    private final MicroCaseAnalysisDAO caseAnalysisDAO;
    private final MicroReportProjectionService reportProjectionService;
    private final MicroReportVersionSourceDAO reportVersionSourceDAO;

    public MicroReportVersionServiceImpl(MicroReportVersionDAO reportVersionDAO, MicroCaseAnalysisDAO caseAnalysisDAO,
            MicroReportProjectionService reportProjectionService, MicroReportVersionSourceDAO reportVersionSourceDAO,
            MicroPatientReportService patientReports) {
        this.patientReports = patientReports;
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
        if (existing != null && !MicroReportVersionType.PRELIMINARY.name().equals(existing.getReleaseType()))
            throw new MicroCaseLockedException("FINAL_CASE_LOCKED");
        return append(caseId, null, existing == null ? 1 : existing.getVersionNumber() + 1,
                MicroReportVersionType.FINAL, projection, MicroCaseServiceImpl.now(), performedBy, null);
    }

    @Override
    @Transactional
    public MicroReportVersion recordPreliminary(String caseId, MicroReportProjectionResult projection,
            String performedBy) {
        var existing = reportVersionDAO.getLatestByCaseId(caseId);
        if (existing != null && !MicroReportVersionType.PRELIMINARY.name().equals(existing.getReleaseType()))
            throw new MicroCaseLockedException("FINAL_CASE_LOCKED");
        return append(caseId, null, existing == null ? 1 : existing.getVersionNumber() + 1,
                MicroReportVersionType.PRELIMINARY, projection, MicroCaseServiceImpl.now(), performedBy, null);
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
        version.setStructuredContent(projection.structuredContent);
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

    @Override
    @Transactional(readOnly = true)
    public List<MicroReportVersionForm> getVersionForms(String caseId) {
        var sources = getSourcesForCase(caseId).stream()
                .collect(java.util.stream.Collectors.groupingBy(MicroReportVersionSource::getReportVersionId));
        return getVersions(caseId).stream()
                .map(version -> toForm(version, sources.getOrDefault(version.getId(), List.of()))).toList();
    }

    private MicroReportVersionForm toForm(MicroReportVersion version, List<MicroReportVersionSource> sources) {
        MicroReportVersionForm form = new MicroReportVersionForm();
        form.id = version.getId();
        form.caseId = version.getCaseId();
        form.amendmentId = version.getAmendmentId();
        form.versionNumber = version.getVersionNumber();
        form.releaseType = version.getReleaseType();
        form.content = version.getStructuredContent() == null ? version.getContent() : null;
        form.report = version.getStructuredContent() == null ? null
                : patientReports.decode(version.getStructuredContent());
        form.releasedAt = version.getReleasedAt();
        form.releasedBy = version.getReleasedBy();
        form.correctsVersionId = version.getCorrectsVersionId();
        form.sources.addAll(sources.stream().map(source -> {
            MicroReportVersionSourceForm sourceForm = new MicroReportVersionSourceForm();
            sourceForm.analysisId = source.getAnalysisId();
            sourceForm.resultId = source.getResultId();
            return sourceForm;
        }).toList());
        return form;
    }
}
