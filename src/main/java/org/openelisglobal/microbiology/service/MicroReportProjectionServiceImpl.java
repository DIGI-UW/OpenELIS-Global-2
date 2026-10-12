package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroReportProjectionServiceImpl implements MicroReportProjectionService {
    private final MicroPatientReportService reports;
    private final MicroReportVersionDAO versions;
    private final MicroCaseDAO cases;

    public MicroReportProjectionServiceImpl(MicroPatientReportService reports, MicroReportVersionDAO versions,
            MicroCaseDAO cases) {
        this.reports = reports;
        this.versions = versions;
        this.cases = cases;
    }

    @Override
    public MicroReportProjectionResult preview(String caseId) {
        var c = cases.get(caseId).orElseThrow();
        if ("FINAL_RELEASED".equals(c.getFinalReleaseState())) {
            var version = versions.getLatestByCaseId(caseId);
            if (version != null) {
                var result = new MicroReportProjectionResult(version.getContent(), true, List.of());
                result.structuredContent = version.getStructuredContent();
                result.report = version.getStructuredContent() == null ? null
                        : reports.decode(version.getStructuredContent());
                return result;
            }
        }
        return current(caseId);
    }

    @Override
    @Transactional
    public MicroReportProjectionResult releasePreliminary(String caseId, String actor) {
        var snapshot = reports.preview(caseId);
        if (snapshot.validatedResults == 0)
            throw new MicroAstConflictException("VALIDATED_REPORTABLE_RESULT_REQUIRED");
        return current(caseId);
    }

    @Override
    @Transactional
    public MicroReportProjectionResult releaseFinal(String caseId, String actor) {
        return current(caseId);
    }

    @Override
    @Transactional
    public MicroReportProjectionResult releaseAmended(String caseId, String actor) {
        return current(caseId);
    }

    private MicroReportProjectionResult current(String caseId) {
        var snapshot = reports.preview(caseId);
        String json = reports.encode(snapshot);
        var result = new MicroReportProjectionResult(json, true, List.of());
        result.structuredContent = json;
        result.report = snapshot;
        return result;
    }
}
